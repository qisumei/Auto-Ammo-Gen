package com.qis.ammo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.qis.ammo.config.AmmoBoxConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 把 TaCZ（1.21.1 / 1.1.8-hotfix-r6）子弹盒的容量公式换成可配置的分级倍率。
 *
 * <p>TaCZ 原公式（{@code com.tacz.guns.item.AmmoBoxItem}）：
 * <pre>
 *   存入上限：lambda$overrideStackedOnOther$1（实例方法）
 *       max = ammoIndex.getStackSize() × SyncConfig.AMMO_BOX_STACK_SIZE.get() × (getAmmoLevel(stack) + 1)
 *   耐久条：  getBarWidth（实例方法）→ lambda$getBarWidth$2（静态方法）
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
 * <p><b>回调的静态性必须与目标方法一致</b>：
 * {@code lambda$overrideStackedOnOther$1} 与 {@code getBarWidth} 是实例方法，
 * 回调就不能是 static；而 {@code lambda$getBarWidth$2} 是 <b>static</b>，
 * 它的回调也必须是 static，否则会抛
 * {@code InvalidInjectionException: non-static callback method ... targets a static method}。
 *
 * <p>注入都用 {@code require = 1}：TaCZ 若改了内部实现导致注入点找不到，
 * 会在启动时直接报错（而不是静默失效）。已针对 TaCZ 1.21.1 / 1.1.8-hotfix-r6 验证过。
 * 如果你更希望"失效但不崩"，把四处 {@code require = 1} 改成 {@code 0} 即可。
 */
@Mixin(targets = "com.tacz.guns.item.AmmoBoxItem", remap = false)
public class AmmoBoxCapacityMixin {

  /** {@code AmmoBoxItem.getAmmoLevel(ItemStack)}，返回 0/1/2 三个等级。 */
  private static final String GET_AMMO_LEVEL =
      "Lcom/tacz/guns/item/AmmoBoxItem;getAmmoLevel(Lnet/minecraft/world/item/ItemStack;)I";

  /** TaCZ 的全局倍率配置 {@code SyncConfig.AMMO_BOX_STACK_SIZE.get()}。 */
  private static final String GET_AMMO_BOX_STACK_SIZE =
      "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;";

  /** 存入上限：把等级换成配置倍率。目标 {@code lambda$overrideStackedOnOther$1} 是实例方法。 */
  @ModifyExpressionValue(
      method = "lambda$overrideStackedOnOther$1",
      at = @At(value = "INVOKE", target = GET_AMMO_LEVEL),
      remap = false,
      require = 1)
  private int qisammo$depositMultiplier(int originalLevel) {
    return AmmoBoxConfig.multiplierFor(originalLevel) - 1;
  }

  /** 存入上限：抵消 TaCZ 自己的全局倍率。目标同样是实例方法。 */
  @ModifyExpressionValue(
      method = "lambda$overrideStackedOnOther$1",
      at = @At(value = "INVOKE", target = GET_AMMO_BOX_STACK_SIZE),
      remap = false,
      require = 1)
  private Object qisammo$neutralizeGlobalMultiplierForDeposit(Object original) {
    return 1;
  }

  /** 耐久条：把等级换成配置倍率。目标 {@code getBarWidth} 是实例方法。 */
  @ModifyExpressionValue(
      method = "getBarWidth",
      at = @At(value = "INVOKE", target = GET_AMMO_LEVEL),
      remap = false,
      require = 1)
  private int qisammo$barMultiplier(int originalLevel) {
    return AmmoBoxConfig.multiplierFor(originalLevel) - 1;
  }

  /** 耐久条：抵消 TaCZ 自己的全局倍率。目标 {@code lambda$getBarWidth$2} 是 <b>静态</b>方法，回调必须也是 static。 */
  @ModifyExpressionValue(
      method = "lambda$getBarWidth$2",
      at = @At(value = "INVOKE", target = GET_AMMO_BOX_STACK_SIZE),
      remap = false,
      require = 1)
  private static Object qisammo$neutralizeGlobalMultiplierForBar(Object original) {
    return 1;
  }
}
