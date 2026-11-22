package com.oreexcavationauto;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(OreExcavationAuto.MOD_ID)
public class OreExcavationAuto {
    public static final String MOD_ID = "oreexcavationauto";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OreExcavationAuto(IEventBus modEventBus, ModContainer modContainer) {
        OreExcavationAutoConfig.register(modContainer);
        OreExcavationAutoClientSetup.init(modEventBus);
        LOGGER.info("Ore Excavation Auto initialized!");
    }
}
