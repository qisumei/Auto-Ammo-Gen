package com.qis.ammo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.qis.ammo.config.AmmoBoxConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 把 TaCZ 子弹盒的容量公式换成可配置的分级倍率。
 *
 * <p>TaCZ 原公式（{@code com.tacz.guns.item.AmmoBoxItem}）：
 * <pre>
 *   存入上限：lambda$overrideStackedOnOther$1
 *       max = ammoIndex.getStackSize() × SyncConfig.AMMO_BOX_STACK_SIZE.get() × (getAmmoLevel(stack) + 1)
 *   耐久条：  getBarWidth → lambda$getBarWidth$2
 *       ratio = ammoCount / (ammoIndex.getStackSize() × SyncConfig.AMMO_BOX_STACK_SIZE.get() × (level + 1))
 * </pre>
 *
 * <p>这里做两件事，两处公式一起改，保证「实际能存多少」和「耐久条显示」始终一致：
 * <ol>
 *   <li>把 {@code getAmmoLevel(stack)} 的返回值改成 {@code 倍率 - 1}，
 *       于是 {@code (level + 1)} 变成我们配置的倍率；</li>
 *   <li>把 {@code SyncConfig.AMMO_BOX_STACK_SIZE.get()} 的返回值改成常数 1，
 *       抵消掉 TaCZ 自己的全局倍率。</li>
 * </ol>
 * 最终结果：{@code 容量 = 该弹种 stackSize × 配置倍率}。
 *
 * <p>注入都用 {@code require = 0}：TaCZ 以后改内部实现时只会打日志警告，
 * 不会直接把游戏搞崩。
 */
@Mixin(targets = "com.tacz.guns.item.AmmoBoxItem", remap = false)
public class AmmoBoxCapacityMixin {

  /** {@code AmmoBoxItem.getAmmoLevel(ItemStack)}，返回 0/1/2 三个等级。 */
  private static final String GET_AMMO_LEVEL =
      "Lcom/tacz/guns/item/AmmoBoxItem;getAmmoLevel(Lnet/minecraft/world/item/ItemStack;)I";

  /** TaCZ 的全局倍率配置 {@code SyncConfig.AMMO_BOX_STACK_SIZE.get()}。 */
  private static final String GET_AMMO_BOX_STACK_SIZE =
      "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;";

  /** 存入上限：把等级换成配置倍率。 */
  @ModifyExpressionValue(
      method = "lambda$overrideStackedOnOther$1",
      at = @At(value = "INVOKE", target = GET_AMMO_LEVEL),
      remap = false,
      require = 0)
  private int qisammo$depositMultiplier(int originalLevel) {
    return AmmoBoxConfig.multiplierFor(originalLevel) - 1;
  }

  /** 存入上限：抵消 TaCZ 自己的全局倍率。 */
  @ModifyExpressionValue(
      method = "lambda$overrideStackedOnOther$1",
      at = @At(value = "INVOKE", target = GET_AMMO_BOX_STACK_SIZE),
      remap = false,
      require = 0)
  private Object qisammo$neutralizeGlobalMultiplierForDeposit(Object original) {
    return 1;
  }

  /** 耐久条：把等级换成配置倍率。 */
  @ModifyExpressionValue(
      method = "getBarWidth",
      at = @At(value = "INVOKE", target = GET_AMMO_LEVEL),
      remap = false,
      require = 0)
  private int qisammo$barMultiplier(int originalLevel) {
    return AmmoBoxConfig.multiplierFor(originalLevel) - 1;
  }

  /** 耐久条：抵消 TaCZ 自己的全局倍率。 */
  @ModifyExpressionValue(
      method = "lambda$getBarWidth$2",
      at = @At(value = "INVOKE", target = GET_AMMO_BOX_STACK_SIZE),
      remap = false,
      require = 0)
  private Object qisammo$neutralizeGlobalMultiplierForBar(Object original) {
    return 1;
  }
}
