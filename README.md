# Avaritia 无尽贪婪插件

[English](README_EN.md) | 简体中文

## 📖 简介

是的 就是一个普通的插件 把无尽贪婪转成了插件

## 🎮 指令详解

所有指令都支持以下别名：
- `/avaritia` = `/av` = `/avar`

### 基础指令

#### `/av help` 或 `/av ?`
显示帮助信息，列出所有可用指令。

**别名**: `?`  
**权限**: `avaritia.command` (默认所有人)  
**示例**:
```
/av help
/av ?
```

---

#### `/av version` 或 `/av ver`
显示插件版本信息、作者和服务器类型（Folia/Paper/Spigot）。

**别名**: `ver`  
**权限**: `avaritia.command.version` (默认所有人)  
**示例**:
```
/av version
/av ver
```

---

### 物品管理

#### `/av give <玩家> <物品ID> [数量]`
给予玩家插件自定义物品。

**别名**: `item`  
**权限**: `avaritia.give` (默认OP)  
**参数**:
- `<玩家>` - 目标玩家名称（支持Tab补全）
- `<物品ID>` - 物品的内部ID（支持Tab补全）
- `[数量]` - 可选，物品数量（默认为1）

**可用物品ID**:
- **基础材料**: `diamond_lattice`, `crystal_matrix_ingot`, `neutron_ingot`, `infinity_catalyst`, `infinity_ingot`, `infinity_block`
- **奇点**: `iron_singularity`, `gold_singularity`, `lapis_singularity`, `redstone_singularity`, `quartz_singularity`, `diamond_singularity`, `emerald_singularity`, `coal_singularity`, `copper_singularity`
- **无尽工具**: 
  - `world_breaker` - 世界崩解之镐（无尽镐）
  - `planet_heaver` - 行星升降机（无尽锹）
  - `natures_ruin` - 自然毁灭者（无尽斧）
  - `hoe_of_the_stars` - 星辰之锄（无尽锄）
  - `sword_of_the_cosmos` - 无尽寰宇之剑
  - `skullfire_sword` - 骷髅剑
  - `infinity_bow` - 天堂陨落长弓
- **无尽装备**: 
  - `infinity_helmet` - 无尽头盔
  - `infinity_chestplate` - 无尽胸甲
  - `infinity_leggings` - 无尽护腿
  - `infinity_boots` - 无尽靴子
- **特殊物品**: 
  - `endest_pearl` - 末影珍珠
  - `matter_cluster` - 物质团
  - `extreme_table` - 终极工作台
  - `neutronium_compressor` - 中子态素压缩机

**示例**:
```
/av give Little100 infinity_sword 1
/av give @p world_breaker
/av item Steve diamond_lattice 64
```

---

### 语言设置

#### `/av language <语言代码>` 或 `/av lang <语言代码>`
切换插件显示语言（仅对执行命令的玩家生效）。

**别名**: `lang`, `lng`  
**权限**: `avaritia.command.language` (默认所有人)  
**支持的语言**:
- `zh_cn` - 简体中文
- `en_us` - English (US)
- `lzh` - 文言文

**示例**:
```
/av language zh_cn
/av lang en_us
/av lng lzh
```

---

### GUI管理

#### `/av gui <子命令> [参数]`
打开或管理自定义GUI界面。

**别名**: `menu`, `inv`  
**权限**: 
- `avaritia.gui.use` - 使用GUI (默认所有人)
- `avaritia.gui.edit` - 编辑GUI (默认OP)
- `avaritia.gui.create` - 创建GUI (默认OP)

**子命令**:
- `look` - 查看现有GUI
- `edit <大小>` - 编辑GUI（需要OP权限）
- `new <大小>` - 创建新GUI（需要OP权限）

**示例**:
```
/av gui look
/av menu edit 54
/av inv new 27
```

---

### 调试工具

#### `/av nbtdebug` 或 `/av nbt`
显示手持物品的NBT标签调试信息。对于开发和调试自定义物品非常有用。

**别名**: `nbt`, `debug`  
**权限**: `avaritia.command.nbtdebug` (默认OP)  
**使用方法**: 手持物品后执行此命令

**示例**:
```
/av nbtdebug
/av nbt
/av debug
```

---

### 管理指令

#### `/av reload`
重新加载插件配置文件和语言文件。

**权限**: `avaritia.admin.reload` (默认OP)  
**效果**:
- 重载 `config.yml`
- 重载所有语言文件（`zh_cn.yml`, `en_us.yml`, `lzh.yml`）
- 重新初始化语言管理器

**示例**:
```
/av reload
```

---

**作者**: Little_100  
**网站**: [little100.top](https://little100.top)  
**文档**: [docs.little100.top](https://docs.little100.top/guide/avaritia/)  

