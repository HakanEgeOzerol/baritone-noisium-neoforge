# Baritone Noisium NeoForge - The Epic Debugging Saga

## Partial Breakthrough! 🟡

After days of debugging, **WE HAVE ACHIEVED BASIC MOVEMENT!** This repository documents an incredible journey from complete failure to partial progress in making Baritone work with complex mod environments on NeoForge 1.21.1. However, actual pathfinding execution remains broken.

## Current Status: MOVEMENT ACHIEVED BUT NETWORK BLOCKED! 🚧

**✅ WORKING:**
- Path calculation (✓)
- Command processing (✓) 
- Input system replacement (✓)
- **PLAYER MOVEMENT** (✓) - *The player now moves!*

**⚠️ PARTIALLY WORKING:**
- **Basic movement achieved** - Player can move forward with nuclear input forcing
- **Path calculation works** - Baritone calculates paths successfully
- **BUT**: Movement doesn't follow calculated paths (goes toward camera instead)

**❌ CURRENT BLOCKER:**
- **Network registration limit exceeded** - Cannot connect to server
- Error: `End size 612 is less than fixed size 613`
- Prevents all testing until resolved

**❌ CORE MOVEMENT ISSUES (Even if network was fixed):**
- **PathExecutor objects never created** - Calculated paths don't become executable
- **PathExecutor.onTick() never called** - No path execution happening
- **All movement inputs stay `false`** - Nuclear option only forces forward movement
- Movement goes toward camera direction, not calculated path
- Block breaking/mining system still broken

## The Epic Journey

### Day 1: Total Failure
- **The Problem**: Baritone's `MixinClientPlayerEntity` failed with `InvalidInjectionException`
- **Environment**: 100+ mod server with Create, JourneyMap, KubeJS, etc.
- **Reality Check**: Even official Baritone releases failed completely

### Day 2: Systematic Elimination 
- Removed performance mods (Sodium, Lithium, InvMove, etc.)
- Discovered the root cause: **Input replacement system completely broken**
- Debug logs showed: `KeyboardInput` never changed to `PlayerMovementInput`

### Day 3: The Nuclear Option
- **BREAKTHROUGH**: Implemented aggressive input forcing system
- **Method**: Direct field manipulation + forced input states every tick
- **Result**: Player movement achieved! (But path following broken)

## The Technical Journey

### Phase 1: Mixin Hell
```java
// Original approach - Complete failure
@Redirect(method = "tick", at = @At(value = "INVOKE", 
    target = "isAllowFlying()Z"))
// Result: InvalidInjectionException - target not found
```

### Phase 2: Optional Mixins
```java
// Made redirects optional with require = 0
@Redirect(require = 0, method = "tick", ...)
// Result: No crashes, but no functionality
```

### Phase 3: The Nuclear Solution
```java
// Direct input system takeover
if (inControl && isPathing) {
    // Replace input system entirely
    setInput(new PlayerMovementInput(...));
    // Force movement states directly
    setInputForceState(Input.MOVE_FORWARD, true);
    // Manipulate fields directly as backup
    input.up = true;
    input.forwardImpulse = 1.0F;
}
```

## Key Discoveries

### The Root Cause  
- **Not a mixin conflict** as originally thought
- **Input replacement system interference** by other mods
- Baritone successfully replaced input but something immediately overwrote it
- **Solution**: Bypass normal input system entirely

### Debug Process Evolution
1. **Mixin redirect logging** → Found redirects weren't being called
2. **Input replacement tracking** → Found input class never changed  
3. **Comprehensive input monitoring** → Found replacement happened then was overwritten
4. **Nuclear input forcing** → **BREAKTHROUGH!**

### The Environment Challenge
- **100+ mods** creating complex interaction patterns
- **Noisium compatibility** required special handling
- **Multiple mod loader variants** (NeoForge + custom builds)

## Current Files & Status

