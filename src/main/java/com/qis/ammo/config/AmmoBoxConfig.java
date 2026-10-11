package com.qis.ammo.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 子弹盒容量配置（<b>服务器权威</b>）。
 *
 * <p>TaCZ 的原始容量公式是：
 * <pre>容量 = 该弹种 stackSize × TaCZ 配置 AmmoBoxStackSize × (盒子等级 + 1)</pre>
 * 其中铁/金/钻石盒子的等级分别是 0/1/2，也就是硬编码的 1:2:3。
 *
 * <p>本模组把「(等级 + 1) × AmmoBoxStackSize」这一整块换成了下面三个可配置倍率，
 * 于是容量变成：
 * <pre>容量 = 该弹种 stackSize × 对应等级的倍率</pre>
 * 默认值 3 / 6 / 9 与 TaCZ 原版（AmmoBoxStackSize=3 时）完全一致。
 *
 * <p><b>为什么是 SERVER 而不是 COMMON：</b>NeoForge 只会把
 * {@link net.neoforged.fml.config.ModConfig.Type#SERVER} 配置在登录阶段（configuration
 * phase）同步给客户端，见 {@code net.neoforged.neoforge.network.ConfigSync} /
 * {@code SyncConfig}；COMMON 配置两端各自读各自的文件、互不同步，于是"只改服务端"
 * 不会生效、"只改客户端"看起来生效但服务端并不认账。改成 SERVER 之后：
 * <ul>
 *   <li>服务端的 {@code config/qisammo-server.toml} 是唯一权威来源；</li>
 *   <li>客户端连上服务器时会收到这份配置并覆盖本地读到的值，
 *       所以「耐久条显示的上限」和「实际能存多少」永远一致；</li>
 *   <li>客户端本地的同名文件在联机时不起作用（单人游戏则由内置服务端读取）。</li>
 * </ul>
 *
 * <p>创造弹药盒与全类型创造弹药盒是 {@code Integer.MAX_VALUE}，不受这里影响。
 */
public final class AmmoBoxConfig {

  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec.IntValue IRON_MULTIPLIER;
  public static final ModConfigSpec.IntValue GOLD_MULTIPLIER;
  public static final ModConfigSpec.IntValue DIAMOND_MULTIPLIER;

  static {
    ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    builder.push("ammo_box");

    IRON_MULTIPLIER = builder
        .comment("铁弹药盒容量倍率，最终容量 = 该弹种 stackSize × 此值（TaCZ 原版等效值：3）")
        .defineInRange("iron_multiplier", 3, 1, Integer.MAX_VALUE);

    GOLD_MULTIPLIER = builder
        .comment("金弹药盒容量倍率（TaCZ 原版等效值：6）")
        .defineInRange("gold_multiplier", 6, 1, Integer.MAX_VALUE);

    DIAMOND_MULTIPLIER = builder
        .comment("钻石弹药盒容量倍率（TaCZ 原版等效值：9）")
        .defineInRange("diamond_multiplier", 9, 1, Integer.MAX_VALUE);

    builder.pop();
    SPEC = builder.build();
  }

  private AmmoBoxConfig() {
  }

  /**
   * 按弹药盒等级取倍率。
   *
   * @param level TaCZ 的弹药盒等级：0 = 铁，1 = 金，2 = 钻石
   */
  public static int multiplierFor(int level) {
    return switch (level) {
      case 1 -> read(GOLD_MULTIPLIER);
      case 2 -> read(DIAMOND_MULTIPLIER);
      default -> read(IRON_MULTIPLIER);
    };
  }

  /**
   * 安全读取。
   *
   * <p>SERVER 配置在客户端要等服务器把配置同步过来之后才可读；在那之前
   * {@link ModConfigSpec.ConfigValue#get()} 会抛 {@code IllegalStateException}
   * （内部是 {@code Preconditions.checkState}）。而未连接任何服务器时
   * {@code getBarWidth} 仍可能被调用（主菜单、单人存档加载前等），所以未加载时
   * 退回默认值 3 / 6 / 9 —— 正好等于 TaCZ 原版行为。
   */
  private static int read(ModConfigSpec.IntValue value) {
    return SPEC.isLoaded() ? value.get() : value.getDefault();
  }
}
