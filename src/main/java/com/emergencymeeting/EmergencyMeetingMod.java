package com.emergencymeeting;

import com.emergencymeeting.client.ClientConfig;
import com.emergencymeeting.network.Packets;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EmergencyMeetingMod.MODID)
public class EmergencyMeetingMod {
    public static final String MODID = "emergencymeeting";

    public EmergencyMeetingMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        Packets.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