### Modified Files
- `src/launch/java/baritone/launch/mixins/MixinClientPlayerEntityNoisium.java` - Nuclear input option
- `src/main/java/baritone/utils/InputOverrideHandler.java` - Enhanced with aggressive forcing
- `src/api/java/baritone/api/utils/BlockOptionalMeta.java` - KubeJS compatibility fix

### Latest Build
- `baritone-standalone-noisium-neoforge-1.11.2-11-g9db25b01-dirty.jar` (cleaned up version)
- **Status**: Movement achieved, but blocked by network registration limit

## What's Next

### Critical Issues (Blocking deployment)
1. **Network Registration Limit**: Fix connection error to enable testing
2. **PathExecutor Creation**: Debug why calculated paths don't become executable
3. **Path Following**: Make movement follow calculated paths, not camera direction

### Secondary Issues (After core fixes)
1. **Mining System**: Implement block breaking with compatible approach
2. **Movement Precision**: Fine-tune path following accuracy
3. **Performance**: Optimize nuclear input forcing system

### Technical Reality Check  
**Current "movement"** = Nuclear option forcing forward movement only  
**Actual pathfinding** = Still completely broken (PathExecutor system non-functional)  
**Real goal** = Proper path execution + movement that follows calculated routes

## Build Instructions

```bash
# Build the breakthrough version
./gradlew :noisium-neoforge:build

# For regular NeoForge (if your environment supports it)
./gradlew :neoforge:build
```

## The Verdict

**PARTIAL BREAKTHROUGH!** This represents significant progress on what appeared to be an impossible mod compatibility issue. Through systematic debugging and the "nuclear option" approach, we achieved *basic player movement* in a complex 100+ mod environment. However, **actual pathfinding is still broken** - the movement doesn't follow calculated paths.

**Status**: 🟡 **BASIC MOVEMENT + DUAL BLOCKERS (NETWORK + PATHFINDING)**  
**Method**: Nuclear input forcing (bypasses proper path following)  
**Real Challenge**: PathExecutor system completely non-functional  
**Last Updated**: December 19, 2024

---

*"When conventional approaches fail, sometimes you need to go nuclear!"* 🚀

## Today's Session: Network Registration Roadblock 🚧

### The Issue
After achieving movement, we hit a **new major roadblock**: Network registration size mismatch causing connection failures.

```
Connection Lost
Internal Exception: java.lang.IllegalStateException: End size 612 is less than fixed size 613
```

### What We Discovered Today

**✅ Good News:**
- Successfully removed all debug logging from the code
- Built a cleaner version: `baritone-standalone-noisium-neoforge-1.11.2-11-g9db25b01-dirty.jar`
- All mods restored to the modpack (no more disabling mods approach)
- Performance mods confirmed not to be the issue:
  - `farsight-1.21-3.8.jar` ✓
  - `modernfix-neoforge-5.23.1+mc1.21-1.21.1.jar` ✓ 
  - `noisium-neoforge-2.3.0+mc1.21-1.21.1.jar` ✓

**❌ Current Blocker:**
- **Network registration limit exceeded**: The modpack + Baritone combination creates too many network channels (612+ when limit is 613)
- This prevents connection to the server entirely
- Cannot test movement improvements until this is resolved
- Issue persists even with cleaner build (rules out debug logging as cause)

### Theory: Dual Problem Investigation Needed

**Network Registration Issue:**
The error suggests our Baritone mod might be:
1. Registering duplicate network channels 
2. Creating too many network handlers
3. Interfering with NeoForge's network registration process
4. Conflicting with other mods' network registration

**Core Path Execution Issue (Discovered in debug logs):**
Even if network was fixed, movement still wouldn't work because:
1. **PathExecutor creation fails** - Calculated paths never become PathExecutor objects
2. **PathingBehavior.current always null** - No active path executor exists
3. **No movement input setting** - PathExecutor.onTick() never called to set movement inputs
4. **Nuclear option bypasses proper system** - Forces movement but ignores calculated paths

### Next Steps for Resolution

**Priority 1: Network Registration (Immediate blocker)**
1. **Investigate network registration in Baritone code**
   - Check for duplicate registrations
   - Review network channel creation
   - Compare with working versions
