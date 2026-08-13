package com.qis.ammo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QISAmmoMod.MODID)
public final class QISAmmoMod {

    public static final String MODID = "qisammo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public QISAmmoMod(IEventBus modEventBus) {
        AmmoScanner.scanAndGenerate();

        modEventBus.addListener(this::registerItems);
        modEventBus.addListener(PackFinders::onAddPackFinders);

        NeoForge.EVENT_BUS.addListener(RefreshCommand::register);
    }

    private void registerItems(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.ITEM) {
            registerAmmoItems(event);
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            event.register(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(MODID, "main"), QISAmmoMod::buildCreativeTab);
        }
    }

    private void registerAmmoItems(RegisterEvent event) {
        int count = 0;
        for (AmmoData ammo : AmmoScanner.getAmmos()) {
            String path = AmmoData.sanitize(ammo.path());
            if (register(event, "incomplete_" + path)) {
                count++;
            }
            if (register(event, "stamp_" + path)) {
                count++;
            }
        }
        LOGGER.info("[qisammo] registered {} ammo items (incomplete + stamp)", count);
    }

    private boolean register(RegisterEvent event, String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, name);
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return false;
        }
        event.register(Registries.ITEM, id, () -> new Item(new Item.Properties()));
        return true;
    }

    private static Item item(String name) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MODID, name));
    }

    private static CreativeModeTab buildCreativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.qisammo"))
                .icon(() -> {
                    java.util.List<AmmoData> ammos = AmmoScanner.getAmmos();
                    if (ammos.isEmpty()) {
                        return new ItemStack(Items.BARRIER);
                    }
                    Item stamp = item("stamp_" + AmmoData.sanitize(ammos.get(0).path()));
                    return new ItemStack(stamp == Items.AIR ? Items.BARRIER : stamp);
                })
                .displayItems((params, output) -> {
                    for (AmmoData ammo : AmmoScanner.getAmmos()) {
                        String path = AmmoData.sanitize(ammo.path());
                        Item stamp = item("stamp_" + path);
                        Item incomplete = item("incomplete_" + path);
                        if (stamp != Items.AIR) {
                            output.accept(new ItemStack(stamp));
                        }
                        if (incomplete != Items.AIR) {
                            output.accept(new ItemStack(incomplete));
                        }
                    }
                })
                .build();
    }
}
