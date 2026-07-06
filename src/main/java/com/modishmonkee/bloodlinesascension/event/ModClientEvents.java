package com.modishmonkee.bloodlinesascension.event;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.ClientBloodState;
import com.modishmonkee.bloodlinesascension.client.ModKeyBindings;
import com.modishmonkee.bloodlinesascension.client.hud.BloodOrbHudLayer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.minecraft.client.Minecraft;
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

    // ── Liquid inertia: previous-tick movement state ─────────────────────────
    private static float lastYaw = Float.NaN;
    private static float lastPitch;
    private static double lastVelocityY;
    private static boolean wasOnGround = true;

    // Feel tuning for how hard the orb reacts to player motion
    private static final float TILT_PER_YAW_DEGREE = 0.022f;
    private static final float HEAVE_PER_PITCH_DEGREE = 0.012f;
    private static final float HEAVE_PER_VERTICAL_ACCEL = 3.2f;
    private static final float LANDING_SPLASH_SCALE = 4.5f;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.DEV_BLOOD_FILL);
        event.register(ModKeyBindings.DEV_BLOOD_DRAIN);
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

        applyMotionInertia(mc);
        ClientBloodState.WAVES.tick();
    }

    /**
     * Feed player motion into the liquid: turning the camera tilts the blood
     * against the turn, jumping presses it down, landing slaps it hard.
     */
    private static void applyMotionInertia(Minecraft mc) {
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        double velocityY = mc.player.getDeltaMovement().y;
        boolean onGround = mc.player.onGround();

        if (!Float.isNaN(lastYaw)) {
            float yawDelta = Mth.degreesDifference(lastYaw, yaw);
            if (Math.abs(yawDelta) > 0.4f) {
                ClientBloodState.WAVES.tilt(Mth.clamp(yawDelta * TILT_PER_YAW_DEGREE, -1.4f, 1.4f));
            }
            float pitchDelta = pitch - lastPitch;
            if (Math.abs(pitchDelta) > 0.4f) {
                ClientBloodState.WAVES.heave(Mth.clamp(pitchDelta * HEAVE_PER_PITCH_DEGREE, -0.7f, 0.7f));
            }

            // Vertical acceleration (jump start, gravity flip, elytra, etc.)
            float accelY = (float) (velocityY - lastVelocityY);
            if (Math.abs(accelY) > 0.08f) {
                ClientBloodState.WAVES.heave(Mth.clamp(accelY * HEAVE_PER_VERTICAL_ACCEL, -2.2f, 2.2f));
            }
            // Landing after a real fall: extra chaotic splash on top of the heave
            if (onGround && !wasOnGround && lastVelocityY < -0.35) {
                ClientBloodState.WAVES.splash(Math.min((float) -lastVelocityY * LANDING_SPLASH_SCALE, 5.0f));
            }
        }

        lastYaw = yaw;
        lastPitch = pitch;
        lastVelocityY = velocityY;
        wasOnGround = onGround;
    }

    // ── TEST: Veil learning spike — blood sky post pipeline ──────────────────
    private static final ResourceLocation BLOOD_SKY_PIPELINE =
            ResourceLocation.fromNamespaceAndPath(BloodlinesAscension.MOD_ID, "blood_sky");

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        PostProcessingManager post = VeilRenderSystem.renderer().getPostProcessingManager();
        if (!post.isActive(BLOOD_SKY_PIPELINE)) {
            post.add(BLOOD_SKY_PIPELINE);
        }
    }

    /** Don't let blood/wave state bleed into the next world (same lesson as Arsenal's trackers). */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBloodState.reset();
        lastYaw = Float.NaN;
        lastVelocityY = 0;
        wasOnGround = true;
    }
}
