package com.modishmonkee.bloodlinesascension.event;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.ClientBloodState;
import com.modishmonkee.bloodlinesascension.client.ModKeyBindings;
import com.modishmonkee.bloodlinesascension.client.hud.BloodOrbHudLayer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
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

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.DEV_BLOOD_FILL);
        event.register(ModKeyBindings.DEV_BLOOD_DRAIN);
        event.register(ModKeyBindings.DEV_SPAWN_LIGHT);
        event.register(ModKeyBindings.DEV_CLEAR_LIGHTS);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "blood_orb"),
                new BloodOrbHudLayer());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;

        // DEV placeholder input until real blood mechanics exist (M1)
        if (ModKeyBindings.DEV_BLOOD_FILL.isDown()) ClientBloodState.addBlood(0.8f);
        if (ModKeyBindings.DEV_BLOOD_DRAIN.isDown()) ClientBloodState.addBlood(-0.8f);

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

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
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
        lastYaw = Float.NaN;
        DEV_LIGHTS.forEach(LightRenderHandle::free);
        DEV_LIGHTS.clear();
    }
}
