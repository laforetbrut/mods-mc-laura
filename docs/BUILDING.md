# Building from source

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Repository / Dépôt : <https://github.com/laforetbrut/lauramod>

---

## English

### Requirements

| Tool | Notes |
|---|---|
| Git | To clone the repository. |
| JDK 17, 21, 25 | The JDK of the targets you build: Java 17 for Minecraft 1.20.1, 21 for 1.21.1, 25 for 26.1.2. The projects use a Gradle toolchain, with the foojay resolver plugin to locate one. |
| Gradle | Not needed. Each project ships its own wrapper (`gradlew`, `gradlew.bat`). |
| Python 3, Bash | Only for the scripts of `tools/`. |

The continuous integration starts Gradle with the JDK of the target, except for `fabric-1.20.1`: Fabric Loom needs Java 21 to run, so that project sets its Gradle daemon to Java 21 in `gradle/gradle-daemon-jvm.properties` while the game is still compiled for Java 17. `fabric-26.1.2` sets its daemon to Java 25 the same way.

### Repository layout

```
common/<mcversion>/src/main/     code and resources shared by every loader (1.20.1, 1.21.1, 26.1.2)
neoforge-1.20.1/                 NeoForge project for Minecraft 1.20.1
forge-1.20.1/                    Forge project for Minecraft 1.20.1
fabric-1.20.1/                   Fabric project for Minecraft 1.20.1
neoforge-1.21.1/                 NeoForge project for Minecraft 1.21.1
forge-1.21.1/                    Forge project for Minecraft 1.21.1
fabric-1.21.1/                   Fabric project for Minecraft 1.21.1
neoforge-26.1.2/                 NeoForge project for Minecraft 26.1.2
forge-26.1.2/                    Forge project for Minecraft 26.1.2
fabric-26.1.2/                   Fabric project for Minecraft 26.1.2
tools/                           asset generator, language checker, branch generator, jar collector
docs/                            this documentation
```

- Each `<loader>-<mcversion>/` folder is a **standalone Gradle project**. There is no root build.
- Its `build.gradle` adds `../common/<mcversion>/src/main/java` and `../common/<mcversion>/src/main/resources` to the main source set. The loader folder itself only holds the glue: entry points, networking, the `Platform` implementation and the optional integrations (`compat` package).
- One branch per target, named like its folder (`fabric-26.1.2` for example), is generated from `main` by `tools/make_branches.sh`; the branches are created when `main` is published. On such a branch the project sits at the repository root, the common code is merged into `src/main`, and the include does nothing.
- NeoForge for Minecraft 1.20.1 is the 47.1 fork of Forge. Its project uses the legacy plugin of ModDevGradle, and the mod depends on the mod id `forge` there.
- Version 2.0.0 builds for the nine targets, and the nine pass the in-game self test suite.

### Build a jar

From the folder of the target you want:

```
cd neoforge-1.21.1
./gradlew build
```

On Windows use `gradlew.bat build`. The jar is written to `build/libs/`:

| Project | Jar |
|---|---|
| `neoforge-1.20.1` | `lauramod-neoforge-1.20.1-2.0.0.jar` |
| `forge-1.20.1` | `lauramod-forge-1.20.1-2.0.0.jar` |
| `fabric-1.20.1` | `lauramod-fabric-1.20.1-2.0.0.jar` |
| `neoforge-1.21.1` | `lauramod-neoforge-1.21.1-2.0.0.jar` |
| `forge-1.21.1` | `lauramod-forge-1.21.1-2.0.0.jar` |
| `fabric-1.21.1` | `lauramod-fabric-1.21.1-2.0.0.jar` |
| `neoforge-26.1.2` | `lauramod-neoforge-26.1.2-2.0.0.jar` |
| `forge-26.1.2` | `lauramod-forge-26.1.2-2.0.0.jar` |
| `fabric-26.1.2` | `lauramod-fabric-26.1.2-2.0.0.jar` |

