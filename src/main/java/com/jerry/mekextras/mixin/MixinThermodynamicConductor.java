package com.jerry.mekextras.mixin;
import java.util.function.DoubleSupplier;
import com.jerry.mekextras.common.content.network.transmitter.ExtraThermodynamicConductor;
import com.jerry.mekextras.common.tier.transmitter.TCTier;
import mekanism.common.content.network.transmitter.ThermodynamicConductor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
@Mixin(ThermodynamicConductor.class)
public abstract class MixinThermodynamicConductor {
    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lmekanism/common/capabilities/heat/VariableHeatCapacitor;create(DLjava/util/function/DoubleSupplier;Ljava/util/function/DoubleSupplier;Ljava/util/function/DoubleSupplier;Lmekanism/api/IContentsListener;)Lmekanism/common/capabilities/heat/VariableHeatCapacitor;"))
    private void mekextras$heat(Args args) {
        if ((Object) this instanceof ExtraThermodynamicConductor conductor) {
            args.set(0, TCTier.getHeatCapacity(conductor.tier));
            args.set(1, (DoubleSupplier) () -> TCTier.getConduction(conductor.tier));
            args.set(2, (DoubleSupplier) () -> TCTier.getConductionInsulation(conductor.tier));
        }
    }
}
