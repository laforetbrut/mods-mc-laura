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
| JDK 21 | Minecraft 1.21.1 runs on Java 21. The projects use a Gradle toolchain, with the foojay resolver plugin to locate one. |
| Gradle | Not needed. Each project ships its own wrapper (`gradlew`, `gradlew.bat`). |
| Python 3 | Only for the scripts of `tools/`. |

### Repository layout

```
common/<mcversion>/src/main/     code and resources shared by every loader
neoforge-1.21.1/                 NeoForge project
forge-1.21.1/                    Forge project
fabric-1.21.1/                   Fabric project
tools/                           asset generator and language checker
docs/                            this documentation
```

- Each `<loader>-<mcversion>/` folder is a **standalone Gradle project**. There is no root build.
- Its `build.gradle` adds `../common/<mcversion>/src/main/java` and `../common/<mcversion>/src/main/resources` to the main source set. The loader folder itself only holds the glue: entry points, networking, the `Platform` implementation and the optional integrations (`compat` package).
- On a per-target branch the common code is merged into `src/main` of the project, and the include does nothing.
- Version 2.0.0 targets Minecraft 1.21.1 on NeoForge, Forge and Fabric. Other targets are listed in the README.

### Build a jar

From the folder of the loader you want:

```
cd neoforge-1.21.1
./gradlew build
```

On Windows use `gradlew.bat build`. The jar is written to `build/libs/`:

| Project | Jar |
|---|---|
| `neoforge-1.21.1` | `lauramod-neoforge-1.21.1-2.0.0.jar` |
| `forge-1.21.1` | `lauramod-forge-1.21.1-2.0.0.jar` |
| `fabric-1.21.1` | `lauramod-fabric-1.21.1-2.0.0.jar` |

Mod metadata (`neoforge.mods.toml`, `mods.toml`, `fabric.mod.json`) is filled in from the `gradle.properties` of the project: `mod_id`, `mod_name`, `mod_version`, `mod_authors`, `mod_license`, `mod_description`. The license file of the repository root is copied into the jar as `LICENSE_lauramod`.

### Toolchain of each project

| | NeoForge 1.21.1 | Forge 1.21.1 | Fabric 1.21.1 |
|---|---|---|---|
| Gradle wrapper | 9.2.1 | 9.3.1 | 9.5.1 |
| Build plugin | `net.neoforged.moddev` 2.0.147 | `net.minecraftforge.gradle` 7.0.40 | `fabric-loom` 1.17.21 |
| Loader | NeoForge 21.1.252 | Forge 52.1.16 | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 |
| Mappings | Parchment 2024.11.17 | official | official Mojang mappings |
| Java | 21 | 21 | 21 |

### Development runs

`neoforge-1.21.1/build.gradle` declares four runs:

| Run | Gradle task | Game folder | Purpose |
|---|---|---|---|
| `client` | `runClient` | `run/` | A normal client. |
| `server` | `runServer` | `run/` | A dedicated server without GUI. |
| `clienttest` | `runClienttest` | `run-clienttest/` | A client started with `-Dlauramod.clienttest=true` on the quick play world `laura_test`. It summons a companion, opens every screen of the mod, saves a screenshot of each in `screenshots/`, then quits. |
| `selftest` | `runSelftest` | `run-selftest/` | A dedicated server started with `-Dlauramod.selftest=true`. It runs the in-game test suite, writes `lauramod-selftest.json` in the game folder and stops. |

- The Forge project declares the same four runs. The Fabric project declares `client`, `server` and `selftest`.
- The self test ends the process with an exit code: 0 when every test passed, 1 when a test failed, 2 when the server stopped before the tests ran.
- The system property `lauramod.selftest.only` keeps only the tests whose name contains its value. In the Forge project it is set with `-PselftestOnly=<text>`.
- The `clienttest` run needs a singleplayer world named `laura_test` in `run-clienttest/saves`.
- The run folders are ignored by Git.

### Testing the optional integrations

The integrations are compiled against the public APIs of the other mods, which are never bundled in the jar.

