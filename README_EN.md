# Avaritia Plugin

English | [简体中文](README.md)

## 📖 Introduction

Uhmm Yes this is a simple plugin just from avaitia mod to plugin.

## 🎮 Commands Reference

All commands support the following aliases:
- `/avaritia` = `/av` = `/avar`

### Basic Commands

#### `/av help` or `/av ?`
Display help information with all available commands.

**Alias**: `?`  
**Permission**: `avaritia.command` (default: everyone)  
**Examples**:
```
/av help
/av ?
```

---

#### `/av version` or `/av ver`
Display plugin version, author information, and server type (Folia/Paper/Spigot).

**Alias**: `ver`  
**Permission**: `avaritia.command.version` (default: everyone)  
**Examples**:
```
/av version
/av ver
```

---

### Item Management

#### `/av give <player> <itemID> [amount]`
Give custom plugin items to a player.

**Alias**: `item`  
**Permission**: `avaritia.give` (default: OP)  
**Parameters**:
- `<player>` - Target player name (supports tab completion)
- `<itemID>` - Internal item ID (supports tab completion)
- `[amount]` - Optional, item quantity (defaults to 1)

**Available Item IDs**:
- **Basic Materials**: `diamond_lattice`, `crystal_matrix_ingot`, `neutron_ingot`, `infinity_catalyst`, `infinity_ingot`, `infinity_block`
- **Singularities**: `iron_singularity`, `gold_singularity`, `lapis_singularity`, `redstone_singularity`, `quartz_singularity`, `diamond_singularity`, `emerald_singularity`, `coal_singularity`, `copper_singularity`
- **Infinity Tools**: 
  - `world_breaker` - World Breaker (Infinity Pickaxe)
  - `planet_heaver` - Planet Heaver (Infinity Shovel)
  - `natures_ruin` - Nature's Ruin (Infinity Axe)
  - `hoe_of_the_stars` - Hoe of the Stars (Infinity Hoe)
  - `sword_of_the_cosmos` - Sword of the Cosmos
  - `skullfire_sword` - Skullfire Sword
  - `infinity_bow` - Infinity Bow
- **Infinity Armor**: 
  - `infinity_helmet` - Infinity Helmet
  - `infinity_chestplate` - Infinity Chestplate
  - `infinity_leggings` - Infinity Leggings
  - `infinity_boots` - Infinity Boots
- **Special Items**: 
  - `endest_pearl` - Endest Pearl
  - `matter_cluster` - Matter Cluster
  - `extreme_table` - Extreme Crafting Table
  - `neutronium_compressor` - Neutronium Compressor

**Examples**:
```
/av give Little100 infinity_sword 1
/av give @p world_breaker
/av item Steve diamond_lattice 64
```

---

### Language Settings

#### `/av language <language_code>` or `/av lang <language_code>`
Switch plugin display language (only affects the player executing the command).

**Aliases**: `lang`, `lng`  
**Permission**: `avaritia.command.language` (default: everyone)  
**Supported Languages**:
- `zh_cn` - Simplified Chinese (简体中文)
- `en_us` - English (US)
- `lzh` - Classical Chinese (文言文)

**Examples**:
```
/av language zh_cn
/av lang en_us
/av lng lzh
```

---

### GUI Management

#### `/av gui <subcommand> [parameters]`
Open or manage custom GUI interfaces.

**Aliases**: `menu`, `inv`  
**Permissions**: 
- `avaritia.gui.use` - Use GUI (default: everyone)
- `avaritia.gui.edit` - Edit GUI (default: OP)
- `avaritia.gui.create` - Create GUI (default: OP)

**Subcommands**:
- `look` - View existing GUI
- `edit <size>` - Edit GUI (requires OP permission)
- `new <size>` - Create new GUI (requires OP permission)

**Examples**:
```
/av gui look
/av menu edit 54
/av inv new 27
```

---

### Debug Tools

#### `/av nbtdebug` or `/av nbt`
Display NBT tag debug information for the held item. Very useful for development and debugging custom items.

**Aliases**: `nbt`, `debug`  
**Permission**: `avaritia.command.nbtdebug` (default: OP)  
**Usage**: Hold an item and execute this command

**Examples**:
```
/av nbtdebug
/av nbt
/av debug
```

---

### Admin Commands

#### `/av reload`
Reload plugin configuration and language files.

**Permission**: `avaritia.admin.reload` (default: OP)  
**Effects**:
- Reload `config.yml`
- Reload all language files (`zh_cn.yml`, `en_us.yml`, `lzh.yml`)
- Reinitialize language manager

**Examples**:
```
/av reload
```

---

**Author**: Little_100  
**Website**: [little100.top](https://little100.top)  
**Documentation**: [docs.little100.top](https://docs.little100.top/guide/avaritia/)

