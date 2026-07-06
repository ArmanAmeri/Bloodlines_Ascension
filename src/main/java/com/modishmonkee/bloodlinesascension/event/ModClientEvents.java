package com.modishmonkee.bloodlinesascension.event;

import com.modishmonkee.bloodlinesascension.BloodlinesAscension;
import com.modishmonkee.bloodlinesascension.client.ClientBloodState;
import com.modishmonkee.bloodlinesascension.client.ModKeyBindings;
import com.modishmonkee.bloodlinesascension.client.hud.BloodOrbHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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

        ClientBloodState.WAVES.tick();
    }

    /** Don't let blood/wave state bleed into the next world (same lesson as Arsenal's trackers). */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBloodState.reset();
    }
}