| Project | Compile-only dependencies | Loaded in dev runs with `-PwithCompatMods` |
|---|---|---|
| `neoforge-1.21.1` | Applied Energistics 2 API 19.0.27, Curios API 9.5.1+1.21.1, Farmer's Delight 1.21.1-1.3.4, KubeJS 2101.7.2-build.377 | Farmer's Delight and KubeJS |
| `fabric-1.21.1` | Farmer's Delight Refabricated 1.21.1-3.3.6 | Farmer's Delight Refabricated |
| `forge-1.21.1` | none | not available |

Example:

```
cd neoforge-1.21.1
./gradlew runSelftest -PwithCompatMods
```

### Tools

| Script | Usage | Role |
|---|---|---|
| `tools/generate_assets.py` | `python tools/generate_assets.py [resources_dir] [--mc 1.21.1] [--no-sounds] [--preview <folder>]` | Generates the skins, GUI icons, item and block textures, block models, the logo and the sound files into the resources folder of a Minecraft version (default `common/1.21.1/src/main/resources`). Requires Pillow, numpy and ffmpeg with libvorbis. |
| `tools/check_lang.py` | `python tools/check_lang.py [--mc 1.21.1] [--strict]` | Checks that every translation key and dialogue key used by the code exists. For each language and dialogue file it lists the missing keys (English is the reference), the unused keys and the placeholders that differ from English. Exit code 1 when English is incomplete, or when any file is incomplete with `--strict`. |

Run `check_lang.py` after adding a translatable text or a dialogue key.

### Code map

Package root: `com.vyrriox.lauramod` in `common/1.21.1/src/main/java`.

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
| JDK 21 | Minecraft 1.21.1 fonctionne avec Java 21. Les projets utilisent une toolchain Gradle, avec le plugin foojay resolver pour en trouver une. |
| Gradle | Inutile. Chaque projet fournit son propre wrapper (`gradlew`, `gradlew.bat`). |
| Python 3 | Uniquement pour les scripts de `tools/`. |

### Organisation du dépôt

```
common/<mcversion>/src/main/     code et ressources partagés par tous les chargeurs
neoforge-1.21.1/                 projet NeoForge
forge-1.21.1/                    projet Forge
fabric-1.21.1/                   projet Fabric
tools/                           générateur de ressources et vérificateur de langues
docs/                            cette documentation
```

- Chaque dossier `<chargeur>-<mcversion>/` est un **projet Gradle autonome**. Il n'y a pas de build racine.
- Son `build.gradle` ajoute `../common/<mcversion>/src/main/java` et `../common/<mcversion>/src/main/resources` au source set principal. Le dossier du chargeur ne contient que la colle : points d'entrée, réseau, implémentation de `Platform` et intégrations facultatives (paquet `compat`).
- Sur une branche dédiée à une cible, le code commun est fusionné dans `src/main` du projet, et l'inclusion ne fait rien.
- La version 2.0.0 vise Minecraft 1.21.1 sur NeoForge, Forge et Fabric. Les autres cibles sont listées dans le README.

### Construire un jar

Depuis le dossier du chargeur voulu :

```
cd neoforge-1.21.1
./gradlew build
```

Sous Windows, utilisez `gradlew.bat build`. Le jar est écrit dans `build/libs/` :

| Projet | Jar |
|---|---|
| `neoforge-1.21.1` | `lauramod-neoforge-1.21.1-2.0.0.jar` |
| `forge-1.21.1` | `lauramod-forge-1.21.1-2.0.0.jar` |
| `fabric-1.21.1` | `lauramod-fabric-1.21.1-2.0.0.jar` |

Les métadonnées du mod (`neoforge.mods.toml`, `mods.toml`, `fabric.mod.json`) sont remplies à partir du `gradle.properties` du projet : `mod_id`, `mod_name`, `mod_version`, `mod_authors`, `mod_license`, `mod_description`. Le fichier de licence de la racine du dépôt est copié dans le jar sous le nom `LICENSE_lauramod`.

### Outils de build de chaque projet