2. **Network channel optimization**
   - Reduce Baritone's network footprint
   - Consolidate network handlers where possible
3. **NeoForge compatibility review**
   - Ensure proper NeoForge network registration patterns

**Priority 2: Path Execution System (Core functionality)**
1. **Debug PathExecutor creation process**
   - Why calculated paths don't become PathExecutor objects
   - Investigate PathingBehavior.current assignment logic
2. **Fix path execution pipeline**
   - Ensure PathExecutor.onTick() gets called
   - Fix movement input system to follow calculated paths instead of camera direction
3. **Replace nuclear option with proper solution**
   - Make movement follow actual calculated paths
   - Integrate with proper Baritone movement system

### Files Affected This Session
- **Cleaned up**: `src/main/java/baritone/behavior/PathingBehavior.java` (removed debug logging)
- **Cleaned up**: `src/main/java/baritone/pathing/path/PathExecutor.java` (removed debug logging)
- **Status**: All mods re-enabled in modpack (proper compatibility approach)

### The Bigger Picture
This represents a shift from input system issues to **networking/modpack compatibility** issues. While we solved the core movement problem, we now face infrastructure challenges that prevent testing and deployment.

**Priority**: Network registration size limit resolution (blocks all testing)
**Secondary**: Fix PathExecutor creation system (actual pathfinding still broken)
**Goal**: Connect successfully + proper path following movement (not just camera-direction movement)

### The Legacy
This debugging session demonstrates that seemingly impossible mod compatibility issues can be solved with:
- Systematic debugging approaches
- Creative problem-solving 
- Willingness to bypass broken systems entirely
- **Never giving up!**

# Baritone
<p align="center">
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/downloads/cabaletta/baritone/total.svg" alt="GitHub All Releases"/></a>
</p>

<p align="center">
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.12.2-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.13.2-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.14.4-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.15.2-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.16.5-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.17.1-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.18.2-yellow.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.19.2-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.19.4-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.20.1-brightgreen.svg" alt="Minecraft"/></a>
  <a href="#Baritone"><img src="https://img.shields.io/badge/MC-1.21.3-brightgreen.svg" alt="Minecraft"/></a>
</p>

<p align="center">
  <a href="https://travis-ci.com/cabaletta/baritone/"><img src="https://travis-ci.com/cabaletta/baritone.svg?branch=master" alt="Build Status"/></a>
  <a href="https://github.com/cabaletta/baritone/releases/"><img src="https://img.shields.io/github/release/cabaletta/baritone.svg" alt="Release"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-LGPL--3.0%20with%20anime%20exception-green.svg" alt="License"/></a>
  <a href="https://www.codacy.com/gh/cabaletta/baritone/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade"><img src="https://app.codacy.com/project/badge/Grade/cadab857dab049438b6e28b3cfc5570e" alt="Codacy Badge"/></a>
  <a href="https://github.com/cabaletta/baritone/blob/master/CODE_OF_CONDUCT.md"><img src="https://img.shields.io/badge/%E2%9D%A4-code%20of%20conduct-blue.svg?style=flat" alt="Code of Conduct"/></a>
  <a href="https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle"><img src="https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle" alt="Known Vulnerabilities"/></a>
  <a href="https://github.com/cabaletta/baritone/issues/"><img src="https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat" alt="Contributions welcome"/></a>
  <a href="https://github.com/cabaletta/baritone/issues/"><img src="https://img.shields.io/github/issues/cabaletta/baritone.svg" alt="Issues"/></a>
  <a href="https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed"><img src="https://img.shields.io/github/issues-closed/cabaletta/baritone.svg" alt="GitHub issues-closed"/></a>
  <a href="https://github.com/cabaletta/baritone/pulls/"><img src="https://img.shields.io/github/issues-pr/cabaletta/baritone.svg" alt="Pull Requests"/></a>
  <a href="https://github.com/cabaletta/baritone/graphs/contributors/"><img src="https://img.shields.io/github/contributors/cabaletta/baritone.svg" alt="GitHub contributors"/></a>
  <a href="https://github.com/cabaletta/baritone/commit/"><img src="https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg" alt="GitHub commits"/></a>
  <img src="https://img.shields.io/github/languages/code-size/cabaletta/baritone.svg" alt="Code size"/>
  <img src="https://img.shields.io/github/repo-size/cabaletta/baritone.svg" alt="GitHub repo size"/>
  <img src="https://tokei.rs/b1/github/cabaletta/baritone?category=code&style=flat" alt="Lines of Code"/>
  <img src="https://img.shields.io/badge/Badges-36-blue.svg" alt="yes"/>
