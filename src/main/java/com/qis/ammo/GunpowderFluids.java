package com.qis.ammo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
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
            () -> new LiquidBlock(GUNPOWDER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()));

    private GunpowderFluids() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
