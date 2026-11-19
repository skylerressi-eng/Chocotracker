# ChocoTracker

Client-side Fabric mod for Hypixel SkyBlock that tracks Hoppity's Collection rabbits and
Chocolate Factory / Time Tower boosts.

- Minecraft **26.2**, Fabric Loader 0.20.1+, Fabric API 0.161.0+26.2, Java 25
- **Read-only**: it only reads menus, chat, the scoreboard and the tab list. No clicks, no chat or
  commands sent, nothing automated.
- Optional: if the [Hypixel Mod API](https://modrinth.com/mod/hypixel-mod-api) mod is installed,
  its location event is used for island detection. Otherwise, the scoreboard and tab list are used.
- Data lives in `config/chocotracker/` (JSON).

## Status

| Step | State |
|---|---|
| 1. Project structure | done |
| 2. Debug lore dump (`/choco debug`) | **done, waiting for real Hypixel logs** |
| 3a. Collection + Chocolate Factory parsers | todo (built from your logs) |
| 3b. Island detection | todo |
| 3c. Island HUD | todo |
| 3d. `/choco` command | todo |
| 3e. Boost math | todo |

## Build

```bash
./gradlew build
```

The jar is written to `build/libs/chocotracker-<version>.jar`. Copy it into `.minecraft/mods/`
next to Fabric API.

In-game tests run a real client: `./gradlew runClientGameTest -PclientGameTest`. They need a
display; on a headless machine, wrap the command with `xvfb-run -a`.

## Debug mode: collecting logs

1. `/choco debug` turns logging on. Chat shows the log file path:
   `config/chocotracker/debug/dump-YYYYMMDD-HHmmss.log`.
2. Open, in order:
   - `/hoppity` → Hoppity's Collection, and flip through **every** page. Include at least one found
     and one not-found rabbit of each rarity.
   - Any hotspot / hunt view that shows hotspot islands.
   - `/cf` → Chocolate Factory. Leave it open ~20 s. Hover the Time Tower and the CpS/multiplier
     items. Do this once with the Time Tower **inactive** and once **active** if you can.
   - Visit a few islands (Hub, your private island, Crimson Isle, The Park...). A scoreboard and
     tab list snapshot is taken ~3 s after each world change; `/choco debug snapshot` forces one.
3. `/choco debug` again to close the log, then send the file.

Other debug options:
- `/choco debug chat all` logs every chat line, including other players' messages. The default
  logs only lines that mention rabbits, chocolate, the Time Tower, hotspots and similar.
- `/choco debug chat filtered` switches back to the default.

Menus are written when their contents change and stay stable for 5 ticks. Menus that update every
second (like the Chocolate Factory) are re-written at most every 15 s.

## Project layout

```
src/main/java/com/chocotracker/
  ChocoTrackerClient.java       entrypoint, event wiring
  command/ChocoCommand.java     /choco (+ subcommands, tab-complete)
  debug/DebugDumper.java        raw menu/chat/scoreboard/tab dump
  island/HypixelLocationBridge  optional Hypixel Mod API location event
  island/IslandTracker          current island (API → scoreboard → tab list)        [todo]
  data/Rabbit, Rarity, RabbitStore      rabbits.json model + persistence          [todo]
  data/FactoryState, FactoryStore       factory.json (CpS, multiplier, tower)     [todo]
  parse/CollectionParser, FactoryParser built from real logs                       [todo]
  calc/BoostCalc                time-tower math from parsed multipliers            [todo]
  hud/IslandHud, BoostHud, HudEditScreen                                           [todo]
  mixin/PlayerTabOverlayAccessor  read tab header/footer
  util/TextUtil, HudText, Chat
src/gametest/…                  in-game client tests (opt-in)
src/test/…                      unit tests
```