</p>

<p align="center">
  <a href="https://impactclient.net/"><img src="https://img.shields.io/badge/Impact%20integration-v1.2.14%20/%20v1.3.8%20/%20v1.4.6%20/%20v1.5.3%20/%20v1.6.3-brightgreen.svg" alt="Impact integration"/></a>
  <a href="https://github.com/lambda-client/lambda"><img src="https://img.shields.io/badge/Lambda%20integration-v1.2.17-brightgreen.svg" alt="Lambda integration"/></a>
  <a href="https://github.com/fr1kin/ForgeHax/"><img src="https://img.shields.io/badge/ForgeHax%20%22integration%22-scuffed-yellow.svg" alt="ForgeHax integration"/></a>
  <a href="https://aristois.net/"><img src="https://img.shields.io/badge/Aristois%20add--on%20integration-v1.6.3-green.svg" alt="Aristois add-on integration"/></a>
  <a href="https://rootnet.dev/"><img src="https://img.shields.io/badge/rootNET%20integration-v1.2.14-green.svg" alt="rootNET integration"/></a>
  <a href="https://futureclient.net/"><img src="https://img.shields.io/badge/Future%20integration-v1.2.12%20%2F%20v1.3.6%20%2F%20v1.4.4-red" alt="Future integration"/></a>
  <a href="https://rusherhack.org/"><img src="https://img.shields.io/badge/RusherHack%20integration-v1.2.14-green" alt="RusherHack integration"/></a>
</p>

<p align="center">
  <a href="http://forthebadge.com/"><img src="https://web.archive.org/web/20230604002050/https://forthebadge.com/images/badges/built-with-swag.svg" alt="forthebadge"/></a>
  <a href="http://forthebadge.com/"><img src="https://web.archive.org/web/20230604002050/https://forthebadge.com/images/badges/mom-made-pizza-rolls.svg" alt="forthebadge"/></a>
</p>

A Minecraft pathfinder bot.

