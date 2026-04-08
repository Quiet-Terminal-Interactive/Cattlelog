# Cattlelog

Persists Minestom player data in cow-themed `.cow` NBT files. When a player disconnects, their state is serialized into a GZIP-compressed NBT compound structured like a cow entity. When they rejoin, the cow is loaded and their state is restored.

## Requirements

- Java 25
- Minestom 2026.01.08-1.21.11

## Setup

Add Cattlelog as a dependency in your Minestom server project, then call:

```java
Cattlelog.initialize();
```

That's it. Player data will be saved to `./barn/<uuid>.cow` on disconnect and loaded on join.

For a custom directory:

```java
Cattlelog.initialize(Path.of("data", "player_cows"));
```

## What Gets Saved

| Data            | NBT Key           |
|-----------------|-------------------|
| Health          | `Health`          |
| Food level      | `FeedLevel`       |
| Food saturation | `FeedSaturation`  |
| XP level        | `HerdRank`        |
| XP progress     | `GrazingProgress` |
| Game mode       | `Temperament`     |
| Position        | `Pasture`         |
| Inventory       | `Udder`           |
| Effects         | `Brands`          |
| Held slot       | `GrazingSlot`     |

All player stats live under a `BrandingIron` compound.

## Physical Cows

Optionally spawn real cow entities where players log off. When a player disconnects, a named cow appears at their position. When they rejoin, the cow disappears.

```java
Cattlelog.initialize();
CowHerd herd = Cattlelog.enablePhysicalCows();
```

After your instance is ready, spawn cows for all saved players at once:

```java
herd.summonHerd(instance);
```

The `CowHerd` also provides:
- `herd.removeCow(uuid)` - remove a specific cow
- `herd.getCow(uuid)` - get the Entity for a player's cow
- `herd.disperseHerd()` - remove all cows
- `herd.size()` - how many cows are standing around

## Building

```
mvn package
```

The output JAR will be in `target/`. Minestom is a `provided` dependency so it won't be bundled.
