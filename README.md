# QIS Ammo Automation (qisammo)

TACZ × Create 弹药自动化联动模组（NeoForge 1.21.1）。

## 它做什么

在加载阶段**只读取一次** TACZ 枪包文件夹（`tacz/`），完成三件事：

1. **读取弹药配方** — 解析每个枪包的 `data/<pack>/recipe/ammo/*.json`，得到每种弹药的内含物与火药数量；
2. **自动注册物品** — 为每种弹药注册两个物品（沿用 TACZ 自己的贴图）：
   - `qisammo:incomplete_<口径>` 未完成弹药（序列装配中的半成品）
   - `qisammo:stamp_<口径>` 子弹模板
3. **生成两份包**：
   - **数据包**（服务端自动加载）：切石机把铜板切成模板 + Create 序列装配配方
   - **资源包** `qisammo_resources.zip`（玩家手动加载）：我们注册物品的模型 / 贴图 / 语言文件

## 生产流程

```
铜板(create:copper_sheet)
   │  切石机（stonecutting）
   ▼
模板 qisammo:stamp_<口径>
   │  Create 序列装配（sequenced_assembly）
   │    动力锯 ×1 → 冲压机 ×1 → 机械手 ×N（按原配方数量放入内含物/火药）
   ▼
tacz:ammo（带 AmmoId，真实弹药，一次数十发）
```

- 铜板默认是 `create:copper_sheet`，可通过数据包修改 `qisammo:casing_plate` 标签覆盖。
- 每种弹药产出数量与原枪械工台配方一致（Create 单次产出上限 99，超出部分封顶）。

## 命令（OP）

- `/qisammo refresh` — 重新扫描枪包、重新生成数据包与资源包并重载；新增口径需重启服务器才能注册物品
- `/qisammo give ammo <玩家> <弹药id>` — 直接给真实弹药（测试用）
- `/qisammo give stamp <玩家> <弹药id>` — 直接给模板

## 分发

- 模组 jar 需要客户端/服务端都安装（物品注册双方一致，客户端枪包需与服务端一致）。
- 贴图走独立资源包：服务器启动后生成于 `config/qisammo/qisammo_resources.zip`，玩家放入 `resourcepacks/` 并启用即可，不依赖服务器同步。

## 依赖

- NeoForge 21.1.248（Minecraft 1.21.1）
- TACZ（可选依赖，不装则只注册物品、不生成配方）
- Create（可选依赖，同上）
