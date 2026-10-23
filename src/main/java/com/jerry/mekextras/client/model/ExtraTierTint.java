package com.jerry.mekextras.client.model;
import com.mojang.serialization.MapCodec;
import com.jerry.mekextras.common.tier.TierColor;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.api.tier.IAdvancedTier;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import org.jspecify.annotations.Nullable;
public class ExtraTierTint implements ItemTintSource {
    public static final ExtraTierTint INSTANCE=new ExtraTierTint();
    public static final MapCodec<ExtraTierTint> MAP_CODEC=MapCodec.unit(INSTANCE);
    public int calculate(ItemStack stack,@Nullable ClientLevel level,@Nullable LivingEntity owner) {
        if(stack.getItem() instanceof BlockItem block) {
            IAdvancedTier tier=ExtraAttribute.getAdvancedTier(block.getBlock(),IAdvancedTier.class);
            if(tier!=null) return TierColor.getPackedColor(tier);
        }
        return -1;
    }
    public MapCodec<ExtraTierTint> type() { return MAP_CODEC; }
}
