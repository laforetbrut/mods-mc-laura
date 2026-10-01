# My Girlfriend Laura

[![Build](https://github.com/laforetbrut/mods-mc-laura/actions/workflows/build.yml/badge.svg)](https://github.com/laforetbrut/mods-mc-laura/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1%20%7C%2026.1.2-green)
![Loaders](https://img.shields.io/badge/Loaders-NeoForge%20%7C%20Forge%20%7C%20Fabric-orange)

A Minecraft mod that adds Laura, a companion who shares your world. She follows you, talks with
you in chat in your own language, has needs, moods and wishes, fetches items, works as a
lumberjack, a farmer or a cook, and wears the skin or the Blockbench model you choose.

Say "I feel lonely" in chat. She takes it from there.

Version 2.0.0, mod id `lauramod`, for NeoForge, Forge and Fabric on Minecraft 1.20.1, 1.21.1 and
26.1.2.

## Features

- **A companion who follows you**: she walks with you, into the Nether and the End too, and only
  teleports when you are more than 128 blocks away, always to a safe spot next to you. She can
  also stay, wander, come or go home.
- **Talks in your language**: she reads the chat and understands plain sentences such as
  "follow me", "bring me 16 bread" or "be a farmer", chained with "then". She answers in chat and
  in a bubble above her head, heard within 64 blocks. 18 languages, and more can be added from
  config files.
- **Needs, moods and affection**: hunger, energy, fun, attention and hygiene, ten moods, and
  affection from "Hates you" to "Soulmates". About every 12 minutes she wishes for an item, a
  place or an activity.
- **Fetch, jobs and chests**: she brings items from the ground, chests and modded storage within
  24 blocks, works as a lumberjack, a farmer or a cook, follows a to-do list of 16 tasks, and uses
  chests assigned to eight purposes.
- **Several companions**: 3 per player by default, each bound to the player who summoned her.
  If one dies, a flower on a Laura's Gravestone brings her back with her inventory.
- **Made for servers**: she follows the PvP setting and the teams of the server, leaves locked
  and protected containers alone, and skin and model transfers are limited in size and rate.
- **Your look**: six built-in skins, any PNG skin, the skin of a Minecraft account, and custom
  Blockbench models (`.bbmodel`, `.geo.json`) with their own animations.
- **Menus**: a menu with seven tabs (key K), an emote wheel with 30 emotes (key G), an inventory
  with armor, hand and back slots, and an optional needs overlay.
- **58 advancements** in their own tab.
- **Readable settings**: JSON files in `config/lauramod/`, applied with `/laura reload`.
- **For modpacks**: a public Java API, KubeJS scripting on some targets, and optional
  integrations with other mods (see below).

## Supported versions

| Minecraft | Loader | Built and tested with | Java | Jar |
|---|---|---|---|---|
| 1.20.1 | NeoForge | NeoForge 47.1.106 (the 47.1 fork of Forge) | 17 | `lauramod-neoforge-1.20.1-2.0.0.jar` |
| 1.20.1 | Forge | Forge 47.4.10 | 17 | `lauramod-forge-1.20.1-2.0.0.jar` |
| 1.20.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.92.12+1.20.1 | 17 | `lauramod-fabric-1.20.1-2.0.0.jar` |
| 1.21.1 | NeoForge | NeoForge 21.1.252 | 21 | `lauramod-neoforge-1.21.1-2.0.0.jar` |
| 1.21.1 | Forge | Forge 52.1.16 | 21 | `lauramod-forge-1.21.1-2.0.0.jar` |
| 1.21.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 | 21 | `lauramod-fabric-1.21.1-2.0.0.jar` |
| 26.1.2 | NeoForge | NeoForge 26.1.2.109 | 25 | `lauramod-neoforge-26.1.2-2.0.0.jar` |
| 26.1.2 | Forge | Forge 64.1.3 | 25 | `lauramod-forge-26.1.2-2.0.0.jar` |
| 26.1.2 | Fabric | Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2 | 25 | `lauramod-fabric-26.1.2-2.0.0.jar` |

Each jar is built for one loader and one Minecraft version. Download the one that matches
yours from the CurseForge page of the mod or from the
[releases page](https://github.com/laforetbrut/mods-mc-laura/releases), or build it (see
[Building from source](#building-from-source)). The nine targets share the same features and
pass the in-game self test suite; only the optional integrations differ.

Coming from 1.x? 2.0.0 is a full rewrite, and a companion saved by 1.x is kept. The differences
are listed in the [changelog](CHANGELOG.md).

## Requirements

- Minecraft Java Edition with the loader of your target:
  [NeoForge](https://neoforged.net/), [Forge](https://files.minecraftforge.net/) or
  [Fabric](https://fabricmc.net/use/).
- The Java version of the table (most launchers provide it).
- On Fabric: [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version.
- No other mod is required.
- In multiplayer the mod is needed on both sides: on the server and on every client.

## Installation

### Singleplayer

1. Install NeoForge, Forge or Fabric for your Minecraft version.
2. On Fabric, put Fabric API in the `mods` folder of your game.
3. Put the jar matching your Minecraft version and loader in the `mods` folder.
4. Start the game with the loader profile and open a world.
5. Say "I feel lonely" in chat, or run `/laura summon`. Press K near her to open her menu.

### Server

1. Install the same loader on the server.
2. Put the same jar in the `mods` folder of the server (and Fabric API on Fabric).
3. Start the server. Its settings are in `config/lauramod/`, see [CONFIG.md](docs/CONFIG.md).
   `/laura reload` applies changes (permission level 2 by default).
4. Every player installs the same jar on their own client, as in the singleplayer steps.

## Optional integrations

None of these mods is required and none is bundled in the jar.

| Target | Farmer's Delight | KubeJS | Applied Energistics 2 | Curios | Backpack as storage |
|---|---|---|---|---|---|
| NeoForge 1.20.1 | yes | yes | compiled in, never run in game | yes | yes |
| Forge 1.20.1 | yes | yes | compiled in, never run in game | yes | yes |
| Fabric 1.20.1 | yes, Refabricated | yes | compiled in, never run in game | no | no |
| NeoForge 1.21.1 | yes | yes | yes | yes | yes |
| Forge 1.21.1 | no | no | no | no | yes |
| Fabric 1.21.1 | yes, Refabricated | no | no | no | no |
| NeoForge 26.1.2 | no | no | no | no | yes |
| Forge 26.1.2 | no | no | no | no | yes |
| Fabric 26.1.2 | yes, Refabricated | no | no | no | yes |

- **On every target**: storage blocks of other mods work like vanilla chests, she can wear a
  backpack on her back, Carry On cannot pick her up, and she rejoins you after a waystone, a
  teleport command or an ender pearl.
- **Farmer's Delight**: as a cook she also uses cooking pots. On Fabric the supported port is
  Farmer's Delight Refabricated.
- **KubeJS**: server scripts get the `Laura` binding and the `LauraEvents` event group.
- **Applied Energistics 2**: any block of a powered ME network gives her the whole network storage.
- **Curios**: she wears trinkets in seven slots.
- **Backpack as storage**: a backpack that exposes its inventory adds storage. Elsewhere it is
  worn for the look only.

The in-game self test suite checks Farmer's Delight, KubeJS and Curios. Applied Energistics 2
has no in-game test. Details: [COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Documentation

Ten guides, each in English then French:

- [Actions and behaviour](docs/ACTIONS.md)
- [Commands](docs/COMMANDS.md)
- [Configuration](docs/CONFIG.md)
- [Skins](docs/SKINS.md)
- [Custom models and animations](docs/MODELS.md)
- [Languages and dialogues](docs/LANGUAGES.md)
- [KubeJS scripting](docs/KUBEJS.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Building from source](docs/BUILDING.md)
- [FAQ](docs/FAQ.md)

What changed in each version: [CHANGELOG.md](CHANGELOG.md).

## Building from source

Each target is a standalone Gradle project with its own wrapper. You need the JDK of the target
(17, 21 or 25). From its folder:

```
cd neoforge-1.21.1
./gradlew build
```

On Windows use `gradlew.bat build`. The jar is written to `build/libs/`. To run the in-game self
test suite on a dedicated server (this accepts the
[Minecraft EULA](https://aka.ms/MinecraftEULA)):

```
./gradlew runSelftest -PacceptEula
```

To build the nine targets and collect their jars in the local `jars/` folder (ignored by Git),
run this from the repository root:

```
bash tools/collect_jars.sh --build
```

Toolchains, development runs, self tests and the optional mods of each target are described in
[docs/BUILDING.md](docs/BUILDING.md).

## Repository layout

```
common/<mcversion>/src/main/   code and resources shared by the three loaders of one Minecraft version
<loader>-<mcversion>/          one standalone Gradle project per target, nine in all
tools/                         asset generator, language checker, branch generator, jar collector
docs/                          the ten guides
```

- `main` holds everything: one folder per target (`neoforge-1.20.1` to `fabric-26.1.2`) and one
  `common/<mcversion>` folder per Minecraft version (1.20.1, 1.21.1, 26.1.2). There is no root build.
- One branch per target, named like its folder, is generated from `main` by
  `tools/make_branches.sh`; the branches are created when `main` is published. Such a branch
  holds that single project at the root, with the common code merged into `src/main`. These
  branches are never edited by hand.

## Contributing and security

- How to contribute, build and test: [CONTRIBUTING.md](CONTRIBUTING.md). Pull requests go
  against `main`.
- Questions, bugs and requests:
  [GitHub issues](https://github.com/laforetbrut/mods-mc-laura/issues/new/choose).
- Security problems are reported privately, see [SECURITY.md](SECURITY.md).
- Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE)

## Credits

Author: vyrriox

Created for Zenh

---

# My Girlfriend Laura (Version Française)

Un mod Minecraft qui ajoute Laura, une compagne qui partage votre monde. Elle vous suit, discute
avec vous dans le chat dans votre langue, a des besoins, des humeurs et des envies, rapporte des
objets, travaille comme bûcheronne, fermière ou cuisinière, et porte le skin ou le modèle
Blockbench de votre choix.

Dites « je me sens seul » dans le chat. Elle s'occupe du reste.

Version 2.0.0, identifiant de mod `lauramod`, pour NeoForge, Forge et Fabric sur Minecraft
1.20.1, 1.21.1 et 26.1.2.

## Caractéristiques

- **Une compagne qui vous suit** : elle marche avec vous, jusque dans le Nether et l'End, et ne se
  téléporte que si vous êtes à plus de 128 blocs, toujours à un endroit sûr à côté de vous. Elle
  peut aussi rester sur place, se promener, venir ou rentrer à la maison.
- **Elle parle votre langue** : elle lit le chat et comprend des phrases simples comme
  « suis-moi », « apporte-moi 16 pains » ou « sois fermière », enchaînées avec « puis ». Elle
  répond dans le chat et dans une bulle au-dessus de sa tête, entendue dans un rayon de 64 blocs.
  18 langues, et d'autres s'ajoutent par des fichiers de configuration.
- **Besoins, humeurs et affection** : faim, énergie, amusement, attention et hygiène, dix humeurs,
  et une affection qui va de « Te déteste » à « Âmes sœurs ». Toutes les 12 minutes environ, elle
  désire un objet, un lieu ou une activité.
- **Rapporter, travailler, ranger** : elle rapporte des objets trouvés au sol, dans les coffres et
  les rangements de mods dans un rayon de 24 blocs, travaille comme bûcheronne, fermière ou
  cuisinière, suit une liste de 16 tâches, et utilise des coffres attribués à huit usages.
- **Plusieurs compagnes** : 3 par joueur par défaut, chacune liée au joueur qui l'a invoquée.
  Si l'une meurt, une fleur sur une Tombe de Laura la ramène avec son inventaire.
- **Pensée pour les serveurs** : elle suit le réglage PvP et les équipes du serveur, ne touche
  pas aux conteneurs verrouillés ou protégés, et les transferts de skins et de modèles sont
  limités en taille et en débit.
- **Son apparence** : six skins intégrés, n'importe quel skin PNG, le skin d'un compte Minecraft,
  et des modèles Blockbench personnalisés (`.bbmodel`, `.geo.json`) avec leurs propres animations.
- **Menus** : un menu à sept onglets (touche K), une roue de 30 émotes (touche G), un inventaire
  avec emplacements d'armure, de mains et de dos, et un cadre des besoins en option.
- **58 progrès** dans leur propre onglet.
- **Des réglages lisibles** : des fichiers JSON dans `config/lauramod/`, appliqués avec
  `/laura reload`.
- **Pour les modpacks** : une API Java publique, des scripts KubeJS sur certaines cibles, et des
  intégrations facultatives avec d'autres mods (voir plus bas).

## Versions supportées

| Minecraft | Chargeur | Compilé et testé avec | Java | Jar |
|---|---|---|---|---|
| 1.20.1 | NeoForge | NeoForge 47.1.106 (le fork 47.1 de Forge) | 17 | `lauramod-neoforge-1.20.1-2.0.0.jar` |
| 1.20.1 | Forge | Forge 47.4.10 | 17 | `lauramod-forge-1.20.1-2.0.0.jar` |
| 1.20.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.92.12+1.20.1 | 17 | `lauramod-fabric-1.20.1-2.0.0.jar` |
| 1.21.1 | NeoForge | NeoForge 21.1.252 | 21 | `lauramod-neoforge-1.21.1-2.0.0.jar` |
| 1.21.1 | Forge | Forge 52.1.16 | 21 | `lauramod-forge-1.21.1-2.0.0.jar` |
| 1.21.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1 | 21 | `lauramod-fabric-1.21.1-2.0.0.jar` |
| 26.1.2 | NeoForge | NeoForge 26.1.2.109 | 25 | `lauramod-neoforge-26.1.2-2.0.0.jar` |
| 26.1.2 | Forge | Forge 64.1.3 | 25 | `lauramod-forge-26.1.2-2.0.0.jar` |
| 26.1.2 | Fabric | Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2 | 25 | `lauramod-fabric-26.1.2-2.0.0.jar` |

Chaque jar est compilé pour un seul chargeur et une seule version de Minecraft. Téléchargez
celui qui correspond aux vôtres depuis la page CurseForge du mod ou depuis la
[page des releases](https://github.com/laforetbrut/mods-mc-laura/releases), ou compilez-le (voir
[Compiler depuis les sources](#compiler-depuis-les-sources)). Les neuf cibles ont les mêmes
fonctionnalités et réussissent la suite de self tests en jeu ; seules les intégrations
facultatives diffèrent.

Vous venez de la 1.x ? La 2.0.0 est une réécriture complète, et une compagne enregistrée par la
1.x est conservée. Les différences sont listées dans le [changelog](CHANGELOG.md).

## Prérequis

- Minecraft Java Edition avec le chargeur de votre cible :
  [NeoForge](https://neoforged.net/), [Forge](https://files.minecraftforge.net/) ou
  [Fabric](https://fabricmc.net/use/).
- La version de Java du tableau (la plupart des launchers la fournissent).
- Sur Fabric : [Fabric API](https://modrinth.com/mod/fabric-api) pour votre version de Minecraft.
- Aucun autre mod n'est obligatoire.
- En multijoueur, le mod est nécessaire des deux côtés : sur le serveur et sur chaque client.

## Installation

### Solo

1. Installez NeoForge, Forge ou Fabric pour votre version de Minecraft.
2. Sur Fabric, placez Fabric API dans le dossier `mods` de votre jeu.
3. Placez le jar correspondant à votre version de Minecraft et à votre chargeur dans le dossier
   `mods`.
4. Lancez le jeu avec le profil du chargeur et ouvrez un monde.
5. Dites « je me sens seul » dans le chat, ou lancez `/laura summon`. Appuyez sur K près d'elle
   pour ouvrir son menu.

### Serveur

1. Installez le même chargeur sur le serveur.
2. Placez le même jar dans le dossier `mods` du serveur (et Fabric API sur Fabric).
3. Démarrez le serveur. Ses réglages sont dans `config/lauramod/`, voir
   [CONFIG.md](docs/CONFIG.md). `/laura reload` applique les changements (niveau de permission 2
   par défaut).
4. Chaque joueur installe le même jar sur son propre client, comme pour le solo.

## Intégrations facultatives

Aucun de ces mods n'est obligatoire et aucun n'est inclus dans le jar.

| Cible | Farmer's Delight | KubeJS | Applied Energistics 2 | Curios | Sac à dos comme rangement |
|---|---|---|---|---|---|
| NeoForge 1.20.1 | oui | oui | compilé, jamais lancé en jeu | oui | oui |
| Forge 1.20.1 | oui | oui | compilé, jamais lancé en jeu | oui | oui |
| Fabric 1.20.1 | oui, Refabricated | oui | compilé, jamais lancé en jeu | non | non |
| NeoForge 1.21.1 | oui | oui | oui | oui | oui |
| Forge 1.21.1 | non | non | non | non | oui |
| Fabric 1.21.1 | oui, Refabricated | non | non | non | non |
| NeoForge 26.1.2 | non | non | non | non | oui |
| Forge 26.1.2 | non | non | non | non | oui |
| Fabric 26.1.2 | oui, Refabricated | non | non | non | oui |

- **Sur toutes les cibles** : les rangements des autres mods fonctionnent comme les coffres du jeu
  de base, elle peut porter un sac à dos sur son dos, Carry On ne peut pas la porter, et elle vous
  rejoint après un waystone, une commande de téléportation ou une perle de l'Ender.
- **Farmer's Delight** : cuisinière, elle utilise aussi les marmites. Sur Fabric, le portage pris
  en charge est Farmer's Delight Refabricated.
- **KubeJS** : les scripts serveur reçoivent la liaison `Laura` et le groupe d'événements
  `LauraEvents`.
- **Applied Energistics 2** : n'importe quel bloc d'un réseau ME alimenté lui donne accès à tout
  le stockage du réseau.
- **Curios** : elle porte des bijoux dans sept emplacements.
- **Sac à dos comme rangement** : un sac à dos qui expose son inventaire ajoute du rangement.
  Ailleurs, il est porté uniquement pour l'apparence.

La suite de self tests en jeu vérifie Farmer's Delight, KubeJS et Curios. Applied Energistics 2
n'a aucun test en jeu. Détails : [COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Documentation

Dix guides, chacun en anglais puis en français :

- [Actions et comportement](docs/ACTIONS.md)
- [Commandes](docs/COMMANDS.md)
- [Configuration](docs/CONFIG.md)
- [Skins](docs/SKINS.md)
- [Modèles personnalisés et animations](docs/MODELS.md)
- [Langues et dialogues](docs/LANGUAGES.md)
- [Scripts KubeJS](docs/KUBEJS.md)
- [Compatibilité](docs/COMPATIBILITY.md)
- [Compiler depuis les sources](docs/BUILDING.md)
- [FAQ](docs/FAQ.md)

Ce qui change à chaque version : [CHANGELOG.md](CHANGELOG.md).

## Compiler depuis les sources

Chaque cible est un projet Gradle autonome avec son propre wrapper. Il faut le JDK de la cible
(17, 21 ou 25). Depuis son dossier :

```
cd neoforge-1.21.1
./gradlew build
```

Sous Windows, utilisez `gradlew.bat build`. Le jar est écrit dans `build/libs/`. Pour lancer la
suite de self tests en jeu sur un serveur dédié (cela accepte
l'[EULA de Minecraft](https://aka.ms/MinecraftEULA)) :

```
./gradlew runSelftest -PacceptEula
```

Pour compiler les neuf cibles et rassembler leurs jars dans le dossier local `jars/` (ignoré par
Git), lancez ceci depuis la racine du dépôt :

```
bash tools/collect_jars.sh --build
```

Les outils de build, les lancements de développement, les self tests et les mods facultatifs de
chaque cible sont décrits dans [docs/BUILDING.md](docs/BUILDING.md).

## Organisation du dépôt

```
common/<mcversion>/src/main/   code et ressources partagés par les trois chargeurs d'une version de Minecraft
<chargeur>-<mcversion>/        un projet Gradle autonome par cible, neuf en tout
tools/                         générateur de ressources, vérificateur de langues, générateur de branches, collecte des jars
docs/                          les dix guides
```

- `main` contient tout : un dossier par cible (de `neoforge-1.20.1` à `fabric-26.1.2`) et un
  dossier `common/<mcversion>` par version de Minecraft (1.20.1, 1.21.1, 26.1.2). Il n'y a pas
  de build racine.
- Une branche par cible, nommée comme son dossier, est générée depuis `main` par
  `tools/make_branches.sh` ; les branches sont créées quand `main` est publié. Une telle branche
  contient ce seul projet à la racine, avec le code commun fusionné dans `src/main`. Ces branches
  ne sont jamais modifiées à la main.

## Contribuer et sécurité

- Comment contribuer, compiler et tester : [CONTRIBUTING.md](CONTRIBUTING.md). Les pull requests
  se font sur `main`.
- Questions, bugs et demandes :
  [issues GitHub](https://github.com/laforetbrut/mods-mc-laura/issues/new/choose).
- Les problèmes de sécurité se signalent en privé, voir [SECURITY.md](SECURITY.md).
- Chaque participant suit le [Code de conduite](CODE_OF_CONDUCT.md).

## Licence

[MIT](LICENSE)

## Credits

Author: vyrriox

Créé pour Zenh