| | NeoForge 1.21.1 | Forge 1.21.1 | Fabric 1.21.1 |
|---|---|---|---|
| Wrapper Gradle | 9.2.1 | 9.3.1 | 9.5.1 |
| Plugin de build | `net.neoforged.moddev` 2.0.147 | `net.minecraftforge.gradle` 7.0.40 | `fabric-loom` 1.17.21 |
| Chargeur | NeoForge 21.1.252 | Forge 52.1.16 | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 |
| Mappings | Parchment 2024.11.17 | officiels | mappings officiels de Mojang |
| Java | 21 | 21 | 21 |

### Lancements de développement

`neoforge-1.21.1/build.gradle` déclare quatre lancements :

| Lancement | Tâche Gradle | Dossier de jeu | Rôle |
|---|---|---|---|
| `client` | `runClient` | `run/` | Un client normal. |
| `server` | `runServer` | `run/` | Un serveur dédié sans interface. |
| `clienttest` | `runClienttest` | `run-clienttest/` | Un client lancé avec `-Dlauramod.clienttest=true` sur le monde en jeu rapide `laura_test`. Il invoque une compagne, ouvre chaque écran du mod, enregistre une capture de chacun dans `screenshots/`, puis quitte. |
| `selftest` | `runSelftest` | `run-selftest/` | Un serveur dédié lancé avec `-Dlauramod.selftest=true`. Il exécute la suite de tests en jeu, écrit `lauramod-selftest.json` dans le dossier de jeu et s'arrête. |

- Le projet Forge déclare les quatre mêmes lancements. Le projet Fabric déclare `client`, `server` et `selftest`.
- Le self test termine le processus avec un code de sortie : 0 quand tous les tests ont réussi, 1 quand un test a échoué, 2 quand le serveur s'est arrêté avant l'exécution des tests.
- La propriété système `lauramod.selftest.only` ne garde que les tests dont le nom contient sa valeur. Dans le projet Forge elle se règle avec `-PselftestOnly=<texte>`.
- Le lancement `clienttest` a besoin d'un monde solo nommé `laura_test` dans `run-clienttest/saves`.
- Les dossiers de lancement sont ignorés par Git.

### Tester les intégrations facultatives

Les intégrations sont compilées avec les API publiques des autres mods, qui ne sont jamais incluses dans le jar.

| Projet | Dépendances de compilation seulement | Chargés dans les lancements de développement avec `-PwithCompatMods` |
|---|---|---|
| `neoforge-1.21.1` | API Applied Energistics 2 19.0.27, API Curios 9.5.1+1.21.1, Farmer's Delight 1.21.1-1.3.4, KubeJS 2101.7.2-build.377 | Farmer's Delight et KubeJS |
| `fabric-1.21.1` | Farmer's Delight Refabricated 1.21.1-3.3.6 | Farmer's Delight Refabricated |
| `forge-1.21.1` | aucune | non disponible |

Exemple :

```
cd neoforge-1.21.1
./gradlew runSelftest -PwithCompatMods
```

### Outils

| Script | Utilisation | Rôle |
|---|---|---|
| `tools/generate_assets.py` | `python tools/generate_assets.py [resources_dir] [--mc 1.21.1] [--no-sounds] [--preview <dossier>]` | Génère les skins, les icônes d'interface, les textures d'objets et de blocs, les modèles de blocs, le logo et les fichiers sonores dans le dossier de ressources d'une version de Minecraft (par défaut `common/1.21.1/src/main/resources`). Demande Pillow, numpy et ffmpeg avec libvorbis. |
| `tools/check_lang.py` | `python tools/check_lang.py [--mc 1.21.1] [--strict]` | Vérifie que chaque clé de traduction et chaque clé de dialogue utilisée par le code existe. Pour chaque fichier de langue et de dialogues, il liste les clés manquantes (l'anglais sert de référence), les clés inutilisées et les variables qui diffèrent de l'anglais. Code de sortie 1 quand l'anglais est incomplet, ou quand un fichier quelconque est incomplet avec `--strict`. |

Lancez `check_lang.py` après avoir ajouté un texte traduisible ou une clé de dialogue.

### Plan du code

Racine des paquets : `com.vyrriox.lauramod` dans `common/1.21.1/src/main/java`.

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
