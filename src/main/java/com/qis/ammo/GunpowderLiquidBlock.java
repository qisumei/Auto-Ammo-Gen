package com.qis.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * 火药流体方块：遇到明火立刻爆炸。
 *
 * <p>「明火」的判定见 {@link #isFlame}：火焰/灵魂火、岩浆（含流动岩浆）、岩浆块、
 * 火把与灯笼、以及**点燃状态**的营火/蜡烛。封闭的火（如点燃的熔炉）不算。
 *
 * <p>引爆走「方块计划刻」而不是在邻居更新里直接爆炸，原因有两个：
 * <ol>
 *   <li>避免递归：一片火药池逐个邻居连锁会形成 N 层递归调用栈，池子一大就爆栈；</li>
 *   <li>观感更好：整池火药按距离逐 tick 次第炸开，而不是同一瞬间全炸。</li>
 * </ol>
 */
public class GunpowderLiquidBlock extends LiquidBlock {

    /** 爆炸威力，与原版 TNT 同级（TNT 为 4.0F）。 */
    private static final float EXPLOSION_POWER = 4.0F;

    /** 点火到爆炸之间的延迟（tick）。1 基本等于「碰到就炸」。 */
    private static final int FUSE_TICKS = 1;

    public GunpowderLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 流体是流过来的，用 onPlace 抓「新流到明火旁边」这种情况
        igniteIfTouchingFlame(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        // 明火是后来放在火药旁边的，用 neighborChanged 抓
        igniteIfTouchingFlame(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 位置可能已经被上一轮爆炸清掉了，别重复炸
        if (!(level.getBlockState(pos).getBlock() instanceof GunpowderLiquidBlock)) {
            return;
        }
        level.removeBlock(pos, false);

        // 连锁：把相邻的火药流体排到下一 tick，形成逐格扩散的爆炸
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            Block neighbourBlock = level.getBlockState(neighbour).getBlock();
            if (neighbourBlock instanceof GunpowderLiquidBlock) {
                level.scheduleTick(neighbour, neighbourBlock, FUSE_TICKS);
            }
        }

        level.explode(null,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
    }

    private void igniteIfTouchingFlame(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        if (touchesFlame(level, pos)) {
            level.scheduleTick(pos, this, FUSE_TICKS);
        }
    }

    private boolean touchesFlame(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isFlame(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFlame(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.LAVA)) {
            return true;
        }

        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
            return true;
        }
        // 营火和蜡烛只有点燃时才算明火
        if (state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof CandleBlock) {
            return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
        }
        return false;
    }
}
