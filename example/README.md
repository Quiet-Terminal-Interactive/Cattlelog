# Cattlelog Example Server

A minimal Minestom server that demonstrates [Cattlelog](../README.md) integration.

## What it does

- Starts a Minestom server with a flat grass world
- Initializes Cattlelog to persist player data (inventory, position, health, XP, game mode)
- Enables physical cow entities — each saved player gets a cow spawned at their last position

## Prerequisites

- Java 25+
- Cattlelog installed to your local Maven repository

## Build & Run

1. **Install Cattlelog** to your local Maven repo (from the project root):

   ```bash
   cd ..
   mvn install
   ```

2. **Build the example server:**

   ```bash
   cd example
   mvn package
   ```

3. **Run it:**

   ```bash
   java -jar target/cattlelog-example-1.0.0.jar
   ```

4. **Connect** with Minecraft 1.21.1 to `localhost:25565`.

## How Cattlelog works here

The entire integration is two lines:

```java
// Initialize — starts saving/loading player data automatically
Cattlelog.initialize(Path.of("player-data"));

// Optional — enable physical cow entities in the world
CowHerd herd = Cattlelog.enablePhysicalCows();
```

Once initialized, Cattlelog automatically:
- **Loads** player data (position, inventory, stats) when they join
- **Saves** player data to `.cow` files when they disconnect

The `CowHerd` is optional and lets you spawn cow entities representing saved players.
