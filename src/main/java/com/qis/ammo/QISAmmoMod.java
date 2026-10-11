package com.qis.ammo;

import com.qis.ammo.config.AmmoBoxConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Mod(QISAmmoMod.MODID)
public final class QISAmmoMod {

    public static final String MODID = "qisammo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /** 服务器权威配置的文件名（SERVER 类型；NeoForge 会在登录阶段把它同步给客户端）。 */
    private static final String SERVER_CONFIG_FILE = MODID + "-server.toml";

    /** 旧版本用的 COMMON 配置文件名，保留只是为了顺手清理它的 BOM。 */
    private static final String LEGACY_COMMON_CONFIG_FILE = MODID + "-common.toml";

    public QISAmmoMod(IEventBus modEventBus, ModContainer modContainer) {
        // 必须在 registerConfig 之前：NeoForge 加载时如果解析失败会把整份配置重置成默认值
        stripUtf8BomFromConfig();

        // 子弹盒容量配置。
        // 用 SERVER 而不是 COMMON：NeoForge 只同步 SERVER 类型的配置（ConfigSync/SyncConfig，
        // 登录阶段的 configuration task），COMMON 是两端各读各的文件、互不同步。
        // 用 COMMON 的后果就是"只改服务端不生效、只改客户端看着生效但服务端不认账"。
        modContainer.registerConfig(ModConfig.Type.SERVER, AmmoBoxConfig.SPEC);

        // 把真正生效的倍率打进日志：配置没读进去时一眼就能看出来
        modEventBus.addListener((ModConfigEvent.Loading e) -> logAmmoBoxMultipliers(e.getConfig()));
        modEventBus.addListener((ModConfigEvent.Reloading e) -> logAmmoBoxMultipliers(e.getConfig()));

        AmmoScanner.scanAndGenerate();

        GunpowderFluids.register(modEventBus);
        modEventBus.addListener(this::registerItems);
        modEventBus.addListener(PackFinders::onAddPackFinders);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(GunpowderFluidClient::onRegisterExtensions);
        }

        NeoForge.EVENT_BUS.addListener(RefreshCommand::register);
    }

    /**
     * NeoForge 用的 TOML 解析器（NightConfig）**不接受 UTF-8 BOM**：带 BOM 的配置文件会解析失败，
     * 然后被整份重置成默认值（日志里是 "Failed to load config ... Attempting to recreate"）。
     * 很多 Windows 编辑器（记事本等）保存 UTF-8 时会加上 BOM，所以这里在加载之前先把它去掉。
     */
    private static void stripUtf8BomFromConfig() {
        stripUtf8Bom(SERVER_CONFIG_FILE);
        // 旧版本的 COMMON 配置已经不再被读取，但如果它带着 BOM 留着，只会让人误以为改了有效
        stripUtf8Bom(LEGACY_COMMON_CONFIG_FILE);
    }

    private static void stripUtf8Bom(String fileName) {
        Path file = FMLPaths.CONFIGDIR.get().resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xEF
                    && (bytes[1] & 0xFF) == 0xBB
                    && (bytes[2] & 0xFF) == 0xBF) {
                Files.write(file, Arrays.copyOfRange(bytes, 3, bytes.length));
                LOGGER.warn("[qisammo] {} 带 UTF-8 BOM，已自动去掉（否则 NeoForge 会判定配置损坏并把数值重置成默认值）",
                        fileName);
            }
        } catch (IOException e) {
            LOGGER.error("[qisammo] 检查 {} 的 BOM 失败", fileName, e);
        }
    }

    /**
     * 把当前真正生效的子弹盒倍率打出来。
     *
     * <p>SERVER 配置在客户端是「服务器同步过来的那份」，所以联机时客户端这条日志
     * 显示的就是服务端的值 —— 直接用它确认「服务器权威」有没有生效。
     */
    private static void logAmmoBoxMultipliers(ModConfig config) {
        if (config.getSpec() != AmmoBoxConfig.SPEC) {
            return;
        }
        boolean loaded = AmmoBoxConfig.SPEC.isLoaded();
        LOGGER.info("[qisammo] 子弹盒容量倍率 -> 铁={} 金={} 钻石={}（{}，最终容量 = 该弹种 stackSize × 倍率）",
                loaded ? AmmoBoxConfig.IRON_MULTIPLIER.get() : AmmoBoxConfig.IRON_MULTIPLIER.getDefault(),
                loaded ? AmmoBoxConfig.GOLD_MULTIPLIER.get() : AmmoBoxConfig.GOLD_MULTIPLIER.getDefault(),
                loaded ? AmmoBoxConfig.DIAMOND_MULTIPLIER.get() : AmmoBoxConfig.DIAMOND_MULTIPLIER.getDefault(),
                config.getType() == ModConfig.Type.SERVER
                        ? "SERVER 配置：以服务端 config/" + SERVER_CONFIG_FILE + " 为准，客户端由服务器同步覆盖"
                        : config.getType() + " 配置");
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
