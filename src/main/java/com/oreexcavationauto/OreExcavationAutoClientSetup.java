package com.oreexcavationauto;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

public class OreExcavationAutoClientSetup {
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(OreExcavationAutoClientSetup::onClientSetup);
        modEventBus.addListener(OreExcavationAutoClientSetup::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.register(OreExcavationAutoClientEvents.class);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        OreExcavationAuto.LOGGER.info("Ore Excavation Auto client setup complete.");
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OreExcavationAutoKeys.TOGGLE_KEY);
        event.register(OreExcavationAutoKeys.OPEN_GUI_KEY);
    }
}
