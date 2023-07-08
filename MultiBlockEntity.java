package kelvin.slendermod.multiblock;

import kelvin.slendermod.registry.BlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class MultiBlockEntity extends BlockEntity {
    private BlockPos mainBlock;

    public MultiBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MULTI_BLOCK_ENTITY, pos, state);
        if (state.get(MultiBlock.MAIN_BLOCK)) {
            this.setMainBlock(pos);
        }
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.mainBlock = BlockPos.fromLong(nbt.getLong("MainBlockPos"));
    }

    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("MainBlockPos", BlockPos.asLong(this.mainBlock.getX(), this.mainBlock.getY(), this.mainBlock.getZ()));
    }

    public BlockPos getMainBlock() {
        return this.mainBlock;
    }
    public void setMainBlock(BlockPos newPos) {
        this.mainBlock = newPos;
    }
}
