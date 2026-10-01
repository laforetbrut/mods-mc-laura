# Contributing

My Girlfriend Laura (mod id `lauramod`), author / auteur : vyrriox.

- [English](#contributing)
- [Français](#contribuer)

Thanks for wanting to make Laura a better companion.

- Questions, bugs and feature requests: [GitHub issues](https://github.com/laforetbrut/lauramod/issues/new/choose).
- Security problems: never in public, see [SECURITY.md](SECURITY.md).
- Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## Repository layout

```
common/<mcversion>/src/main/     code and resources shared by every loader of that Minecraft version
<loader>-<mcversion>/            one standalone Gradle project per target (neoforge-1.21.1, forge-1.21.1, fabric-1.21.1...)
tools/                           asset generator and language checker (Python), target branch script, jar collector
docs/                            the guides
```

- `main` holds everything. There is no root build: each `<loader>-<mcversion>/` folder is a Gradle project with its own wrapper.
- A loader project adds `../common/<mcversion>/src/main` to its source set. The folder itself only holds the loader glue: entry points, networking, the `Platform` implementation and the optional integrations (`compat` package).
- One branch per target, named like its folder (`neoforge-1.21.1` for example), is generated from `main` by `tools/make_branches.sh`; the branches are created when `main` is published. Such a branch holds that single project at the repository root, with the common code merged into `src/main`. Never edit these branches by hand, and open every pull request against `main`.
- The nine targets are on `main`: Minecraft 1.20.1, 1.21.1 and 26.1.2, each on NeoForge, Forge and Fabric. Minecraft 1.21.1 is the reference.

The full description is in [docs/BUILDING.md](docs/BUILDING.md).

## Build and run one target

From the folder of the target you work on:

```
cd neoforge-1.21.1
./gradlew build
./gradlew runClient
```

On Windows use `gradlew.bat`. The jar is written to `build/libs/`.

Requirements, toolchain versions, the other development runs and the `-PwithCompatMods` switch that loads the optional mods are described in [docs/BUILDING.md](docs/BUILDING.md).

## Self tests

The common code contains an in-game test suite (package `test`). From a loader folder:

```
./gradlew runSelftest
```

This starts a dedicated server in `run-selftest/`. Each test gets a clean platform high in the sky and a fake player. When the last test ends, the report is written to `run-selftest/lauramod-selftest.json` and the server stops.

A passing run prints these lines in the console and in `run-selftest/logs/latest.log`:

```
[SELFTEST] Starting the Laura self test suite
[SELFTEST] PASS registries (0 ticks)
[SELFTEST] PASS dialogues_loaded (0 ticks)
...
[SELFTEST] <n> passed, 0 failed
[SELFTEST] RESULT SUCCESS
```

- The process ends with exit code 0 when every test passed, 1 when a test failed, 2 when the server stopped before the tests ran.
- A failed test prints `[SELFTEST] FAIL <name> (<n> ticks): <reason>`, and the run ends with `[SELFTEST] RESULT FAILURE`.
- A run without the `RESULT SUCCESS` line is not a pass, whatever else is printed.
- The self test starts a Minecraft server, and a server only runs once its EULA (<https://aka.ms/MinecraftEULA>) is accepted. With a new `run-selftest/` folder the run stops and says so. Create `run-selftest/eula.txt` containing `eula=true`, then run the task again. In every project you can instead pass `-PacceptEula` once (`./gradlew runSelftest -PacceptEula`), which writes that file.
- Each project uses its own server port (`selftest_port` in its `gradle.properties`), so several self tests can run at the same time.
- The tests of the optional integrations only run when the other mod is loaded (`-PwithCompatMods`).
- Running a single test by name is explained in [docs/BUILDING.md](docs/BUILDING.md).

New behaviour that can be checked on a server deserves a test in `common/<mcversion>/src/main/java/com/vyrriox/lauramod/test/LauraTestCases.java`. A test builds everything it needs itself and never relies on what an earlier run left in the world.

## Language check

From the repository root:

```
python tools/check_lang.py --strict
```

It checks that every translation key and dialogue key used by the code exists in every language, that every required intent has a trigger, and that the `%s` placeholders of the interface files match English. A passing run prints one line per file, each ending with `-> OK`, then the index line:

```
[lang] fr_fr.json: 484 keys, 0 missing, 0 unused, 0 placeholder mismatches -> OK
[dialogue] fr_fr.json: 216 line keys, 75 intents, 0 missing lines, 0 missing intents -> OK
[index] 18 languages listed, 0 files not listed, 0 listed without a file
```

- The counts grow with the mod. What matters is `0 missing`, `0 placeholder mismatches`, `0 missing lines`, `0 missing intents` and `-> OK` on all 18 files of both kinds.
- The exit code is 0 on success and 1 on failure. Without `--strict` only an incomplete English file or a wrong index fails the run.
- `--mc <mcversion>` checks the common folder of another Minecraft version (default `1.21.1`).

## Translations

There are two kinds of text. Their formats, the trigger rules and the placeholders are described in [docs/LANGUAGES.md](docs/LANGUAGES.md).

| Text | Files |
|---|---|
| Interface: menu, items, advancements, system messages | `common/<mcversion>/src/main/resources/assets/lauramod/lang/<language>.json` |
| Dialogues: what she says and what she understands in chat | `common/<mcversion>/src/main/resources/data/lauramod/dialogues/<language>.json` |

The mod ships 18 languages, and all 18 must stay complete for both kinds: `cs_cz`, `de_de`, `en_us`, `es_es`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `nl_nl`, `pl_pl`, `pt_br`, `ru_ru`, `sv_se`, `tr_tr`, `uk_ua`, `zh_cn`, `zh_tw`.

**Fix a translation**

1. Edit the value in the file of that language. Keys stay as they are.
2. Keep the placeholders of the English text: `%s` and `%1$s` in interface files, `{player}`, `{item}` and the others in dialogue lines.
3. Apply the same change in the common folder of every Minecraft version present on `main`.
4. Run the language check.

You can try a wording in game without building anything: both kinds of files can be overridden from `config/lauramod/`, as explained in [docs/LANGUAGES.md](docs/LANGUAGES.md).

**Add a text**

1. Add the key to `en_us.json` first: English is the reference.
2. Add it to the 17 other files in the same pull request. If you cannot write some of these languages, say which ones in the pull request so that they get reviewed.
3. Run the language check: it lists every file where the key is missing.

**Add a language**

1. Create the interface file and the dialogue file, both named after the Minecraft language code.
2. Add the code to `data/lauramod/dialogues/_index.json`. The mod only loads the built-in dialogue files listed there.
3. Give the `summon` intent its phrase, and every required intent at least one trigger. The language check lists what is missing.
4. Update the language list of [docs/LANGUAGES.md](docs/LANGUAGES.md).

## Coding conventions

- Code, identifiers, comments, log messages and commit messages are written in English.
- Common code only calls vanilla Minecraft methods and the `Platform` interface (package `platform`). A method added by a loader does not exist on the others: `ServerPlayer.getLanguage()`, for example, is a NeoForge and Forge patch. What only a loader can provide becomes a method of `Platform`, implemented in each loader project.
- `common/1.21.1` is the reference. A change made there is carried to the common folder of every other Minecraft version present on `main`, adapted where the game API differs.
- Compile every loader project of a Minecraft version before calling a change in its common folder done.
- Optional integrations live in the `compat` package of a loader project. They are compiled against the public API of the other mod, never bundled and never required.
- New text shown to players is not written in the code: use a translation key (interface) or a dialogue key (what she says).
- Skins, icons, textures and sounds are written by `tools/generate_assets.py`. A manual edit of a generated file is lost at the next run, so change the generator.
- Keep the existing `@author` tags.

## Commit messages

`type: descriptive message` in English. Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `ci`.

Example: `fix: pick left and right limbs of custom models from their position`.

## Pull requests

1. Fork the repository and create a branch from `main`: `feat/short-name` or `fix/short-name`.
2. Make your change, and carry it to every folder it concerns.
3. Build each project you touched: `./gradlew build`. A change in a common folder touches every loader project of that Minecraft version.
4. Run the self tests on each loader whose own folder you changed, and on at least one loader for a change in common code.
5. Run the language check if you added or changed a text.
6. Test what you changed in game (`./gradlew runClient`).
7. Update `CHANGELOG.md` (English and French sections) and the guide of `docs/` that describes the behaviour (both languages).
8. Open the pull request with the template.

A pull request must include:

- What it changes and why.
- The targets it touches, and the ones you built and tested.
- The result line of the self tests (`[SELFTEST] RESULT SUCCESS`) and of the language check when text changed.
- The changelog entry and the documentation update, in English and French.
- New text in all 18 languages.
- No version number change: versions are set by the maintainer at release time.
- No personal information, in files or in pasted logs (IP addresses, tokens, account details).

---

# Contribuer

Merci de vouloir faire de Laura une meilleure compagne.

- Questions, bugs et demandes de fonctionnalités : [issues GitHub](https://github.com/laforetbrut/lauramod/issues/new/choose).
- Problèmes de sécurité : jamais en public, voir [SECURITY.md](SECURITY.md).
- Toute personne qui participe respecte le [Code de conduite](CODE_OF_CONDUCT.md).

## Organisation du dépôt

```
common/<mcversion>/src/main/     code et ressources partagés par tous les chargeurs de cette version de Minecraft
<chargeur>-<mcversion>/          un projet Gradle autonome par cible (neoforge-1.21.1, forge-1.21.1, fabric-1.21.1...)
tools/                           générateur de ressources et vérificateur de langues (Python), script des branches de cible, collecte des jars
docs/                            les guides
```

- `main` contient tout. Il n'y a pas de build racine : chaque dossier `<chargeur>-<mcversion>/` est un projet Gradle avec son propre wrapper.
- Un projet de chargeur ajoute `../common/<mcversion>/src/main` à son source set. Le dossier lui-même ne contient que la colle du chargeur : points d'entrée, réseau, implémentation de `Platform` et intégrations facultatives (paquet `compat`).
- Une branche par cible, qui porte le nom de son dossier (`neoforge-1.21.1` par exemple), est générée à partir de `main` par `tools/make_branches.sh` ; les branches sont créées quand `main` est publié. Une telle branche contient ce seul projet à la racine du dépôt, avec le code commun fusionné dans `src/main`. Ne modifiez jamais ces branches à la main, et ouvrez toutes les pull requests sur `main`.
- Les neuf cibles sont sur `main` : Minecraft 1.20.1, 1.21.1 et 26.1.2, chacun sur NeoForge, Forge et Fabric. Minecraft 1.21.1 est la référence.

La description complète se trouve dans [docs/BUILDING.md](docs/BUILDING.md).

## Compiler et lancer une cible

Depuis le dossier de la cible sur laquelle vous travaillez :

```
cd neoforge-1.21.1
./gradlew build
./gradlew runClient
```

Sous Windows, utilisez `gradlew.bat`. Le jar est écrit dans `build/libs/`.

Les prérequis, les versions des outils, les autres lancements de développement et l'option `-PwithCompatMods` qui charge les mods facultatifs sont décrits dans [docs/BUILDING.md](docs/BUILDING.md).

## Self tests

Le code commun contient une suite de tests en jeu (paquet `test`). Depuis un dossier de chargeur :

```
./gradlew runSelftest
```

Cette commande démarre un serveur dédié dans `run-selftest/`. Chaque test reçoit une plateforme propre, haut dans le ciel, et un faux joueur. Quand le dernier test se termine, le rapport est écrit dans `run-selftest/lauramod-selftest.json` et le serveur s'arrête.

Une exécution réussie affiche ces lignes dans la console et dans `run-selftest/logs/latest.log` :

```
[SELFTEST] Starting the Laura self test suite
[SELFTEST] PASS registries (0 ticks)
[SELFTEST] PASS dialogues_loaded (0 ticks)
...
[SELFTEST] <n> passed, 0 failed
[SELFTEST] RESULT SUCCESS
```

- Le processus se termine avec le code de sortie 0 quand tous les tests ont réussi, 1 quand un test a échoué, 2 quand le serveur s'est arrêté avant l'exécution des tests.
- Un test en échec affiche `[SELFTEST] FAIL <nom> (<n> ticks): <raison>`, et l'exécution se termine par `[SELFTEST] RESULT FAILURE`.
- Une exécution sans la ligne `RESULT SUCCESS` n'est pas une réussite, quoi qu'elle affiche d'autre.
- Le self test démarre un serveur Minecraft, et un serveur ne fonctionne qu'une fois son CLUF (<https://aka.ms/MinecraftEULA>) accepté. Avec un nouveau dossier `run-selftest/`, l'exécution s'arrête et le signale. Créez `run-selftest/eula.txt` contenant `eula=true`, puis relancez la tâche. Dans tous les projets, vous pouvez à la place passer `-PacceptEula` une fois (`./gradlew runSelftest -PacceptEula`), ce qui écrit ce fichier.
- Chaque projet utilise son propre port de serveur (`selftest_port` dans son `gradle.properties`), si bien que plusieurs self tests peuvent tourner en même temps.
- Les tests des intégrations facultatives ne s'exécutent que lorsque l'autre mod est chargé (`-PwithCompatMods`).
- L'exécution d'un seul test par son nom est expliquée dans [docs/BUILDING.md](docs/BUILDING.md).

Un nouveau comportement vérifiable sur un serveur mérite un test dans `common/<mcversion>/src/main/java/com/vyrriox/lauramod/test/LauraTestCases.java`. Un test construit lui-même tout ce dont il a besoin et ne dépend jamais de ce qu'une exécution précédente a laissé dans le monde.

## Vérification des langues

Depuis la racine du dépôt :

```
python tools/check_lang.py --strict
```

Le script vérifie que chaque clé de traduction et chaque clé de dialogue utilisée par le code existe dans chaque langue, que chaque intention obligatoire a un déclencheur, et que les variables `%s` des fichiers d'interface correspondent à l'anglais. Une exécution réussie affiche une ligne par fichier, chacune terminée par `-> OK`, puis la ligne de l'index :

```
[lang] fr_fr.json: 484 keys, 0 missing, 0 unused, 0 placeholder mismatches -> OK
[dialogue] fr_fr.json: 216 line keys, 75 intents, 0 missing lines, 0 missing intents -> OK
[index] 18 languages listed, 0 files not listed, 0 listed without a file
```

- Les nombres grandissent avec le mod. Ce qui compte : `0 missing`, `0 placeholder mismatches`, `0 missing lines`, `0 missing intents` et `-> OK` sur les 18 fichiers des deux sortes.
- Le code de sortie vaut 0 en cas de réussite et 1 en cas d'échec. Sans `--strict`, seuls un fichier anglais incomplet ou un index incorrect font échouer l'exécution.
- `--mc <mcversion>` vérifie le dossier commun d'une autre version de Minecraft (`1.21.1` par défaut).

## Traductions

Il existe deux sortes de textes. Leurs formats, les règles des déclencheurs et les variables sont décrits dans [docs/LANGUAGES.md](docs/LANGUAGES.md).

| Texte | Fichiers |
|---|---|
| Interface : menu, objets, progrès, messages système | `common/<mcversion>/src/main/resources/assets/lauramod/lang/<langue>.json` |
| Dialogues : ce qu'elle dit et ce qu'elle comprend dans le chat | `common/<mcversion>/src/main/resources/data/lauramod/dialogues/<langue>.json` |

Le mod fournit 18 langues, et les 18 doivent rester complètes pour les deux sortes de textes : `cs_cz`, `de_de`, `en_us`, `es_es`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `nl_nl`, `pl_pl`, `pt_br`, `ru_ru`, `sv_se`, `tr_tr`, `uk_ua`, `zh_cn`, `zh_tw`.

**Corriger une traduction**

1. Modifiez la valeur dans le fichier de cette langue. Les clés ne changent pas.
2. Conservez les variables du texte anglais : `%s` et `%1$s` dans les fichiers d'interface, `{player}`, `{item}` et les autres dans les répliques.
3. Appliquez la même modification dans le dossier commun de chaque version de Minecraft présente sur `main`.
4. Lancez la vérification des langues.

Vous pouvez essayer une formulation en jeu sans rien compiler : les deux sortes de fichiers peuvent être surchargées depuis `config/lauramod/`, comme l'explique [docs/LANGUAGES.md](docs/LANGUAGES.md).

**Ajouter un texte**

1. Ajoutez d'abord la clé à `en_us.json` : l'anglais sert de référence.
2. Ajoutez-la aux 17 autres fichiers dans la même pull request. Si vous ne pouvez pas écrire certaines de ces langues, indiquez lesquelles dans la pull request pour qu'elles soient relues.
3. Lancez la vérification des langues : elle liste chaque fichier où la clé manque.

**Ajouter une langue**

1. Créez le fichier d'interface et le fichier de dialogues, tous deux nommés d'après le code de langue de Minecraft.
2. Ajoutez le code à `data/lauramod/dialogues/_index.json`. Le mod ne charge que les fichiers de dialogues intégrés listés dans ce fichier.
3. Donnez sa phrase à l'intention `summon`, et au moins un déclencheur à chaque intention obligatoire. La vérification des langues liste ce qui manque.
4. Mettez à jour la liste des langues de [docs/LANGUAGES.md](docs/LANGUAGES.md).

## Conventions de code

- Le code, les identifiants, les commentaires, les messages de log et les messages de commit sont écrits en anglais.
- Le code commun n'appelle que des méthodes du jeu de base et l'interface `Platform` (paquet `platform`). Une méthode ajoutée par un chargeur n'existe pas sur les autres : `ServerPlayer.getLanguage()`, par exemple, est un patch de NeoForge et de Forge. Ce que seul un chargeur peut fournir devient une méthode de `Platform`, implémentée dans chaque projet de chargeur.
- `common/1.21.1` est la référence. Une modification faite dans ce dossier est reportée dans le dossier commun de chaque autre version de Minecraft présente sur `main`, adaptée là où l'API du jeu diffère.
- Compilez chaque projet de chargeur d'une version de Minecraft avant de considérer comme terminée une modification de son dossier commun.
- Les intégrations facultatives se trouvent dans le paquet `compat` d'un projet de chargeur. Elles sont compilées avec l'API publique de l'autre mod, jamais incluses dans le jar et jamais obligatoires.
- Un nouveau texte affiché aux joueurs ne s'écrit pas dans le code : utilisez une clé de traduction (interface) ou une clé de dialogue (ce qu'elle dit).
- Les skins, les icônes, les textures et les sons sont écrits par `tools/generate_assets.py`. Une modification manuelle d'un fichier généré est perdue à l'exécution suivante : modifiez donc le générateur.
- Conservez les balises `@author` existantes.

## Messages de commit

`type: message descriptif` en anglais. Types : `feat`, `fix`, `docs`, `chore`, `refactor`, `ci`.

Exemple : `fix: pick left and right limbs of custom models from their position`.

## Pull requests

1. Forkez le dépôt et créez une branche depuis `main` : `feat/nom-court` ou `fix/nom-court`.
2. Faites votre modification, et reportez-la dans chaque dossier concerné.
3. Compilez chaque projet touché : `./gradlew build`. Une modification d'un dossier commun touche tous les projets de chargeur de cette version de Minecraft.
4. Lancez les self tests sur chaque chargeur dont vous avez modifié le dossier, et sur au moins un chargeur pour une modification du code commun.
5. Lancez la vérification des langues si vous avez ajouté ou modifié un texte.
6. Testez en jeu ce que vous avez modifié (`./gradlew runClient`).
7. Mettez à jour `CHANGELOG.md` (sections anglaise et française) et le guide de `docs/` qui décrit le comportement (dans les deux langues).
8. Ouvrez la pull request avec le modèle fourni.

Une pull request doit contenir :

- Ce qu'elle change et pourquoi.
- Les cibles qu'elle touche, et celles que vous avez compilées et testées.
- La ligne de résultat des self tests (`[SELFTEST] RESULT SUCCESS`) et celle de la vérification des langues quand un texte a changé.
- L'entrée du changelog et la mise à jour de la documentation, en anglais et en français.
- Les nouveaux textes dans les 18 langues.
- Aucun changement de numéro de version : les versions sont fixées par le mainteneur au moment de la publication.
- Aucune information personnelle, dans les fichiers comme dans les logs collés (adresses IP, jetons, informations de compte).
