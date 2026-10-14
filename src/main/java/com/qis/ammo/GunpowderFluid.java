package com.qis.ammo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class GunpowderFluid extends FlowingFluid {

    @Override
    public FluidType getFluidType() {
        return GunpowderFluids.GUNPOWDER_TYPE.get();
    }

    @Override
    public Fluid getSource() {
        return GunpowderFluids.GUNPOWDER.get();
    }

    @Override
    public Fluid getFlowing() {
        return GunpowderFluids.GUNPOWDER_FLOWING.get();
    }

    @Override
    public Item getBucket() {
        return GunpowderFluids.GUNPOWDER_BUCKET.get();
    }

    /**
     * 与原版水一致：只允许「从上方落下」的非水液体替换自己。
     *
     * <p>注意 {@code Fluid#canBeReplacedWith} 是抽象方法，{@code FlowingFluid} 并没有给出默认实现
     * （只有 {@code WaterFluid}/{@code LavaFluid} 各自实现了），所以这里不能直接删掉，必须照抄默认语义。
     * 之前恒返回 {@code false}，会让水等液体永远无法替换火药流体。
     */
    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.is(FluidTags.WATER);
    }

    // 不覆写 getFlow()：FlowingFluid 的默认实现会按相邻液面算出流向，
    // 既让流动贴图有方向/滚动动画，也让实体被水流推动。
    // 原先恒返回 Vec3.ZERO 会把这两件事一起关掉。

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        // 必须把流体等级写进方块 LEVEL，否则 LiquidBlock.getFluidState 永远按 LEVEL=0 回读成
        // 源头状态：世界里只会出现 8 格高的源头、流动贴图/流动等级永不生效，而且可能无限蔓延。
        return GunpowderFluids.GUNPOWDER_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == GunpowderFluids.GUNPOWDER.get() || fluid == GunpowderFluids.GUNPOWDER_FLOWING.get();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 1;
    }

    public static final class Source extends GunpowderFluid {

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends GunpowderFluid {

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
