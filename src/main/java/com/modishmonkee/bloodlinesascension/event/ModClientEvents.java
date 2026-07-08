package com.modishmonkee.bloodlinesascension.event;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.ClientBloodState;
import com.modishmonkee.bloodlinesascension.client.ClientSkillBarState;
import com.modishmonkee.bloodlinesascension.client.ModKeyBindings;
import com.modishmonkee.bloodlinesascension.client.hud.BloodOrbHudLayer;
import com.modishmonkee.bloodlinesascension.client.hud.SkillBarHudLayer;
import com.modishmonkee.bloodlinesascension.client.screen.CharacterScreen;
import com.mojang.brigadier.arguments.FloatArgumentType;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = BloodlinesAscension.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    // ── Liquid inertia: previous-tick movement state ─────────────────────────
    private static float lastYaw = Float.NaN;
    private static float lastPitch;

    /** DEV: handles for test lights so they can be removed/cleaned up. */
    private static final java.util.List<LightRenderHandle<PointLightData>> DEV_LIGHTS = new java.util.ArrayList<>();

    // Feel tuning — deliberately subtle so the fill level stays readable
    private static final float TILT_PER_YAW_DEGREE = 0.008f;
    private static final float HEAVE_PER_PITCH_DEGREE = 0.004f;

    // Skill-bar toggle key edge-tracking: a plain tap toggles the bar, but if the player
    // scrolled while holding it (to change sets) we suppress the toggle on release.
    private static boolean skillKeyWasDown = false;
    private static boolean skillKeyScrolled = false;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.OPEN_CHARACTER_SCREEN);
        event.register(ModKeyBindings.TOGGLE_SKILL_BAR);
        event.register(ModKeyBindings.DEV_BLOOD_FILL);
        event.register(ModKeyBindings.DEV_BLOOD_DRAIN);
        event.register(ModKeyBindings.DEV_SPAWN_LIGHT);
        event.register(ModKeyBindings.DEV_CLEAR_LIGHTS);
    }

    /** DEV/testing: {@code /blood <amount>} sets current blood; {@code /blood max <amount>} sets max blood. */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("blood")
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                        .executes(ctx -> {
                            float amount = FloatArgumentType.getFloat(ctx, "amount");
                            ClientBloodState.setBlood(amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Blood set to " + amount), false);
                            return 1;
                        }))
                .then(Commands.literal("max")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(1f))
                                .executes(ctx -> {
                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                    ClientBloodState.setMax(amount);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Max blood set to " + amount), false);
                                    return 1;
                                }))));
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "blood_orb"),
                new BloodOrbHudLayer());
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "skill_bar"),
                new SkillBarHudLayer());
    }

    /** Every vanilla hotbar-anchored bar/meter, hidden while the skill bar is on. */
    private static final java.util.Set<ResourceLocation> HIDDEN_IN_SKILL_MODE = java.util.Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.JUMP_METER,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH);

    /**
     * While the skill bar is toggled on, hide the vanilla hotbar and every status bar anchored to
     * it — the skill bar renders its own slots and side hearts/food instead. Also cancels AppleSkin's
     * overlays (saturation/exhaustion/etc.), which live under its own namespace.
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!ClientSkillBarState.isActive()) return;
        ResourceLocation layer = event.getName();
        if (HIDDEN_IN_SKILL_MODE.contains(layer) || layer.getNamespace().equals("appleskin")) {
            event.setCanceled(true);
        }
    }

    /** While the skill key is held, scrolling cycles skill sets (and shows the current one). */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!ModKeyBindings.TOGGLE_SKILL_BAR.isDown()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double dy = event.getScrollDeltaY();
        if (dy == 0) return;
        ClientSkillBarState.cycleSet(dy > 0 ? 1 : -1);
        skillKeyScrolled = true; // suppress the tap-toggle when the key is released
        mc.player.displayClientMessage(
                Component.translatable("message.bloodlinesascension.skill_set", ClientSkillBarState.getSetNumber()), true);
        event.setCanceled(true);
    }

    /**
     * In skill mode, swallow the number-key hotbar-slot clicks BEFORE vanilla's keybind handling
     * runs (this fires at the start of the client tick), so 1-6 don't switch the held item. The
     * scroll wheel uses a separate mouse path and still switches items; the gold slot highlight
     * reads {@code isDown()} (not the click queue), so it's unaffected.
     */
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        if (!ClientSkillBarState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (KeyMapping slot : mc.options.keyHotbarSlots) {
            while (slot.consumeClick()) { /* drain so vanilla doesn't switch slots */ }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;

        // Closing is handled by CharacterScreen#keyPressed itself (same key re-closes it,
        // like vanilla's inventory key) — this only needs to handle opening.
        while (ModKeyBindings.OPEN_CHARACTER_SCREEN.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new CharacterScreen());
            }
        }

        // Skill bar: tap < to toggle it; hold < + scroll cycles sets (onMouseScroll). A tap is
        // "released without having scrolled", so it doesn't fire when the hold was used to scroll.
        boolean skillKeyDown = ModKeyBindings.TOGGLE_SKILL_BAR.isDown();
        if (skillKeyDown && !skillKeyWasDown) {
            skillKeyScrolled = false;
        } else if (!skillKeyDown && skillKeyWasDown && !skillKeyScrolled) {
            ClientSkillBarState.toggle();
        }
        skillKeyWasDown = skillKeyDown;

        // DEV placeholder input until real blood mechanics exist (M1). Rate is a % of max blood
        // so it fills/drains in the same time regardless of max (0.8% per tick).
        float bloodStep = ClientBloodState.getMax() * 0.008f;
        if (ModKeyBindings.DEV_BLOOD_FILL.isDown()) ClientBloodState.addBlood(bloodStep);
        if (ModKeyBindings.DEV_BLOOD_DRAIN.isDown()) ClientBloodState.addBlood(-bloodStep);

        // DEV: Veil dynamic light test — spawn a blood-red light at the player.
        // Gotcha this exists to demonstrate: default radius is 1.0 (invisible);
        // a visible light needs a real radius + brightness.
        while (ModKeyBindings.DEV_SPAWN_LIGHT.consumeClick()) {
            Vec3 eye = mc.player.getEyePosition();
            PointLightData light = new PointLightData();
            light.setPosition(eye.x, eye.y, eye.z)
                    .setColor(0.784f, 0.125f, 0.173f) // blood_bright
                    .setBrightness(2.0f)
                    .setRadius(15.0f);
            DEV_LIGHTS.add(VeilRenderSystem.renderer().getLightRenderer().addLight(light));
            LOGGER.info("Dev light spawned at {} ({} active)", eye, DEV_LIGHTS.size());
        }
        while (ModKeyBindings.DEV_CLEAR_LIGHTS.consumeClick()) {
            DEV_LIGHTS.forEach(LightRenderHandle::free);
            LOGGER.info("Cleared {} dev lights", DEV_LIGHTS.size());
            DEV_LIGHTS.clear();
        }

        applyMotionInertia(mc);
        ClientBloodState.WAVES.tick();
    }

    /**
     * Feed camera motion into the liquid, gently: turning tilts the blood a
     * touch against the turn, pitching bobs it slightly. No jump/fall reaction.
     */
    private static void applyMotionInertia(Minecraft mc) {
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        if (!Float.isNaN(lastYaw)) {
            float yawDelta = Mth.degreesDifference(lastYaw, yaw);
            if (Math.abs(yawDelta) > 0.4f) {
                ClientBloodState.WAVES.tilt(Mth.clamp(yawDelta * TILT_PER_YAW_DEGREE, -0.5f, 0.5f));
            }
            float pitchDelta = pitch - lastPitch;
            if (Math.abs(pitchDelta) > 0.4f) {
                ClientBloodState.WAVES.heave(Mth.clamp(pitchDelta * HEAVE_PER_PITCH_DEGREE, -0.25f, 0.25f));
            }
        }

        lastYaw = yaw;
        lastPitch = pitch;
    }

    // ── TEST: Veil learning spike — blood sky post pipeline ──────────────────
    private static final ResourceLocation BLOOD_SKY_PIPELINE =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "blood_sky");
    /** Off by default; flip to true to re-enable the blood sky spike on login. */
    private static final boolean ENABLE_BLOOD_SKY = false;

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLE_BLOOD_SKY) return;
        PostProcessingManager post = VeilRenderSystem.renderer().getPostProcessingManager();
        if (!post.isActive(BLOOD_SKY_PIPELINE)) {
            if (post.add(BLOOD_SKY_PIPELINE)) {
                LOGGER.info("Blood sky post pipeline enabled");
            } else {
                LOGGER.warn("Blood sky post pipeline not found — check pinwheel/post/blood_sky.json");
            }
        }
    }

    /** Don't let blood/wave state bleed into the next world (same lesson as Arsenal's trackers). */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBloodState.reset();
        ClientSkillBarState.reset();
        lastYaw = Float.NaN;
        skillKeyWasDown = false;
        skillKeyScrolled = false;
        DEV_LIGHTS.forEach(LightRenderHandle::free);
        DEV_LIGHTS.clear();
    }
}
