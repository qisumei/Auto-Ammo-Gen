# QIS Ammo Automation (qisammo)

TACZ 与 Create 的弹药自动化联动（NeoForge 1.21.1）。

## 功能

- 读取 `tacz/` 枪包中的弹药配方，为每种弹药注册子弹模板与未完成弹药
- 铜板经切石机切成模板，再用 Create 序列装配产出真实弹药
- 生成数据包，以及贴图资源包 `config/qisammo/qisammo_resources.zip`

## 命令

- `/qisammo refresh`：重新扫描枪包并生成
- `/qisammo give ammo|stamp <玩家> <弹药id>`

## 依赖

- NeoForge 1.21.1
- TACZ、Create（可选）

## 构建

```
gradlew build
```

## 许可

MIT
