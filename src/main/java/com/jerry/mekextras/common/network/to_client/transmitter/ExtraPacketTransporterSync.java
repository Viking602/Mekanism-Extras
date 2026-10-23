package com.jerry.mekextras.common.network.to_client.transmitter;

import com.jerry.mekextras.MekanismExtras;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraLogisticalTransporterBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExtraPacketTransporterSync(long pos, int stackId, TransporterStack stack) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<ExtraPacketTransporterSync> TYPE = new CustomPacketPayload.Type<>(MekanismExtras.rl("transporter_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraPacketTransporterSync> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_LONG, ExtraPacketTransporterSync::pos,
          ByteBufCodecs.VAR_INT, ExtraPacketTransporterSync::stackId,
          TransporterStack.STREAM_CODEC, ExtraPacketTransporterSync::stack,
          ExtraPacketTransporterSync::new
    );

    public static ExtraPacketTransporterSync create(long pos, int stackId, TransporterStack stack) {
        return new ExtraPacketTransporterSync(pos, stackId, stack.updateForPos(pos));
    }

    @Override
    public CustomPacketPayload.Type<ExtraPacketTransporterSync> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, pos) instanceof TileEntityExtraLogisticalTransporterBase tile) {
            tile.getTransmitter().addStack(stackId, stack);
        }
    }
}