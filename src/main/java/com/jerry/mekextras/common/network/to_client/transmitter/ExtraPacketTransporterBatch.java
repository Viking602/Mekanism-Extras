package com.jerry.mekextras.common.network.to_client.transmitter;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import com.jerry.mekextras.MekanismExtras;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraLogisticalTransporterBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExtraPacketTransporterBatch(long pos, IntSet deletes, Int2ObjectMap<TransporterStack> updates) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<ExtraPacketTransporterBatch> TYPE = new CustomPacketPayload.Type<>(MekanismExtras.rl("transporter_batch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraPacketTransporterBatch> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_LONG, ExtraPacketTransporterBatch::pos,
          ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.collection(IntOpenHashSet::new)), ExtraPacketTransporterBatch::deletes,
          ByteBufCodecs.map(Int2ObjectOpenHashMap::new, ByteBufCodecs.VAR_INT, TransporterStack.STREAM_CODEC), ExtraPacketTransporterBatch::updates,
          ExtraPacketTransporterBatch::new
    );

    public static ExtraPacketTransporterBatch create(long pos, IntSet deletes, Int2ObjectMap<TransporterStack> updates) {
        for (TransporterStack stack : updates.values()) {
            stack.updateForPos(pos);
        }
        return new ExtraPacketTransporterBatch(pos, deletes, updates);
    }

    @Override
    public CustomPacketPayload.Type<ExtraPacketTransporterBatch> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, pos) instanceof TileEntityExtraLogisticalTransporterBase tile) {
            LogisticalTransporterBase transporter = tile.getTransmitter();
            for (ObjectIterator<Int2ObjectMap.Entry<TransporterStack>> iterator = Int2ObjectMaps.fastIterator(updates); iterator.hasNext(); ) {
                Int2ObjectMap.Entry<TransporterStack> entry = iterator.next();
                transporter.addStack(entry.getIntKey(), entry.getValue());
            }
            for (int toDelete : deletes) {
                transporter.deleteStack(toDelete);
            }
        }
    }
}