Baritone is the pathfinding system used in [Impact](https://impactclient.net/) since 4.4. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a (very old!) video I made showing off what it can do.

[**Baritone Discord Server**](http://discord.gg/s6fRBAUpmr)

**Quick download links:**

| Forge                                                                                                         | Fabric                                                                                                        |
|---------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| [1.12.2 Forge](https://github.com/cabaletta/baritone/releases/download/v1.2.19/baritone-api-forge-1.2.19.jar) |                                                                                                               |
| [1.16.5 Forge](https://github.com/cabaletta/baritone/releases/download/v1.6.5/baritone-api-forge-1.6.5.jar)   | [1.16.5 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.6.5/baritone-api-fabric-1.6.5.jar) |
| [1.17.1 Forge](https://github.com/cabaletta/baritone/releases/download/v1.7.3/baritone-api-forge-1.7.3.jar)   | [1.17.1 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.7.3/baritone-api-fabric-1.7.3.jar) |
| [1.18.2 Forge](https://github.com/cabaletta/baritone/releases/download/v1.8.6/baritone-api-forge-1.8.6.jar)   | [1.18.2 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.8.6/baritone-api-fabric-1.8.6.jar) |
| [1.19.2 Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.4/baritone-api-forge-1.9.4.jar)   | [1.19.2 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.4/baritone-api-fabric-1.9.4.jar) |
| [1.19.3 Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.1/baritone-api-forge-1.9.1.jar)   | [1.19.3 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.1/baritone-api-fabric-1.9.1.jar) |
| [1.19.4 Forge](https://github.com/cabaletta/baritone/releases/download/v1.9.3/baritone-api-forge-1.9.3.jar)   | [1.19.4 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.9.3/baritone-api-fabric-1.9.3.jar) |
| [1.20.1 Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.1/baritone-api-forge-1.10.1.jar)   | [1.20.1 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.1/baritone-api-fabric-1.10.1.jar) |
| [1.20.3 Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.2/baritone-api-forge-1.10.2.jar)   | [1.20.3 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.2/baritone-api-fabric-1.10.2.jar) |
| [1.20.4 Forge](https://github.com/cabaletta/baritone/releases/download/v1.10.2/baritone-api-forge-1.10.2.jar)   | [1.20.4 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.10.2/baritone-api-fabric-1.10.2.jar) |
| [1.21.3 Forge](https://github.com/cabaletta/baritone/releases/download/v1.11.1/baritone-api-forge-1.11.1.jar)   | [1.21.3 Fabric](https://github.com/cabaletta/baritone/releases/download/v1.11.1/baritone-api-fabric-1.11.1.jar) |

**Message for 2b2t players looking for 1.19/1.20 Baritone** If you like, please try the beta for Baritone Elytra for 2b2t, find it in #announcements of [the Baritone discord](http://discord.gg/s6fRBAUpmr). It supports 1.19.4 and 1.20.1, Forge or Fabric. If you have to see it to believe it, watch [this YouTube video](https://youtu.be/NnSlQi-68eQ).

**How to immediately get started:** Type `#goto 1000 500` in chat to go to x=1000 z=500. Type `#mine diamond_ore` to mine diamond ore. Type `#stop` to stop. For more, read [the usage page](USAGE.md) and/or watch this [tutorial playlist](https://www.youtube.com/playlist?list=PLnwnJ1qsS7CoQl9Si-RTluuzCo_4Oulpa). Also try `#elytra` for Elytra flying in the Nether using fireworks.

For other versions of Minecraft or more complicated situations or for development, see [Installation & setup](SETUP.md). Also consider just installing [Impact](https://impactclient.net/), which comes with Baritone and is easier to install than wrangling with version JSONs and zips. For 1.16.5, [click here](https://www.youtube.com/watch?v=_4eVJ9Qz2J8) and see description. Once Baritone is installed, look [here](USAGE.md) for instructions on how to use it. There's a [showcase video](https://youtu.be/CZkLXWo4Fg4) made by @Adovin#6313 on Baritone which I recommend.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8.9, rebuilt for 1.12.2 onwards. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

Have committed at least once a day from Aug 1, 2018, to Aug 1, 2019.

1Leijurv3DWTrGAfmmiTphjhXLvQiHg7K2

# Getting Started

Here are some links to help to get started:

- [Features](FEATURES.md)

- [Installation & setup](SETUP.md)

- [API Javadocs](https://baritone.leijurv.com/)

- [Settings](https://baritone.leijurv.com/baritone/api/Settings.html#field.detail)

- [Usage (chat control)](USAGE.md)

## Stars over time

[![Stargazers over time](https://starchart.cc/cabaletta/baritone.svg)](https://starchart.cc/cabaletta/baritone)

# API

The API is heavily documented, you can find the Javadocs for the latest release [here](https://baritone.leijurv.com/).
Please note that usage of anything located outside of the ``baritone.api`` package is not supported by the API release
jar.

Below is an example of basic usage for changing some settings, and then pathing to an X/Z goal.

```java
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;

BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```

# FAQ

## Can I use Baritone as a library in my custom utility client?

That's what it's for, sure! (As long as usage complies with the LGPL 3.0 License)

## How is it so fast?

Magic. (Hours of [leijurv](https://github.com/leijurv/) enduring excruciating pain)

### Additional Special Thanks To:

![YourKit-Logo](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications.

YourKit is the creator of the [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

We thank them for granting Baritone an OSS license so that we can make our software the best it can be.

## Why is it called Baritone?

It's named for FitMC's deep sultry voice.
