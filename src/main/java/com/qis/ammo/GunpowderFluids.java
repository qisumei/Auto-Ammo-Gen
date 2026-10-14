package com.qis.ammo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class GunpowderFluids {

    public static final int MB_PER_GUNPOWDER = 10;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(QISAmmoMod.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, QISAmmoMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, QISAmmoMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, QISAmmoMod.MODID);

    public static final DeferredHolder<FluidType, FluidType> GUNPOWDER_TYPE = FLUID_TYPES.register("gunpowder",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1500)
                    .viscosity(2500)
                    .temperature(300)));

    public static final DeferredHolder<Fluid, FlowingFluid> GUNPOWDER = FLUIDS.register("gunpowder",
            GunpowderFluid.Source::new);

    public static final DeferredHolder<Fluid, FlowingFluid> GUNPOWDER_FLOWING = FLUIDS.register("gunpowder_flowing",
            GunpowderFluid.Flowing::new);

    public static final DeferredBlock<Block> GUNPOWDER_BLOCK = BLOCKS.register("gunpowder",
            () -> new GunpowderLiquidBlock(GUNPOWDER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()));

    /**
     * 火药桶。必须由 {@link GunpowderFluid#getBucket()} 返回：{@code LiquidBlock#pickupBlock}
     * 舀起流体时用的就是这个物品，返回 {@code Items.AIR} 会导致桶装/倒出无法闭环。
     * 与桶相关的属性照抄原版水桶（合成后残留空桶、不可堆叠）。
     */
    public static final DeferredHolder<Item, Item> GUNPOWDER_BUCKET = ITEMS.register("gunpowder_bucket",
            () -> new BucketItem(GUNPOWDER.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private GunpowderFluids() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        ITEMS.register(bus);
    }
}
