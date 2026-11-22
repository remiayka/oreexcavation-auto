package com.oreexcavationauto;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.phys.BlockHitResult;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public class OreExcavationAutoClientEvents {

    private static BlockPos lastRequestedPos = null;
    private static long lastRequestGameTime = -100;

    private static void sendOreExcavationPacket(
        BlockPos pos,
        BlockState state,
        ResourceLocation blockId) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.core.Direction facing = mc.player.getDirection();
        net.minecraft.core.Direction side = facing.getOpposite();
        sendOreExcavationPacket(pos, state, blockId, side, facing);
    }

    private static void sendOreExcavationPacket(
        BlockPos pos,
        BlockState state,
        ResourceLocation blockId,
        net.minecraft.core.Direction side,
        net.minecraft.core.Direction facing) {
        try {
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
            sendOreExcavationPacket(
                pos,
                state,
                blockId,
                event.getFace().getOpposite(),
                mc.player.getDirection());
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

        if (!(mc.hitResult instanceof BlockHitResult blockHit)
            || !OreExcavationAutoConfig.isEnabled()) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        ResourceLocation blockId = mc.level.registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
            .getKey(state.getBlock());

        if (blockId == null
            || !OreExcavationAutoConfig.shouldActivate(blockId.toString())) {
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (pos.equals(lastRequestedPos)
            && gameTime - lastRequestGameTime < 40) {
            return;
        }

        lastRequestedPos = pos;
        lastRequestGameTime = gameTime;
        sendOreExcavationPacket(
            pos,
            state,
            blockId,
            blockHit.getDirection().getOpposite(),
            mc.player.getDirection());
    }
}
