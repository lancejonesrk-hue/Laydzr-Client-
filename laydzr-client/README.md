# Laydzr Client (Fabric, Minecraft 1.21.11)

Blue-themed client mod: draggable HUD mods, Modrinth browser, Spotify music bar.

Versions: Minecraft 1.21.11, Yarn 1.21.11+build.4, Fabric Loader 0.18.4,
Fabric API 0.141.0+1.21.11, Loom 1.14-SNAPSHOT, Gradle 9.2.1, Java 21.

## Build with no local setup (GitHub Actions)
1. Create a new GitHub repo and upload everything in this folder (including the hidden .github folder).
2. Open the Actions tab and wait for "Build Laydzr Client" to finish (a few minutes).
3. Open the finished run and download the "laydzr-client-jar" artifact.
4. Put the jar in .minecraft/mods together with Fabric API 0.141.0+1.21.11 (Fabric Loader 0.18.4+, Java 21).

## Build locally
Install JDK 21 and Gradle 9.2.1, then run `gradle build`. The jar is in build/libs/.

## Use
- Right Shift: open the Laydzr menu (Mods / Music / Modrinth, Edit HUD button)
- Music tab: create an app at developer.spotify.com/dashboard, add redirect URI
  http://127.0.0.1:8888/callback, paste the Client ID, press Connect.
- Keys: [ previous, \ play/pause, ] next (playback control needs Spotify Premium)
- Config is saved in .minecraft/config/laydzrclient.json
