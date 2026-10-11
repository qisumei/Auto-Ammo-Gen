package com.qis.ammo.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * TaCZ 在 qisammo 里是可选依赖（{@code mods.toml} 里 type="optional"），
 * 所以没装 TaCZ 时必须跳过 {@link AmmoBoxCapacityMixin}，
 * 否则会因为找不到目标类 {@code com.tacz.guns.item.AmmoBoxItem} 而崩。
 *
 * <p>注意：这里**不能**用 {@code ModList.get().isLoaded("tacz")}。
 * Mixin 在 prepare 阶段就会调用 {@code shouldApplyMixin}，那时 FML 的 ModList
 * 还没初始化，直接 NPE 把游戏搞崩。所以改成查类是否在 classpath 上（只查资源，不加载类）。
 */
public class QisAmmoMixinPlugin implements IMixinConfigPlugin {

  private static final String TACZ_TARGET = "com.tacz.guns.item.AmmoBoxItem";

  @Override
  public void onLoad(String mixinPackage) {
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    return isClassPresent(TACZ_TARGET);
  }

  private static boolean isClassPresent(String className) {
    String resource = className.replace('.', '/') + ".class";
    ClassLoader own = QisAmmoMixinPlugin.class.getClassLoader();
    if (own != null && own.getResource(resource) != null) {
      return true;
    }
    ClassLoader context = Thread.currentThread().getContextClassLoader();
    return context != null && context.getResource(resource) != null;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
  }

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
  }

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
  }
}
