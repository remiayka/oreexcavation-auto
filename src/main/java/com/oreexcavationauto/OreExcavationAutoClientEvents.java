package com.oreexcavationauto;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public class OreExcavationAutoClientEvents {

    // Cached reference to the OreExcavation key binding
    private static KeyMapping oreExcavationKey = null;
    private static boolean keySearchDone = false;
    private static boolean wasActivating = false;
    private static boolean shouldActivateForCurrent = false;
    private static int activationTicksRemaining = 0;
    private static final int ACTIVATION_PULSE_TICKS = 100;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()
            || !OreExcavationAutoConfig.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        ResourceLocation blockId = mc.level.registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
            .getKey(event.getState().getBlock());
        if (blockId != null
            && OreExcavationAutoConfig.shouldActivate(blockId.toString())) {
            sendOreExcavationPacket(
                event.getPos(),
                event.getState(),
                blockId);
        }
    }

    private static void sendOreExcavationPacket(
        BlockPos pos,
        BlockState state,
        ResourceLocation blockId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.core.Direction facing = mc.player.getDirection();
            net.minecraft.core.Direction side = facing.getOpposite();

            net.minecraft.nbt.CompoundTag tags = new net.minecraft.nbt.CompoundTag();
            tags.putLong("pos", pos.asLong());
            tags.putInt("stateId", net.minecraft.world.level.block.Block.getId(state));
            tags.putInt("side", side.get3DDataValue());
            tags.putInt("facing", facing.get3DDataValue());

            Class<?> packetClass =
                Class.forName("oreexcavation.network.PacketExcavation");
            Object packet = packetClass
                .getConstructor(net.minecraft.nbt.CompoundTag.class)
                .newInstance(tags);

            Class<?> distributorClass =
                Class.forName("net.neoforged.neoforge.network.PacketDistributor");
            Method sendToServer = distributorClass.getMethod(
                "sendToServer",
                net.minecraft.network.protocol.common.custom.CustomPacketPayload.class,
                net.minecraft.network.protocol.common.custom.CustomPacketPayload[].class);
            sendToServer.invoke(
                null,
                packet,
                new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);

            OreExcavationAuto.LOGGER.info(
                "Sent Ore Excavation packet for {} at {}", blockId, pos);
        } catch (ClassNotFoundException e) {
            OreExcavationAuto.LOGGER.warn(
                "Ore Excavation is not loaded; client packet activation is unavailable");
        } catch (ReflectiveOperationException e) {
            OreExcavationAuto.LOGGER.error(
                "Failed to send Ore Excavation packet", e);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !OreExcavationAutoConfig.isEnabled()) return;

        BlockPos pos = event.getPos();
        BlockState state = mc.level.getBlockState(pos);
        ResourceLocation blockId = mc.level.registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
            .getKey(state.getBlock());

        if (blockId != null
            && OreExcavationAutoConfig.shouldActivate(blockId.toString())) {
            activationTicksRemaining = ACTIVATION_PULSE_TICKS;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Handle toggle key
        while (OreExcavationAutoKeys.TOGGLE_KEY.consumeClick()) {
            boolean newState = !OreExcavationAutoConfig.isEnabled();
            OreExcavationAutoConfig.setEnabled(newState);
            mc.player.displayClientMessage(
                Component.translatable(newState ?
                    "message.oreexcavationauto.enabled" :
                    "message.oreexcavationauto.disabled"),
                true
            );
        }

        // Handle open GUI key
        while (OreExcavationAutoKeys.OPEN_GUI_KEY.consumeClick()) {
            mc.setScreen(new OreExcavationAutoScreen());
        }

        // Find ore excavation key if not done yet
        if (!keySearchDone) {
            findOreExcavationKey();
        }

        // Keep excavation active while mining, including breaks initiated by Baritone.
        if (oreExcavationKey != null && mc.gameMode != null) {
            // Check what block is being looked at
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                ResourceLocation blockId = mc.level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                    .getKey(state.getBlock());

                if (blockId != null) {
                    String idString = blockId.toString();
                    shouldActivateForCurrent = OreExcavationAutoConfig.shouldActivate(idString);
                } else {
                    shouldActivateForCurrent = false;
                }
            } else {
                shouldActivateForCurrent = false;
            }

            if (shouldActivateForCurrent) {
                activationTicksRemaining = ACTIVATION_PULSE_TICKS;
            } else if (activationTicksRemaining > 0) {
                activationTicksRemaining--;
            }

            boolean shouldPress = activationTicksRemaining > 0;

            if (shouldPress != wasActivating) {
                setKeyDown(oreExcavationKey, shouldPress);
                wasActivating = shouldPress;
            }
        }
    }

    private static void findOreExcavationKey() {
        try {
            try {
                Class<?> keysClass = Class.forName("oreexcavation.client.ExcavationKeys");
                Field excavateField = keysClass.getDeclaredField("excavateKey");
                excavateField.setAccessible(true);
                Object value = excavateField.get(null);
                if (value instanceof KeyMapping key) {
                    oreExcavationKey = key;
                    keySearchDone = true;
                    OreExcavationAuto.LOGGER.info(
                        "Found Ore Excavation key directly: oreexcavation.key.excavate");
                    return;
                }
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
                // Fall back to key mapping names.
            }

            Minecraft mc = Minecraft.getInstance();
            for (KeyMapping keyMapping : mc.options.keyMappings) {
                String desc = keyMapping.getName();
                if (desc.equals("oreexcavation.key.excavate") ||
                    desc.equals("key.oreexcavation.excavate") ||
                    (desc.toLowerCase().contains("oreexcavation")
                        && desc.toLowerCase().contains("excavate")) ||
                    desc.equals("key.excavated_variants.excavate")) {
                    oreExcavationKey = keyMapping;
                    keySearchDone = true;
                    OreExcavationAuto.LOGGER.info("Found Ore Excavation key: {}", desc);
                    return;
                }
            }
            OreExcavationAuto.LOGGER.warn("Ore Excavation key not found. Make sure Ore Excavation mod is installed.");
        } catch (Exception e) {
            OreExcavationAuto.LOGGER.error("Error finding Ore Excavation key", e);
        }
    }

    private static void setKeyDown(KeyMapping key, boolean down) {
        try {
            key.setDown(down);
        } catch (Exception e) {
            OreExcavationAuto.LOGGER.error("Failed to set Ore Excavation key state", e);
        }
    }
}
