package com.jerry.genextras.common.content.naquadah;

import java.util.EnumSet;
import java.util.Objects;
import mekanism.common.MekanismLang;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.math.voxel.VoxelCuboid.CuboidSide;
import mekanism.common.lib.math.voxel.VoxelCuboid.WallRelative;
import mekanism.common.lib.multiblock.CuboidStructureValidator;
import mekanism.common.lib.multiblock.FormationProtocol;
import mekanism.common.lib.multiblock.FormationProtocol.CasingType;
import mekanism.common.lib.multiblock.FormationProtocol.FormationResult;
import mekanism.common.lib.multiblock.FormationProtocol.StructureRequirement;
import mekanism.common.lib.multiblock.Structure.Axis;
import mekanism.common.lib.multiblock.StructureHelper;
import com.jerry.genextras.common.registries.GenExtraBlockTypes;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NaquadahReactorValidator extends CuboidStructureValidator<NaquadahReactorMultiblockData> {

    private static final VoxelCuboid BOUNDS = new VoxelCuboid(9, 9, 9);
    private static final byte[][] ALLOWED_GRID = {
        {0,0,0,1,1,1,0,0,0}, {0,1,1,2,2,2,1,1,0}, {0,1,2,2,2,2,2,1,0},
        {1,2,2,2,2,2,2,2,1}, {1,2,2,2,2,2,2,2,1}, {1,2,2,2,2,2,2,2,1},
        {0,1,2,2,2,2,2,1,0}, {0,1,1,2,2,2,1,1,0}, {0,0,0,1,1,1,0,0,0}
    };

    @Override
    protected StructureRequirement getStructureRequirement(BlockPos pos, VoxelCuboid cuboid) {
        WallRelative relative = cuboid.getWallRelative(pos);
        if (relative.isWall()) {
            Direction side = Objects.requireNonNull(cuboid.getSide(pos), "Side should not be null when part of a wall");
            Axis axis = Axis.get(side);
            Axis h = axis.horizontal(), v = axis.vertical();
            //Note: This ends up becoming immutable by doing this but that is fine and doesn't really matter
            pos = pos.subtract(cuboid.getMinPos());
            return StructureRequirement.REQUIREMENTS[ALLOWED_GRID[h.getCoord(pos)][v.getCoord(pos)]];
        }
        return super.getStructureRequirement(pos, cuboid);
    }

    @Override
    protected FormationResult validateFrame(FormationProtocol<NaquadahReactorMultiblockData> ctx, BlockPos pos, BlockState state, CasingType type, boolean needsFrame, VoxelCuboid cuboid) {
        boolean isControllerPos = pos.getY() == cuboid.getMaxPos().getY() && pos.getX() == cuboid.getMinPos().getX() + 4 && pos.getZ() == cuboid.getMinPos().getZ() + 4;
        boolean controller = structure().getTile(pos) instanceof TileEntityNaquadahReactorController;
        if (isControllerPos && !controller) {
            return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_NO_CONTROLLER);
        } else if (!isControllerPos && controller) {
            //When the controller is potentially outside the multiblock we need to make sure to not allow ignoring the failure
            // as otherwise we may allow duplicate controllers
            return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_CONTROLLER_CONFLICT, pos, true);
        }
        return super.validateFrame(ctx, pos, state, type, needsFrame, cuboid);
    }

    @Override
    protected CasingType getCasingType(BlockState state) {
        Block block = state.getBlock();
        if (BlockType.is(block, GenExtraBlockTypes.NAQUADAH_REACTOR_CASING)) {
            return CasingType.FRAME;
        } else if (BlockType.is(block, GenExtraBlockTypes.NAQUADAH_REACTOR_PORT)) {
            return CasingType.VALVE;
        } else if (BlockType.is(block, GenExtraBlockTypes.NAQUADAH_REACTOR_CONTROLLER,
              GenExtraBlockTypes.NAQUADAH_REACTOR_LOGIC_ADAPTER, GenExtraBlockTypes.LEAD_COATED_LASER_FOCUS_MATRIX)) {
            return CasingType.OTHER;
        }
        return CasingType.INVALID;
    }

    @Override
    public boolean precheck() {
        // 120 = 20 optional corner positions per face across six faces
        return precheck(StructureHelper.fetchCuboid(structure(), BOUNDS, BOUNDS, EnumSet.allOf(CuboidSide.class), 120));
    }
}
