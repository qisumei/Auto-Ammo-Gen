package com.qis.ammo;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class GunpowderFluidClient {

    private static final ResourceLocation STILL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(QISAmmoMod.MODID, "block/gunpowder_fluid");
    private static final ResourceLocation FLOWING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(QISAmmoMod.MODID, "block/gunpowder_fluid_flow");

    private GunpowderFluidClient() {
    }

    public static void onRegisterExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }
        }, GunpowderFluids.GUNPOWDER_TYPE.get());
    }
}