- Install the jar of `build/libs/`. Some projects also write a development jar to `build/devlibs/` (NeoForge 1.20.1, Fabric 1.20.1 and 1.21.1): it only works in the development runs.
- Outside of development, NeoForge and Forge 1.20.1 run on SRG names and Fabric 1.20.1 and 1.21.1 on intermediary names: the jars of these projects are remapped after the build (`reobfJar` on NeoForge and Forge, `remapJar` on Fabric). Minecraft 26.1 is not obfuscated and its jars are not remapped.
- Mod metadata (`neoforge.mods.toml`, `mods.toml`, `fabric.mod.json`) is filled in from the `gradle.properties` of the project: `mod_id`, `mod_name`, `mod_version`, `mod_authors`, `mod_license`, `mod_description`. The license file of the repository root is copied into the jar as `LICENSE_lauramod`.

### Toolchain of each project

Minecraft 1.20.1:

| | NeoForge 1.20.1 | Forge 1.20.1 | Fabric 1.20.1 |
|---|---|---|---|
| Gradle wrapper | 9.2.1 | 8.12.1 | 9.5.1 |
| Build plugin | `net.neoforged.moddev.legacyforge` 2.0.147 | `net.minecraftforge.gradle` 6.0.54, `org.parchmentmc.librarian.forgegradle` 1.2.0 | `fabric-loom` 1.17.21 |
| Loader | NeoForge 47.1.106 | Forge 47.4.10 | Fabric Loader 0.19.5, Fabric API 0.92.12+1.20.1 |
| Mappings | Parchment 2023.09.03 | Parchment 2023.09.03 | official Mojang mappings |
| Java of the game | 17 | 17 | 17 |
| Java running Gradle in CI | 17 | 17 | 21 |

Minecraft 1.21.1:

| | NeoForge 1.21.1 | Forge 1.21.1 | Fabric 1.21.1 |
|---|---|---|---|
| Gradle wrapper | 9.2.1 | 9.3.1 | 9.5.1 |
| Build plugin | `net.neoforged.moddev` 2.0.147 | `net.minecraftforge.gradle` 7.0.40 | `fabric-loom` 1.17.21 |
| Loader | NeoForge 21.1.252 | Forge 52.1.16 | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 |
| Mappings | Parchment 2024.11.17 | official | official Mojang mappings |
| Java of the game | 21 | 21 | 21 |
| Java running Gradle in CI | 21 | 21 | 21 |

Minecraft 26.1.2:

| | NeoForge 26.1.2 | Forge 26.1.2 | Fabric 26.1.2 |
|---|---|---|---|
| Gradle wrapper | 9.2.1 | 9.5.0 | 9.7.1 |
| Build plugin | `net.neoforged.moddev` 2.0.147 | `net.minecraftforge.gradle` 7.0.40 | `net.fabricmc.fabric-loom` 1.18.2 |
| Loader | NeoForge 26.1.2.109 | Forge 64.1.3 | Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2 |
| Mappings | none, the game ships without obfuscation | none | none |
| Java of the game | 25 | 25 | 25 |
| Java running Gradle in CI | 25 | 25 | 25 |

### Development runs

Each project declares these runs:

| Run | Gradle task | Game folder | Purpose |
|---|---|---|---|
| `client` | `runClient` | `run/` | A normal client. |
| `server` | `runServer` | `run/` (`run-server/` on Fabric) | A dedicated server without GUI. |
| `clienttest` | `runClienttest` | `run-clienttest/` | A client started with `-Dlauramod.clienttest=true` on the quick play world `laura_test`. It summons a companion, opens every screen of the mod, saves a screenshot of each in `screenshots/`, then quits. |
| `selftest` | `runSelftest` | `run-selftest/` | A dedicated server started with `-Dlauramod.selftest=true`. It runs the in-game test suite, writes `lauramod-selftest.json` in the game folder and stops. See below. |

- Every project declares `client`, `server` and `selftest`. Every project except `fabric-1.21.1` also declares `clienttest`.
- The `clienttest` run needs a singleplayer world named `laura_test` in `run-clienttest/saves`.
- The system property `lauramod.selftest.only` keeps only the tests whose name contains its value. It is set with `-PselftestOnly=<text>` in `neoforge-1.20.1`, `forge-1.20.1`, `fabric-1.20.1`, `forge-1.21.1` and `forge-26.1.2`. The other four projects do not pass it.
- The run folders are ignored by Git.

### Running the self tests

From the folder of a target:

```
cd forge-1.20.1
./gradlew runSelftest -PacceptEula
```

