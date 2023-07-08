package kelvin.slendermod.multiblock;

import com.mojang.datafixers.util.Function3;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class MultiBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty MAIN_BLOCK = BooleanProperty.of("main_block");


    private static final int WIDTH = 1;
    private static final int HEIGHT = 5;
    private static final int DEPTH = 1;

    private final int MAX_BLOCKS = WIDTH * HEIGHT * DEPTH;
    public MultiBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(MAIN_BLOCK, true));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        BlockState state = this.getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite());

        if (pos.getY() < world.getTopY() - 1 && this.preformOnAll(world, state, pos, (worldPos, relativePos) -> world.getBlockState(worldPos).canReplace(ctx))) {
            return state;
        }
        return null;
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        MultiBlockEntity mainEntity = (MultiBlockEntity) world.getBlockEntity(pos);
        this.preformOnAll(world, state, pos, (worldPos, relativePos) -> {
            if (!worldPos.equals(pos)) {
                world.setBlockState(worldPos, state.with(MAIN_BLOCK, false));
                MultiBlockEntity dummyEntity = (MultiBlockEntity) world.getBlockEntity(worldPos);
                if (dummyEntity != null && mainEntity != null) {
                    dummyEntity.setMainBlock(mainEntity.getMainBlock());
                }
            }
            return true;
        });
    }

    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            MultiBlockEntity blockEntity = (MultiBlockEntity) world.getBlockEntity(pos);
            if (blockEntity != null) {
                BlockPos mainPos = blockEntity.getMainBlock();
                this.preformOnAll(world, state, mainPos, (worldPos, relativePos) -> {
                    world.removeBlock(worldPos, false);
                    return true;
                });
            }
        }
        super.onBreak(world, pos, state, player);
    }

    private boolean preformOnAll(World world, BlockState state, BlockPos pos, BiFunction<BlockPos, Vec3i, Boolean> function) {
        MultiBlockEntity mainBlock = (MultiBlockEntity) world.getBlockEntity(pos);
        Direction facing = state.get(FACING);
        Direction clockwise = facing.rotateYClockwise();
        BlockPos startPos = pos.subtract(new Vec3i(
                        (WIDTH/2-1) * facing.getVector().getZ(),
                        0,
                        -(WIDTH/2-1) * facing.getVector().getX()
                )).offset(facing.getOpposite(), 2).offset(clockwise);
        int blocksSet = 0;
        for (int height = 0; height < HEIGHT; height++) {
            BlockPos offsetPos = startPos.up(height);
            for (int width = 0; width < WIDTH; width++) {
                BlockPos rowPos = offsetPos;
                offsetPos = offsetPos.offset(clockwise.getOpposite());
                for (int depth = 0; depth < DEPTH; depth++) {
                    rowPos = rowPos.offset(facing);
                    if (mainBlock != null) {
                        if (WIDTH == 1) {
                            rowPos = new BlockPos(mainBlock.getMainBlock().getX(), rowPos.getY(), rowPos.getZ());
                        }
                        if (DEPTH == 1) {
                            rowPos = new BlockPos(rowPos.getX(), rowPos.getY(), mainBlock.getMainBlock().getZ());
                        }
                    }

                    if (function.apply(rowPos, new Vec3i(width, height, depth))) {
                        blocksSet++;
                    }
                }
            }
        }
        return blocksSet == this.MAX_BLOCKS;
    }

    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, MAIN_BLOCK);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MultiBlockEntity(pos, state);
    }
}