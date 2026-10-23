package com.jerry.mekextras.mixin;
import com.jerry.mekextras.common.content.network.transmitter.ExtraLogisticalTransporter;
import com.jerry.mekextras.common.tier.transmitter.TPTier;
import com.jerry.mekextras.common.network.to_client.transmitter.ExtraPacketTransporterSync;
import com.jerry.mekextras.common.network.to_client.transmitter.ExtraPacketTransporterBatch;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.network.to_client.transmitter.PacketTransporterSync;
import mekanism.common.network.to_client.transmitter.PacketTransporterBatch;
import mekanism.common.tier.TransporterTier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(LogisticalTransporterBase.class)
public abstract class MixinLogisticalTransporterBase {
    @Redirect(method = {"onUpdateClient", "tickTransit"}, at = @At(value = "INVOKE", target = "Lmekanism/common/tier/TransporterTier;getSpeed()I"))
    private int mekextras$speed(TransporterTier tier) {
        return (Object) this instanceof ExtraLogisticalTransporter ? TPTier.getSpeed(tier) : tier.getSpeed();
    }
    @Redirect(method = "getCost", at = @At(value = "INVOKE", target = "Lmekanism/common/tier/TransporterTier;getSpeed()I", ordinal = 1))
    private int mekextras$cost(TransporterTier tier) {
        return (Object) this instanceof ExtraLogisticalTransporter ? TPTier.getSpeed(tier) : tier.getSpeed();
    }
    @Redirect(method = "tryPull", at = @At(value = "INVOKE", target = "Lmekanism/common/tier/TransporterTier;getPullAmount()I"))
    private int mekextras$pull(TransporterTier tier) {
        return (Object) this instanceof ExtraLogisticalTransporter ? TPTier.getPullAmount(tier) : tier.getPullAmount();
    }
    @ModifyArg(method = {"onUpdateServer", "emitStack"}, at = @At(value = "INVOKE", target = "Lmekanism/common/network/PacketUtils;sendToAllTracking(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"), index = 0)
    private CustomPacketPayload mekextras$sync(CustomPacketPayload packet) {
        if ((Object) this instanceof ExtraLogisticalTransporter) {
            if (packet instanceof PacketTransporterSync sync) return new ExtraPacketTransporterSync(sync.pos(), sync.stackId(), sync.stack());
            if (packet instanceof PacketTransporterBatch batch) return new ExtraPacketTransporterBatch(batch.pos(), batch.deletes(), batch.updates());
        }
        return packet;
    }
}