1. `runSelftest` first runs `prepareSelftest`, which prepares `run-selftest/`:
   - `-PacceptEula` writes `eula.txt` with `eula=true`. Passing it means you accept the Minecraft EULA (<https://aka.ms/MinecraftEULA>). Without it the task stops with a message, unless `run-selftest/eula.txt` already contains `eula=true`.
   - When `server.properties` is missing, it writes one: offline mode, flat world, no spawn protection, no tick watchdog, and the port `selftest_port` of the project's `gradle.properties`.
2. The dedicated server starts and runs the suite of the common code, plus one test for Farmer's Delight and one for KubeJS when they are loaded.
3. A passing run prints `[SELFTEST] <n> passed, 0 failed` and then `[SELFTEST] RESULT SUCCESS` in the console and in `run-selftest/logs/latest.log`, writes `run-selftest/lauramod-selftest.json`, and the server stops by itself.
4. The server process ends with exit code 0 when every test passed, 1 when a test failed, 2 when the server stopped before the tests ran. A run without the `RESULT SUCCESS` line is not a pass.

The world is kept between runs. To start from a fresh world, delete `run-selftest/world` before the run. With `-PwithCompatMods`, the 1.20.1 projects use `run-selftest/world-compat` instead, because a world remembers the registries of the mods it was created with.

Each target has its own server port, so several self tests can run at the same time:

| Project | `selftest_port` |
|---|---|
| `neoforge-1.20.1` | 25623 |
| `forge-1.20.1` | 25621 |
| `fabric-1.20.1` | 25622 |
| `neoforge-1.21.1` | 25611 |
| `forge-1.21.1` | 25612 |
| `fabric-1.21.1` | 25613 |
| `neoforge-26.1.2` | 25631 |
| `forge-26.1.2` | 25632 |
| `fabric-26.1.2` | 25633 |

`server.properties` is only written when it is missing: delete it after changing `selftest_port`.

### Testing the optional integrations

The integrations are compiled against the public APIs of the other mods, which are never bundled in the jar.

| Project | Compile-only dependencies | Loaded in dev runs with `-PwithCompatMods` |
|---|---|---|
| `neoforge-1.20.1` | Applied Energistics 2 API 15.4.10, Curios API 5.14.1+1.20.1, Farmer's Delight 1.20.1-1.3.4, KubeJS 2001.6.5-build.26, Rhino 2001.2.2-build.17 | Farmer's Delight and KubeJS (with Rhino and Architectury 9.1.12) |
| `forge-1.20.1` | Applied Energistics 2 API 15.4.10, Curios API 5.14.1+1.20.1, Farmer's Delight 1.20.1-1.3.4, KubeJS 2001.6.5-build.26, Rhino 2001.2.2-build.17 | Farmer's Delight and KubeJS (with Rhino and Architectury 9.1.12) |
| `fabric-1.20.1` | Applied Energistics 2 API 15.4.10, Farmer's Delight Refabricated 1.20.1-2.5.7, KubeJS 2001.6.5-build.26, Rhino 2001.2.2-build.17 | Farmer's Delight Refabricated (with the jars nested in it) and KubeJS (with Rhino and Architectury 9.1.12) |
| `neoforge-1.21.1` | Applied Energistics 2 API 19.0.27, Curios API 9.5.1+1.21.1, Farmer's Delight 1.21.1-1.3.4, KubeJS 2101.7.2-build.377 | Farmer's Delight and KubeJS |
| `forge-1.21.1` | none | not available |
| `fabric-1.21.1` | Farmer's Delight Refabricated 1.21.1-3.3.6 | Farmer's Delight Refabricated |
| `neoforge-26.1.2` | none | not available |
| `forge-26.1.2` | none | not available |
| `fabric-26.1.2` | Farmer's Delight Refabricated 26.1-3.6.26 | Farmer's Delight Refabricated |

- Applied Energistics 2 and Curios are never loaded in the dev runs, so their bridges have no in-game test. On 1.20.1 they have never been run in game.
- With `-PwithCompatMods`, the projects that load KubeJS copy the test script `src/selftest/kubejs/server_scripts/laura_selftest.js` into `run-selftest/` before the self test.

Example:

```
cd neoforge-1.21.1
./gradlew runSelftest -PacceptEula -PwithCompatMods
```

### Tools

| Script | Usage | Role |
|---|---|---|
| `tools/generate_assets.py` | `python tools/generate_assets.py [resources_dir] [--mc <mcversion>] [--no-sounds] [--preview <folder>]` | Generates the skins, GUI icons, item and block textures, block models, the logo and the sound files into the resources folder of a Minecraft version (default `common/1.21.1/src/main/resources`). Requires Pillow, numpy and ffmpeg with libvorbis. |
| `tools/check_lang.py` | `python tools/check_lang.py [--mc <mcversion>] [--strict]` | Checks that every translation key and dialogue key used by the code exists. For each language and dialogue file it lists the missing keys (English is the reference), the unused keys and the placeholders that differ from English. Exit code 1 when English is incomplete, or when any file is incomplete with `--strict`. Default version: 1.21.1. |
| `tools/make_branches.sh` | `bash tools/make_branches.sh` | Regenerates the per-target branches from `main`. Run it from the repository root with a clean working tree (no uncommitted or untracked file). Each branch gets its project at the root, the common code of its Minecraft version merged into `src/main`, and the shared files (README, changelog, license, community files, `.github`, `docs`). An existing branch gets a new commit on top of its history, an unchanged one is left alone. Only local branches are updated: push them yourself. |
| `tools/collect_jars.sh` | `bash tools/collect_jars.sh [--build]` | Copies the mod jar of every target from its `build/libs/` into `jars/` at the repository root, a local folder ignored by Git. Sources, dev and javadoc jars are skipped. With `--build` it first runs `./gradlew build` in each target folder. A target without a jar is reported. |

- Run `check_lang.py` after adding a translatable text or a dialogue key, once per Minecraft version (`--mc 1.20.1`, `--mc 1.21.1`, `--mc 26.1.2`). The continuous integration runs it for each version.
- `generate_assets.py --mc 26.1.2` also rewrites `models/item/laura_spawn_egg.json` with the spawn egg template of older versions, which Minecraft 26.1 no longer has. Restore the committed file afterwards (`git checkout -- common/26.1.2/src/main/resources/assets/lauramod/models/item/laura_spawn_egg.json`).

### Code map

Package root: `com.vyrriox.lauramod` in `common/<mcversion>/src/main/java`. The three Minecraft versions have the same packages.

| Package | Content |
|---|---|
| `api` | Public API for other mods and scripts. |
| `block`, `item`, `registry` | The gravestone, Laura's Heart, registered objects. |
| `client` | Menu, emote wheel, needs overlay, renderer, animations, client skins and models. |
| `command` | The `/laura` command tree. |
| `config` | The two settings files. |
| `cooking`, `gift`, `desire` | Cookbook, gifts and desires tables. |
| `dialogue` | Dialogue files, chat matching, languages. |
| `entity` | The companion, her goals (`ai`), her personality (`brain`) and her jobs (`work`). |
| `model` | Blockbench model parsing, animation sampling, Molang. |
| `network` | The single mod channel. |
| `platform` | What the common code needs from a loader. |
| `skin` | Skin references, server asset store, uploads. |
| `test` | The in-game self tests. |
| `world` | Orders, chat, summoning, graves, advancements, world data. |

Common code only calls vanilla methods. Anything specific to a loader goes through the `Platform` interface.

### License

MIT. See the `LICENSE` file at the root of the repository.

---

## Français

### Prérequis

| Outil | Remarques |
|---|---|
| Git | Pour cloner le dépôt. |
| JDK 17, 21, 25 | Le JDK des cibles que vous compilez : Java 17 pour Minecraft 1.20.1, 21 pour la 1.21.1, 25 pour la 26.1.2. Les projets utilisent une toolchain Gradle, avec le plugin foojay resolver pour en trouver une. |
| Gradle | Inutile. Chaque projet fournit son propre wrapper (`gradlew`, `gradlew.bat`). |
| Python 3, Bash | Uniquement pour les scripts de `tools/`. |

L'intégration continue lance Gradle avec le JDK de la cible, sauf pour `fabric-1.20.1` : Fabric Loom a besoin de Java 21 pour tourner, ce projet règle donc son daemon Gradle sur Java 21 dans `gradle/gradle-daemon-jvm.properties`, tandis que le jeu reste compilé pour Java 17. `fabric-26.1.2` règle son daemon sur Java 25 de la même façon.

### Organisation du dépôt

```
common/<mcversion>/src/main/     code et ressources partagés par tous les chargeurs (1.20.1, 1.21.1, 26.1.2)
neoforge-1.20.1/                 projet NeoForge pour Minecraft 1.20.1
forge-1.20.1/                    projet Forge pour Minecraft 1.20.1
fabric-1.20.1/                   projet Fabric pour Minecraft 1.20.1
neoforge-1.21.1/                 projet NeoForge pour Minecraft 1.21.1
forge-1.21.1/                    projet Forge pour Minecraft 1.21.1
fabric-1.21.1/                   projet Fabric pour Minecraft 1.21.1
neoforge-26.1.2/                 projet NeoForge pour Minecraft 26.1.2
forge-26.1.2/                    projet Forge pour Minecraft 26.1.2
fabric-26.1.2/                   projet Fabric pour Minecraft 26.1.2
tools/                           générateur de ressources, vérificateur de langues, générateur de branches, collecte des jars
docs/                            cette documentation
```

- Chaque dossier `<chargeur>-<mcversion>/` est un **projet Gradle autonome**. Il n'y a pas de build racine.
- Son `build.gradle` ajoute `../common/<mcversion>/src/main/java` et `../common/<mcversion>/src/main/resources` au source set principal. Le dossier du chargeur ne contient que la colle : points d'entrée, réseau, implémentation de `Platform` et intégrations facultatives (paquet `compat`).
- Une branche par cible, nommée comme son dossier (`fabric-26.1.2` par exemple), est générée depuis `main` par `tools/make_branches.sh` ; les branches sont créées quand `main` est publié. Sur une telle branche, le projet est à la racine du dépôt, le code commun est fusionné dans `src/main`, et l'inclusion ne fait rien.
- NeoForge pour Minecraft 1.20.1 est le fork 47.1 de Forge. Son projet utilise le plugin legacy de ModDevGradle, et le mod y dépend de l'identifiant de mod `forge`.
- La version 2.0.0 se compile pour les neuf cibles, et les neuf réussissent la suite de self tests en jeu.

### Construire un jar

Depuis le dossier de la cible voulue :

```
cd neoforge-1.21.1
./gradlew build
```

Sous Windows, utilisez `gradlew.bat build`. Le jar est écrit dans `build/libs/` :

| Projet | Jar |
|---|---|
| `neoforge-1.20.1` | `lauramod-neoforge-1.20.1-2.0.0.jar` |
| `forge-1.20.1` | `lauramod-forge-1.20.1-2.0.0.jar` |
| `fabric-1.20.1` | `lauramod-fabric-1.20.1-2.0.0.jar` |
| `neoforge-1.21.1` | `lauramod-neoforge-1.21.1-2.0.0.jar` |
| `forge-1.21.1` | `lauramod-forge-1.21.1-2.0.0.jar` |
| `fabric-1.21.1` | `lauramod-fabric-1.21.1-2.0.0.jar` |
| `neoforge-26.1.2` | `lauramod-neoforge-26.1.2-2.0.0.jar` |
| `forge-26.1.2` | `lauramod-forge-26.1.2-2.0.0.jar` |
| `fabric-26.1.2` | `lauramod-fabric-26.1.2-2.0.0.jar` |

- Installez le jar de `build/libs/`. Certains projets écrivent aussi un jar de développement dans `build/devlibs/` (NeoForge 1.20.1, Fabric 1.20.1 et 1.21.1) : il ne fonctionne que dans les lancements de développement.
- Hors développement, NeoForge et Forge 1.20.1 tournent avec les noms SRG, et Fabric 1.20.1 et 1.21.1 avec les noms intermediary : les jars de ces projets sont remappés après le build (`reobfJar` sur NeoForge et Forge, `remapJar` sur Fabric). Minecraft 26.1 n'est pas obfusqué et ses jars ne sont pas remappés.
- Les métadonnées du mod (`neoforge.mods.toml`, `mods.toml`, `fabric.mod.json`) sont remplies à partir du `gradle.properties` du projet : `mod_id`, `mod_name`, `mod_version`, `mod_authors`, `mod_license`, `mod_description`. Le fichier de licence de la racine du dépôt est copié dans le jar sous le nom `LICENSE_lauramod`.

### Outils de build de chaque projet

Minecraft 1.20.1 :

| | NeoForge 1.20.1 | Forge 1.20.1 | Fabric 1.20.1 |
|---|---|---|---|
| Wrapper Gradle | 9.2.1 | 8.12.1 | 9.5.1 |
| Plugin de build | `net.neoforged.moddev.legacyforge` 2.0.147 | `net.minecraftforge.gradle` 6.0.54, `org.parchmentmc.librarian.forgegradle` 1.2.0 | `fabric-loom` 1.17.21 |
| Chargeur | NeoForge 47.1.106 | Forge 47.4.10 | Fabric Loader 0.19.5, Fabric API 0.92.12+1.20.1 |
| Mappings | Parchment 2023.09.03 | Parchment 2023.09.03 | mappings officiels de Mojang |
| Java du jeu | 17 | 17 | 17 |
| Java qui lance Gradle en CI | 17 | 17 | 21 |

Minecraft 1.21.1 :

| | NeoForge 1.21.1 | Forge 1.21.1 | Fabric 1.21.1 |
|---|---|---|---|
| Wrapper Gradle | 9.2.1 | 9.3.1 | 9.5.1 |
| Plugin de build | `net.neoforged.moddev` 2.0.147 | `net.minecraftforge.gradle` 7.0.40 | `fabric-loom` 1.17.21 |
| Chargeur | NeoForge 21.1.252 | Forge 52.1.16 | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 |
| Mappings | Parchment 2024.11.17 | officiels | mappings officiels de Mojang |
| Java du jeu | 21 | 21 | 21 |
| Java qui lance Gradle en CI | 21 | 21 | 21 |

Minecraft 26.1.2 :

| | NeoForge 26.1.2 | Forge 26.1.2 | Fabric 26.1.2 |
|---|---|---|---|
| Wrapper Gradle | 9.2.1 | 9.5.0 | 9.7.1 |
| Plugin de build | `net.neoforged.moddev` 2.0.147 | `net.minecraftforge.gradle` 7.0.40 | `net.fabricmc.fabric-loom` 1.18.2 |
| Chargeur | NeoForge 26.1.2.109 | Forge 64.1.3 | Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2 |
| Mappings | aucun, le jeu est livré sans obfuscation | aucun | aucun |
| Java du jeu | 25 | 25 | 25 |
| Java qui lance Gradle en CI | 25 | 25 | 25 |

### Lancements de développement

Chaque projet déclare ces lancements :

| Lancement | Tâche Gradle | Dossier de jeu | Rôle |
|---|---|---|---|
| `client` | `runClient` | `run/` | Un client normal. |
| `server` | `runServer` | `run/` (`run-server/` sur Fabric) | Un serveur dédié sans interface. |
| `clienttest` | `runClienttest` | `run-clienttest/` | Un client lancé avec `-Dlauramod.clienttest=true` sur le monde en jeu rapide `laura_test`. Il invoque une compagne, ouvre chaque écran du mod, enregistre une capture de chacun dans `screenshots/`, puis quitte. |
| `selftest` | `runSelftest` | `run-selftest/` | Un serveur dédié lancé avec `-Dlauramod.selftest=true`. Il exécute la suite de tests en jeu, écrit `lauramod-selftest.json` dans le dossier de jeu et s'arrête. Voir plus bas. |

- Chaque projet déclare `client`, `server` et `selftest`. Tous les projets sauf `fabric-1.21.1` déclarent aussi `clienttest`.
- Le lancement `clienttest` a besoin d'un monde solo nommé `laura_test` dans `run-clienttest/saves`.
- La propriété système `lauramod.selftest.only` ne garde que les tests dont le nom contient sa valeur. Elle se règle avec `-PselftestOnly=<texte>` dans `neoforge-1.20.1`, `forge-1.20.1`, `fabric-1.20.1`, `forge-1.21.1` et `forge-26.1.2`. Les quatre autres projets ne la transmettent pas.
- Les dossiers de lancement sont ignorés par Git.

### Lancer les self tests

Depuis le dossier d'une cible :

```
cd forge-1.20.1
./gradlew runSelftest -PacceptEula
```

1. `runSelftest` lance d'abord `prepareSelftest`, qui prépare `run-selftest/` :
   - `-PacceptEula` écrit `eula.txt` avec `eula=true`. Le passer signifie que vous acceptez l'EULA de Minecraft (<https://aka.ms/MinecraftEULA>). Sans lui, la tâche s'arrête avec un message, sauf si `run-selftest/eula.txt` contient déjà `eula=true`.
   - Quand `server.properties` manque, elle en écrit un : mode hors ligne, monde plat, pas de protection du spawn, pas de surveillance des ticks, et le port `selftest_port` du `gradle.properties` du projet.
2. Le serveur dédié démarre et exécute la suite du code commun, plus un test pour Farmer's Delight et un pour KubeJS quand ils sont chargés.
3. Une exécution réussie affiche `[SELFTEST] <n> passed, 0 failed` puis `[SELFTEST] RESULT SUCCESS` dans la console et dans `run-selftest/logs/latest.log`, écrit `run-selftest/lauramod-selftest.json`, et le serveur s'arrête tout seul.
4. Le processus du serveur se termine avec le code de sortie 0 quand tous les tests ont réussi, 1 quand un test a échoué, 2 quand le serveur s'est arrêté avant l'exécution des tests. Une exécution sans la ligne `RESULT SUCCESS` n'est pas une réussite.

Le monde est conservé d'une exécution à l'autre. Pour repartir d'un monde neuf, supprimez `run-selftest/world` avant l'exécution. Avec `-PwithCompatMods`, les projets 1.20.1 utilisent `run-selftest/world-compat` à la place, car un monde garde en mémoire les registres des mods avec lesquels il a été créé.

Chaque cible a son propre port de serveur, ce qui permet de lancer plusieurs self tests en même temps :

| Projet | `selftest_port` |
|---|---|
| `neoforge-1.20.1` | 25623 |
| `forge-1.20.1` | 25621 |
| `fabric-1.20.1` | 25622 |
| `neoforge-1.21.1` | 25611 |
| `forge-1.21.1` | 25612 |
| `fabric-1.21.1` | 25613 |
| `neoforge-26.1.2` | 25631 |
| `forge-26.1.2` | 25632 |
| `fabric-26.1.2` | 25633 |

`server.properties` n'est écrit que s'il manque : supprimez-le après avoir changé `selftest_port`.

### Tester les intégrations facultatives

Les intégrations sont compilées avec les API publiques des autres mods, qui ne sont jamais incluses dans le jar.

| Projet | Dépendances de compilation seulement | Chargés dans les lancements de développement avec `-PwithCompatMods` |
|---|---|---|
| `neoforge-1.20.1` | API Applied Energistics 2 15.4.10, API Curios 5.14.1+1.20.1, Farmer's Delight 1.20.1-1.3.4, KubeJS 2001.6.5-build.26, Rhino 2001.2.2-build.17 | Farmer's Delight et KubeJS (avec Rhino et Architectury 9.1.12) |
| `forge-1.20.1` | API Applied Energistics 2 15.4.10, API Curios 5.14.1+1.20.1, Farmer's Delight 1.20.1-1.3.4, KubeJS 2001.6.5-build.26, Rhino 2001.2.2-build.17 | Farmer's Delight et KubeJS (avec Rhino et Architectury 9.1.12) |
| `fabric-1.20.1` | API Applied Energistics 2 15.4.10, Farmer's Delight Refabricated 1.20.1-2.5.7, KubeJS 2001.6.5-build.26, Rhino 2001.2.2-build.17 | Farmer's Delight Refabricated (avec les jars qu'il contient) et KubeJS (avec Rhino et Architectury 9.1.12) |
| `neoforge-1.21.1` | API Applied Energistics 2 19.0.27, API Curios 9.5.1+1.21.1, Farmer's Delight 1.21.1-1.3.4, KubeJS 2101.7.2-build.377 | Farmer's Delight et KubeJS |
| `forge-1.21.1` | aucune | non disponible |
| `fabric-1.21.1` | Farmer's Delight Refabricated 1.21.1-3.3.6 | Farmer's Delight Refabricated |
| `neoforge-26.1.2` | aucune | non disponible |
| `forge-26.1.2` | aucune | non disponible |
| `fabric-26.1.2` | Farmer's Delight Refabricated 26.1-3.6.26 | Farmer's Delight Refabricated |

- Applied Energistics 2 et Curios ne sont jamais chargés dans les lancements de développement : leurs ponts n'ont donc aucun test en jeu. En 1.20.1, ils n'ont jamais été lancés en jeu.
- Avec `-PwithCompatMods`, les projets qui chargent KubeJS copient le script de test `src/selftest/kubejs/server_scripts/laura_selftest.js` dans `run-selftest/` avant le self test.

Exemple :

```
cd neoforge-1.21.1
./gradlew runSelftest -PacceptEula -PwithCompatMods
```

### Outils

| Script | Utilisation | Rôle |
|---|---|---|
| `tools/generate_assets.py` | `python tools/generate_assets.py [resources_dir] [--mc <mcversion>] [--no-sounds] [--preview <dossier>]` | Génère les skins, les icônes d'interface, les textures d'objets et de blocs, les modèles de blocs, le logo et les fichiers sonores dans le dossier de ressources d'une version de Minecraft (par défaut `common/1.21.1/src/main/resources`). Demande Pillow, numpy et ffmpeg avec libvorbis. |
| `tools/check_lang.py` | `python tools/check_lang.py [--mc <mcversion>] [--strict]` | Vérifie que chaque clé de traduction et chaque clé de dialogue utilisée par le code existe. Pour chaque fichier de langue et de dialogues, il liste les clés manquantes (l'anglais sert de référence), les clés inutilisées et les variables qui diffèrent de l'anglais. Code de sortie 1 quand l'anglais est incomplet, ou quand un fichier quelconque est incomplet avec `--strict`. Version par défaut : 1.21.1. |
| `tools/make_branches.sh` | `bash tools/make_branches.sh` | Régénère les branches par cible depuis `main`. Lancez-le depuis la racine du dépôt, avec un arbre de travail propre (aucun fichier modifié ou non suivi). Chaque branche reçoit son projet à la racine, le code commun de sa version de Minecraft fusionné dans `src/main`, et les fichiers partagés (README, changelog, licence, fichiers communautaires, `.github`, `docs`). Une branche existante reçoit un nouveau commit par-dessus son historique, une branche inchangée n'est pas touchée. Seules les branches locales sont mises à jour : poussez-les vous-même. |
| `tools/collect_jars.sh` | `bash tools/collect_jars.sh [--build]` | Copie le jar du mod de chaque cible depuis son `build/libs/` vers `jars/` à la racine du dépôt, un dossier local ignoré par Git. Les jars de sources, de développement et de javadoc sont ignorés. Avec `--build`, il lance d'abord `./gradlew build` dans le dossier de chaque cible. Une cible sans jar est signalée. |

- Lancez `check_lang.py` après avoir ajouté un texte traduisible ou une clé de dialogue, une fois par version de Minecraft (`--mc 1.20.1`, `--mc 1.21.1`, `--mc 26.1.2`). L'intégration continue le lance pour chaque version.
- `generate_assets.py --mc 26.1.2` réécrit aussi `models/item/laura_spawn_egg.json` avec le modèle d'œuf d'apparition des anciennes versions, que Minecraft 26.1 n'a plus. Restaurez ensuite le fichier commité (`git checkout -- common/26.1.2/src/main/resources/assets/lauramod/models/item/laura_spawn_egg.json`).

### Plan du code

Racine des paquets : `com.vyrriox.lauramod` dans `common/<mcversion>/src/main/java`. Les trois versions de Minecraft ont les mêmes paquets.

| Paquet | Contenu |
|---|---|
| `api` | API publique pour les autres mods et les scripts. |
| `block`, `item`, `registry` | La tombe, le Cœur de Laura, les objets enregistrés. |
| `client` | Menu, roue des émotes, cadre des besoins, rendu, animations, skins et modèles côté client. |
| `command` | L'arbre de commandes `/laura`. |
| `config` | Les deux fichiers de réglages. |
| `cooking`, `gift`, `desire` | Tables du livre de cuisine, des cadeaux et des désirs. |
| `dialogue` | Fichiers de dialogues, reconnaissance du chat, langues. |
| `entity` | La compagne, ses objectifs (`ai`), sa personnalité (`brain`) et ses métiers (`work`). |
| `model` | Lecture des modèles Blockbench, échantillonnage des animations, Molang. |
| `network` | Le canal réseau unique du mod. |
| `platform` | Ce que le code commun attend d'un chargeur. |
| `skin` | Références de skin, stockage des fichiers du serveur, envois. |
| `test` | Les self tests en jeu. |
| `world` | Ordres, chat, invocation, tombes, progrès, données du monde. |

Le code commun n'appelle que des méthodes du jeu de base. Tout ce qui est propre à un chargeur passe par l'interface `Platform`.

### Licence

MIT. Voir le fichier `LICENSE` à la racine du dépôt.
