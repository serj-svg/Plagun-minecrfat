package com.plagun.mod;

import com.plagun.mod.commands.ModCommands;
import com.plagun.mod.effects.SparkleManager;
import com.plagun.mod.events.ModEvents;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlagunMod implements ModInitializer {

    public static final String MOD_ID = "plagun";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Plagun mod (v1.0.0) for Minecraft 1.21.3");
        PlagunState.init();
        ModCommands.register();   // /plagun + /lives + /pteam + /hg + /pfreeze...
        ModEvents.register();      // lifecycle, join, death, respawn, tick, pvp guard
        SparkleManager.register(); // sparkle particle trail
        LOGGER.info("Plagun mod initialized.");
    }
}
