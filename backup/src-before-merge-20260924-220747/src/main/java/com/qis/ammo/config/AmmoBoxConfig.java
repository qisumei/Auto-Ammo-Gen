package com.qis.ammo.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 子弹盒容量配置。
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
      case 1 -> GOLD_MULTIPLIER.get();
      case 2 -> DIAMOND_MULTIPLIER.get();
      default -> IRON_MULTIPLIER.get();
    };
  }
}
