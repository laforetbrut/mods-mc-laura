# Changelog

All notable changes to My Girlfriend Laura are documented here.

---

## [2.0.0] - 2026-10-01

### Added

- **Nine targets, one code base**: the repository is laid out for Minecraft 1.20.1, 1.21.1 and 26.1.2, each on NeoForge, Forge and Fabric. Code and resources that do not depend on a loader live in `common/<mcversion>`, and each `<loader>-<mcversion>` folder is a standalone Gradle project that only holds the loader glue. This release is built for the nine targets, and the nine pass the in-game self test suite. Fabric is a new loader for the mod: 1.x shipped for Forge and NeoForge only. See [docs/BUILDING.md](docs/BUILDING.md).
- **Needs**: hunger, energy, fun, attention and hygiene go from 100 (satisfied) to 0. Each one is refilled in its own way (food, sleep, dancing and gifts, talking and hugs, water or rain) and has consequences when it runs low. A right click on her with a water bucket washes her, and the bucket comes back empty. `needs.annoyance` (`CHILL`, `NORMAL`, `NEEDY`, or `UNBEARABLE`, the default) sets how demanding she is, and `needs.enabled` turns them all off. See [docs/ACTIONS.md](docs/ACTIONS.md).
- **Moods and relationship**: ten moods (happy, in love, calm, bored, hungry, tired, sad, angry, jealous, sulking) choose her idle animation and her lines. Affection goes from 0 to 1000 and gives six relationship levels, from "Hates you" to "Soulmates". When she is unhappy she may refuse an order (`needs.refuseOrders`); giving it a second time always works. A hug is never refused because she is hungry, sad or jealous: it comforts her.
- **Desires**: every 12 minutes on average she wishes for an item, a place or an activity, shown in a thought bubble, to fulfil within 15 minutes. The pool is `desires.json`.
- **Gifts and food**: items listed in `gifts.json` are gifts with their own affection, fun and reaction tier, and she may give something back. The `match` of an entry is an item id, an `#item_tag` or the built-in group `@music_disc` (any music disc, with or without mods). Favourite and disliked foods have their own reactions. She eats on her own from her inventory or from a pantry chest.
- **Chat orders**: follow, stay, come, wander, go home, stop, fetch, tasks, jobs, chest assignment, hug, kiss, compliments, apologies, emotes and small talk are understood in plain chat. Each language has 75 intents.
- **Proximity chat**: she hears players, and everybody hears her, within 64 blocks in the same dimension (`dialogue.chatRange`). The summoning phrase works from anywhere.
- **Chained orders**: "bring me wood then come here" starts the first order at once and puts the following ones in her to-do list.
- **Listening per language and spelling rules**: she listens in each player's game language plus English (`dialogue.matchAllLanguages` for every language she knows). Case, accents and punctuation are ignored, and each language can declare `spelling` rules for chat shortcuts and common mistakes. Rules ship for English and French.
- **Fetch orders**: "bring me 16 bread", `/laura fetch <item> [count]` or the Fetch tab. A number written in a chat order is the quantity ("bring me 32 bread"). Within 24 blocks she looks for items on the ground, then in storage, then for blocks of the `lauramod:fetch_harvestable` tag, and brings the items back.
- **Jobs**: lumberjack (fells natural trees, replants a sapling), farmer (harvests, replants, sows, uses bone meal) and cook (furnaces, smokers, campfires, and the meals of `recipes.json` at a crafting table). A job is continuous work in an area around the place where the order is given, 10 blocks of radius by default.
- **Task queue**: a to-do list of 16 tasks (fetch, chop a tree, harvest, cook, come, go home, follow, stay, back to work) that run one after the other.
- **Chests by purpose**: a container can be assigned to `wood`, `harvest`, `seeds`, `ingredients`, `fuel`, `meals`, `pantry` or `storage`. She remembers 48 containers.
- **Several companions**: 3 per player by default (`general.maxPerPlayer`), each bound to the player who summoned her, with `/laura list`, `/laura select`, `/laura where` and selection by name in chat.
- **Gravestone and revival**: with the default `GRAVE` mode a dead companion waits until a flower is laid on a Laura's Gravestone, then comes back with her inventory and her memories. `general.reviveMode` also offers `TIMER` and `NONE`.
- **Laura's Heart**: obtained by smelting a pink tulip in a furnace. The heart summons a first companion, or calls back the ones a player already has.
- **Home, combat modes and pickup**: a home she lives around and sleeps at, passive, defensive and aggressive combat modes, and a switch that makes her collect items lying nearby.
- **Hay gag**: a way to keep her quiet for a while. Right click her with a hay bale, remove it with shears. The settings are in the `gag` section.
- **Skins**: six built-in skins, PNG files of the server's `config/lauramod/skins` folder, player uploads, URL skins limited by a domain whitelist, and the skin of a Minecraft account by name. See [docs/SKINS.md](docs/SKINS.md).
- **Custom Blockbench models**: `.bbmodel` and `.geo.json` models with their own textures and animations, from the server folder, a resource pack or a player upload. Head, body, arm and leg bones are recognised by name in several languages, a model without animations borrows the default ones, and keyframes accept Molang expressions. See [docs/MODELS.md](docs/MODELS.md).
- **Kawaii menu**: key K, or right click with an empty hand. Seven tabs: Home, Orders, Emotes, Work, Fetch, Style, Settings.
- **Emote wheel**: key G, 30 emotes.
- **Inventory screen**: 3 rows of 9 slots by default (`general.inventoryRows`, 1 to 6), four armor slots, both hands and a back slot.
- **Needs overlay**: optional (`display.showNeedsHud`), with the face, name, health and needs of the nearest companion.
- **Speech bubble**: what she says is shown in a bubble above her head as well as in chat, and her current desire in a thought bubble.
- **18 languages**: interface and dialogues in `cs_cz`, `de_de`, `en_us`, `es_es`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `nl_nl`, `pl_pl`, `pt_br`, `ru_ru`, `sv_se`, `tr_tr`, `uk_ua`, `zh_cn` and `zh_tw`. Each dialogue file has 216 line keys.
- **Languages from config files**: `config/lauramod/dialogues/<language>.json` adds or replaces lines, chat triggers, item keywords and spelling rules, and a file for a language code the mod does not ship adds that language. `config/lauramod/lang/<language>.json` overrides interface texts. See [docs/LANGUAGES.md](docs/LANGUAGES.md).
- **JSON config files**: `lauramod-common.json`, `lauramod-client.json`, `gifts.json`, `desires.json` and `recipes.json` in `config/lauramod/`. The two settings files are written with a comment, a range and a default for every option, and `/laura reload` applies server changes. See [docs/CONFIG.md](docs/CONFIG.md).
- **Commands**: a complete `/laura` tree for orders, fetch, tasks, jobs, chests, home, skins, models and administration. See [docs/COMMANDS.md](docs/COMMANDS.md).
- **58 advancements**: in their own tab, with counters kept per player. Going to the Nether or the End with her grants the matching advancement however you get there together, and the bath advancement is also granted when water really washes her, not only when she decides to bathe.
- **Public API and KubeJS**: `com.vyrriox.lauramod.api.LauraAPI` lets other mods add gifts, desires, meals and dialogue, read a companion's state, give orders and listen to eight events. With KubeJS, server scripts get the same functions as the `Laura` binding and the `LauraEvents` event group. See [docs/KUBEJS.md](docs/KUBEJS.md).
- **Optional integrations**: on Minecraft 1.21.1, Applied Energistics 2 (network storage for fetch and jobs), Curios (trinket slots), Farmer's Delight (cooking pots) and KubeJS on NeoForge, and Farmer's Delight Refabricated on Fabric. Forge 1.21.1 has none of these four, which have no Forge 1.21.1 release. On Minecraft 1.20.1, Farmer's Delight (Refabricated on Fabric) and KubeJS on the three loaders; the Applied Energistics 2 bridge is compiled in on the three loaders and the Curios bridge on NeoForge and Forge only, and both have never been run in game on 1.20.1. On Minecraft 26.1.2, Farmer's Delight Refabricated on Fabric only. Storage blocks of other mods are used like vanilla chests, and a backpack worn on her back adds storage when it exposes its inventory: on NeoForge and Forge for every Minecraft version, and on Fabric for 26.1.2. Carry On cannot pick her up. None of these mods is required. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).
- **In-game self tests**: the in-game self test suite (with extra tests for Farmer's Delight and KubeJS when they are loaded) runs on a dedicated server started with `-Dlauramod.selftest=true`, writes `lauramod-selftest.json` and ends the process with the result as exit code. A visual client test (`-Dlauramod.clienttest=true`) opens every screen of the mod and saves a screenshot of each.
- **Documentation**: ten bilingual guides in `docs/` (config, commands, actions, skins, models, languages, KubeJS, compatibility, building, FAQ).

### Changed

- **Complete rewrite**: 2.0.0 is written from scratch. The entries below are what differs for players coming from 1.x.
- **Health**: 100 health points instead of 1000 (`general.maxHealth`), and no free regeneration (1.x healed 1 health per tick). She heals 0.5 health per second while her hunger is 60 or more. Set `general.maxHealth` to 1000 and `general.regenPerSecond` to 20 for the old values.
- **Follow and teleport**: she no longer teleports like a vanilla pet. She walks to her partner beyond 7 blocks, stops at 3.5 blocks, and teleports only beyond 128 blocks (`follow.teleportDistance`, 8 to 1024). Stuck for 10 seconds, she says so and teleports only if `follow.teleportWhenStuck` is true. `/laura come` makes her walk when a path exists, and teleport when none exists or when she is beyond that distance or in another dimension. In follow mode she joins her partner in other dimensions and after any long teleport, and she arrives on a safe spot next to her partner.
- **Single companion limit lifted**: 1.x allowed one Laura for the whole world. Each player can now have 3 companions and the world has no limit by default. `general.maxPerWorld` set to 1 restores the old rule.
- **Config format**: 1.x had no settings file, every value was fixed in the code. Settings now live in JSON files in `config/lauramod/`.
- **License**: MIT instead of All Rights Reserved.
- **Right click**: an empty hand opens her menu instead of making her sit or stand (`controls.menuOnRightClick`). Sneak and right click still opens her inventory.
- **Chat**: in 1.x her lines were private messages to her partner, and she listened within about 10 blocks. She now speaks in proximity chat, heard by every player within 64 blocks.
- **Skin command**: `/laura skin <url>` becomes `/laura skin url <url> [slim]`. Only her partner and operators may change her look unless `skins.othersCanChangeSkin` is true, and the domain must be in `skins.urlDomainWhitelist`.
- **Death**: with the default `GRAVE` mode she waits for a flower on a gravestone. In 1.x the chat phrase simply summoned a new Laura.
- **Inventory**: 27 slots by default instead of 9, plus armor, hand and back slots.
- **Villagers**: they walk away only while she follows her partner nearby, within 5 blocks instead of 10, and not when they sleep or trade (`personality.jealousOfVillagers`).
- **Worlds from 1.x**: a companion saved by 1.x is kept. Her inventory, her URL skin and her sulking state are read from the old data, and she is registered to her partner when she is loaded.

### Fixed

- **Partial word matching in chat**: 1.x searched its triggers anywhere inside a message, so the Spanish "ven" made her follow on "seven" or "souvent", and the "no" of "know" counted as a refusal of her love question. Triggers now match whole words (`dialogue/TextMatcher.java`), a trailing `*` is needed to match the beginning of a word, and the longest trigger wins. Languages written without spaces between words (Chinese and Japanese among the shipped ones) are still matched inside the text.
- **Words of another language**: 1.x tested the triggers of all its languages on every message. She now listens in the player's own language plus English.
- **Summon message always in French**: "Laura est apparue !" was sent as is to the player who summoned her, whatever the language of their game. The summon line now comes from the dialogue file of the player's language.
- **A diamond for every poppy**: in 1.x each poppy given to her dropped a diamond. Flowers are now ordinary gifts of `gifts.json`.
- **Companion lost in unloaded chunks**: 1.x could neither locate nor summon again a Laura left in unloaded chunks. The world now keeps a record and a saved state of every companion: `/laura where` lists them, `/laura come` and Laura's Heart call them back, and a companion that cannot be found is restored from her last saved state.
- **Name always shown as "Laura"**: 1.x ignored name tags. She can now be renamed with `/laura name`, the menu or a name tag.
- **Inventory lost on death**: in 1.x the items she carried disappeared when she died. With `GRAVE` and `TIMER` she keeps them, with `NONE` she drops them.
- **Skin command open to everyone**: in 1.x any player standing within 10 blocks of Laura could change her skin, with any address, and clients downloaded it without a size limit. The server now checks who asks and which domain is used. Clients limit the size (`network.maxSkinDownloadKb`), follow at most 3 redirects and refuse local network addresses.

### Performance

- **Personality on a one second tick**: needs, mood, desires and reactions are computed once per second for each companion, with a random offset per companion so that several companions do not all run on the same tick. Her to-do list is checked every 5 ticks.
- **Throttled scans**: the searches for items to pick up, a playing jukebox, water for a bath and food in storage run at intervals instead of every tick, and a job waits before scanning its work area again when it found nothing to do.
- **Jukebox check**: it reads the block entities of the loaded chunks instead of scanning blocks, and its result is kept for 3 seconds per companion.
- **Chat matching**: triggers are normalised once when the dialogues load and kept in an index sorted from the most to the least specific. For each lookup, a message is normalised once and re-spelled at most once per language.
- **Server upkeep at intervals**: pending recalls are processed every 10 ticks, timed respawns every second, companions left behind every 2 seconds. The record of a companion is refreshed every 5 seconds and her full saved state every 5 minutes.
- **Caches**: smoking results are cached per item, account lookups for player name skins are kept for one hour, and clients keep downloaded skins and models in memory and in `<game folder>/lauramod/cache`. Rescans of the skin and model folders caused by an unknown name are limited to one every 5 seconds.
- **No thread per message**: 1.x started a thread for each chat reply and each skin download. Chat is now handled on the next server tick, and skin downloads share one background thread.

### Ajouts

- **Neuf cibles, une seule base de code**: le dépôt est organisé pour Minecraft 1.20.1, 1.21.1 et 26.1.2, chacun sur NeoForge, Forge et Fabric. Le code et les ressources qui ne dépendent pas d'un loader sont dans `common/<mcversion>`, et chaque dossier `<loader>-<mcversion>` est un projet Gradle autonome qui ne contient que la partie propre au loader. Cette version est compilée pour les neuf cibles, et les neuf réussissent la suite de self tests en jeu. Fabric est un nouveau loader pour le mod : la 1.x n'existait que pour Forge et NeoForge. Voir [docs/BUILDING.md](docs/BUILDING.md).
- **Besoins**: la faim, l'énergie, l'amusement, l'attention et l'hygiène vont de 100 (satisfait) à 0. Chacun se remplit à sa façon (nourriture, sommeil, danse et cadeaux, discussions et câlins, eau ou pluie) et a des conséquences quand il est bas. Un clic droit sur elle avec un seau d'eau la lave, et le seau revient vide. `needs.annoyance` (`CHILL`, `NORMAL`, `NEEDY`, ou `UNBEARABLE`, la valeur par défaut) règle son niveau d'exigence, et `needs.enabled` les désactive tous. Voir [docs/ACTIONS.md](docs/ACTIONS.md).
- **Humeurs et relation**: dix humeurs (heureuse, amoureuse, calme, s'ennuie, affamée, fatiguée, triste, en colère, jalouse, boude) choisissent son animation de repos et ses répliques. L'affection va de 0 à 1000 et donne six niveaux de relation, de « Te déteste » à « Âmes sœurs ». Quand elle est mécontente elle peut refuser un ordre (`needs.refuseOrders`) ; le donner une seconde fois marche toujours. Un câlin n'est jamais refusé parce qu'elle a faim, qu'elle est triste ou jalouse : il la réconforte.
- **Désirs**: toutes les 12 minutes en moyenne elle désire un objet, un lieu ou une activité, affiché dans une bulle de pensée, à satisfaire dans les 15 minutes. La liste se trouve dans `desires.json`.
- **Cadeaux et nourriture**: les objets listés dans `gifts.json` sont des cadeaux avec leur affection, leur amusement et leur niveau de réaction, et elle peut offrir quelque chose en retour. Le `match` d'une entrée est un identifiant d'objet, un `#tag_d_objets` ou le groupe intégré `@music_disc` (n'importe quel disque de musique, avec ou sans mods). Les aliments préférés et détestés ont leurs propres réactions. Elle mange seule dans son inventaire ou dans un coffre garde-manger.
- **Ordres dans le chat**: suivre, rester, venir, se promener, rentrer, stop, rapporter, tâches, métiers, attribution de coffres, câlin, bisou, compliments, excuses, émotes et bavardage sont compris en clair dans le chat. Chaque langue a 75 intentions.
- **Chat de proximité**: elle entend les joueurs, et tout le monde l'entend, dans un rayon de 64 blocs dans la même dimension (`dialogue.chatRange`). La phrase d'invocation fonctionne de partout.
- **Ordres enchaînés**: « apporte-moi du bois puis viens ici » lance le premier ordre tout de suite et place les suivants dans sa liste de tâches.
- **Écoute par langue et règles d'orthographe**: elle écoute dans la langue du jeu de chaque joueur plus l'anglais (`dialogue.matchAllLanguages` pour toutes les langues qu'elle connaît). La casse, les accents et la ponctuation sont ignorés, et chaque langue peut déclarer des règles `spelling` pour les abréviations du chat et les fautes courantes. Des règles sont fournies pour l'anglais et le français.
- **Ordres de rapport**: « apporte-moi 16 pains », `/laura fetch <item> [count]` ou l'onglet Chercher. Un nombre écrit dans un ordre du chat est la quantité (« apporte-moi 32 pains »). Dans un rayon de 24 blocs elle cherche les objets au sol, puis dans les rangements, puis les blocs du tag `lauramod:fetch_harvestable`, et rapporte les objets.
- **Métiers**: bûcheronne (abat les arbres naturels, replante une pousse), fermière (récolte, replante, sème, utilise la poudre d'os) et cuisinière (fours, fumoirs, feux de camp, et les plats de `recipes.json` sur un établi). Un métier est un travail continu dans une zone autour de l'endroit où l'ordre est donné, de 10 blocs de rayon par défaut.
- **File de tâches**: une liste de 16 tâches (rapporter, couper un arbre, récolter, cuisiner, venir, rentrer, suivre, rester, retour au travail) exécutées l'une après l'autre.
- **Coffres par usage**: un conteneur peut être attribué à `wood`, `harvest`, `seeds`, `ingredients`, `fuel`, `meals`, `pantry` ou `storage`. Elle retient 48 conteneurs.
- **Plusieurs compagnes**: 3 par joueur par défaut (`general.maxPerPlayer`), chacune liée au joueur qui l'a invoquée, avec `/laura list`, `/laura select`, `/laura where` et la sélection par le nom dans le chat.
- **Tombe et retour à la vie**: avec le mode `GRAVE` par défaut, une compagne morte attend qu'une fleur soit déposée sur une Tombe de Laura, puis revient avec son inventaire et ses souvenirs. `general.reviveMode` propose aussi `TIMER` et `NONE`.
- **Cœur de Laura**: il s'obtient en faisant cuire une tulipe rose dans un four. Le cœur invoque une première compagne, ou rappelle celles qu'un joueur a déjà.
- **Maison, modes de combat et ramassage**: une maison autour de laquelle elle vit et où elle dort, les modes de combat passif, défensif et agressif, et un interrupteur qui lui fait ramasser les objets au sol à proximité.
- **Bâillon de foin**: un moyen de la faire taire un moment. Clic droit sur elle avec une botte de foin, des cisailles pour le retirer. Les réglages sont dans la section `gag`.
- **Skins**: six skins intégrés, les fichiers PNG du dossier `config/lauramod/skins` du serveur, les envois des joueurs, les skins par URL limités par une liste blanche de domaines, et le skin d'un compte Minecraft par son nom. Voir [docs/SKINS.md](docs/SKINS.md).
- **Modèles Blockbench personnalisés**: des modèles `.bbmodel` et `.geo.json` avec leurs textures et leurs animations, depuis le dossier du serveur, un pack de ressources ou l'envoi d'un joueur. Les os de la tête, du corps, des bras et des jambes sont reconnus par leur nom dans plusieurs langues, un modèle sans animation emprunte les animations par défaut, et les images clés acceptent des expressions Molang. Voir [docs/MODELS.md](docs/MODELS.md).
- **Menu kawaii**: touche K, ou clic droit à main vide. Sept onglets : Accueil, Ordres, Émotes, Travail, Chercher, Style, Réglages.
- **Roue des émotes**: touche G, 30 émotes.
- **Écran d'inventaire**: 3 rangées de 9 emplacements par défaut (`general.inventoryRows`, de 1 à 6), quatre emplacements d'armure, les deux mains et un emplacement de dos.
- **Cadre des besoins**: en option (`display.showNeedsHud`), avec le visage, le nom, la vie et les besoins de la compagne la plus proche.
- **Bulle de dialogue**: ce qu'elle dit s'affiche dans une bulle au-dessus de sa tête en plus du chat, et son désir du moment dans une bulle de pensée.
- **18 langues**: l'interface et les dialogues en `cs_cz`, `de_de`, `en_us`, `es_es`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `nl_nl`, `pl_pl`, `pt_br`, `ru_ru`, `sv_se`, `tr_tr`, `uk_ua`, `zh_cn` et `zh_tw`. Chaque fichier de dialogue compte 216 clés de répliques.
- **Langues par fichiers de configuration**: `config/lauramod/dialogues/<langue>.json` ajoute ou remplace des répliques, des déclencheurs du chat, des mots-clés d'objets et des règles d'orthographe, et un fichier pour un code de langue que le mod ne fournit pas ajoute cette langue. `config/lauramod/lang/<langue>.json` remplace des textes de l'interface. Voir [docs/LANGUAGES.md](docs/LANGUAGES.md).
- **Fichiers de configuration JSON**: `lauramod-common.json`, `lauramod-client.json`, `gifts.json`, `desires.json` et `recipes.json` dans `config/lauramod/`. Les deux fichiers de réglages sont écrits avec un commentaire, une plage et une valeur par défaut pour chaque option, et `/laura reload` applique les changements du serveur. Voir [docs/CONFIG.md](docs/CONFIG.md).
- **Commandes**: une arborescence `/laura` complète pour les ordres, le rapport d'objets, les tâches, les métiers, les coffres, la maison, les skins, les modèles et l'administration. Voir [docs/COMMANDS.md](docs/COMMANDS.md).
- **58 progrès**: dans leur propre onglet, avec des compteurs conservés par joueur. Aller dans le Nether ou l'End avec elle donne le progrès correspondant, quelle que soit la façon dont vous y arrivez ensemble, et le progrès du bain (« Toute propre ») est aussi accordé quand de l'eau la lave vraiment, pas seulement quand elle décide de se baigner.
- **API publique et KubeJS**: `com.vyrriox.lauramod.api.LauraAPI` permet aux autres mods d'ajouter des cadeaux, des désirs, des plats et des dialogues, de lire l'état d'une compagne, de donner des ordres et d'écouter huit événements. Avec KubeJS, les scripts serveur disposent des mêmes fonctions par la liaison `Laura` et le groupe d'événements `LauraEvents`. Voir [docs/KUBEJS.md](docs/KUBEJS.md).
- **Intégrations optionnelles**: sur Minecraft 1.21.1, Applied Energistics 2 (stockage du réseau pour le rapport d'objets et les métiers), Curios (emplacements de bijoux), Farmer's Delight (marmites) et KubeJS sur NeoForge, et Farmer's Delight Refabricated sur Fabric. Forge 1.21.1 n'a aucune de ces quatre intégrations, ces mods n'ayant pas de version Forge 1.21.1. Sur Minecraft 1.20.1, Farmer's Delight (Refabricated sur Fabric) et KubeJS sur les trois loaders ; le pont Applied Energistics 2 est compilé sur les trois loaders et le pont Curios sur NeoForge et Forge seulement, et aucun des deux n'a jamais été lancé en jeu sur la 1.20.1. Sur Minecraft 26.1.2, Farmer's Delight Refabricated sur Fabric uniquement. Les blocs de rangement des autres mods s'utilisent comme les coffres du jeu de base, et un sac à dos porté sur son dos ajoute du rangement quand il expose son inventaire : sur NeoForge et Forge pour toutes les versions de Minecraft, et sur Fabric pour la 26.1.2. Carry On ne peut pas la porter. Aucun de ces mods n'est requis. Voir [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).
- **Tests automatiques en jeu**: la suite de self tests en jeu (avec des tests supplémentaires pour Farmer's Delight et KubeJS quand ils sont chargés) tourne sur un serveur dédié lancé avec `-Dlauramod.selftest=true`, écrit `lauramod-selftest.json` et termine le processus avec le résultat comme code de sortie. Un test visuel du client (`-Dlauramod.clienttest=true`) ouvre chaque écran du mod et enregistre une capture de chacun.
- **Documentation**: dix guides bilingues dans `docs/` (configuration, commandes, actions, skins, modèles, langues, KubeJS, compatibilité, compilation, FAQ).

### Modifications

- **Réécriture complète**: la 2.0.0 est écrite à partir de zéro. Les entrées ci-dessous sont ce qui change pour les joueurs venant de la 1.x.
- **Vie**: 100 points de vie au lieu de 1000 (`general.maxHealth`), et plus de régénération gratuite (la 1.x soignait 1 point de vie par tick). Elle récupère 0,5 point de vie par seconde tant que sa faim est à 60 ou plus. Mettez `general.maxHealth` à 1000 et `general.regenPerSecond` à 20 pour retrouver les anciennes valeurs.
- **Suivi et téléportation**: elle ne se téléporte plus comme un animal apprivoisé du jeu de base. Elle marche vers son partenaire au-delà de 7 blocs, s'arrête à 3,5 blocs, et ne se téléporte qu'au-delà de 128 blocs (`follow.teleportDistance`, de 8 à 1024). Coincée pendant 10 secondes, elle le dit et ne se téléporte que si `follow.teleportWhenStuck` est activé. `/laura come` la fait marcher quand un chemin existe, et se téléporter quand il n'y en a pas, quand elle est au-delà de cette distance ou dans une autre dimension. En mode suivi elle rejoint son partenaire dans les autres dimensions et après toute téléportation lointaine, et elle arrive à un endroit sûr à côté de lui.
- **Fin de la limite à une seule compagne**: la 1.x autorisait une seule Laura pour tout le monde. Chaque joueur peut maintenant avoir 3 compagnes et le monde n'a pas de limite par défaut. `general.maxPerWorld` à 1 rétablit l'ancienne règle.
- **Format de configuration**: la 1.x n'avait pas de fichier de réglages, chaque valeur était fixée dans le code. Les réglages sont maintenant dans des fichiers JSON de `config/lauramod/`.
- **Licence**: MIT au lieu de All Rights Reserved.
- **Clic droit**: la main vide ouvre son menu au lieu de la faire s'asseoir ou se lever (`controls.menuOnRightClick`). S'accroupir et faire un clic droit ouvre toujours son inventaire.
- **Chat**: dans la 1.x ses répliques étaient des messages privés à son partenaire, et elle écoutait dans un rayon d'environ 10 blocs. Elle parle maintenant dans le chat de proximité, entendue par tous les joueurs à moins de 64 blocs.
- **Commande de skin**: `/laura skin <url>` devient `/laura skin url <url> [slim]`. Seuls son partenaire et les opérateurs peuvent changer son apparence, sauf si `skins.othersCanChangeSkin` est activé, et le domaine doit figurer dans `skins.urlDomainWhitelist`.
- **Mort**: avec le mode `GRAVE` par défaut elle attend une fleur sur une tombe. Dans la 1.x la phrase du chat invoquait simplement une nouvelle Laura.
- **Inventaire**: 27 emplacements par défaut au lieu de 9, plus les emplacements d'armure, de mains et de dos.
- **Villageois**: ils ne s'éloignent que lorsqu'elle suit son partenaire de près, à moins de 5 blocs au lieu de 10, et pas quand ils dorment ou commercent (`personality.jealousOfVillagers`).
- **Mondes de la 1.x**: une compagne enregistrée par la 1.x est conservée. Son inventaire, son skin par URL et sa bouderie sont lus dans les anciennes données, et elle est rattachée à son partenaire quand elle est chargée.

### Correctifs

- **Correspondance partielle des mots dans le chat**: la 1.x cherchait ses déclencheurs n'importe où dans un message, si bien que l'espagnol « ven » la faisait suivre sur « seven » ou « souvent », et que le « no » de « know » comptait comme un refus à sa question d'amour. Les déclencheurs correspondent maintenant à des mots entiers (`dialogue/TextMatcher.java`), un `*` final est nécessaire pour correspondre au début d'un mot, et le déclencheur le plus long l'emporte. Les langues écrites sans espaces entre les mots (le chinois et le japonais parmi celles fournies) restent recherchées à l'intérieur du texte.
- **Mots d'une autre langue**: la 1.x testait les déclencheurs de toutes ses langues sur chaque message. Elle écoute maintenant dans la langue du joueur plus l'anglais.
- **Message d'invocation toujours en français**: « Laura est apparue ! » était envoyé tel quel au joueur qui l'invoquait, quelle que soit la langue de son jeu. La réplique d'invocation vient maintenant du fichier de dialogue de la langue du joueur.
- **Un diamant pour chaque coquelicot**: dans la 1.x chaque coquelicot offert lâchait un diamant. Les fleurs sont maintenant des cadeaux ordinaires de `gifts.json`.
- **Compagne perdue dans des chunks déchargés**: la 1.x ne pouvait ni localiser ni invoquer de nouveau une Laura restée dans des chunks déchargés. Le monde conserve maintenant une fiche et un état sauvegardé de chaque compagne : `/laura where` les liste, `/laura come` et le Cœur de Laura les rappellent, et une compagne introuvable est restaurée depuis son dernier état sauvegardé.
- **Nom toujours affiché « Laura »**: la 1.x ignorait les étiquettes de nom. Elle peut maintenant être renommée avec `/laura name`, le menu ou une étiquette de nom.
- **Inventaire perdu à la mort**: dans la 1.x les objets qu'elle portait disparaissaient à sa mort. Avec `GRAVE` et `TIMER` elle les garde, avec `NONE` elle les lâche.
- **Commande de skin ouverte à tous**: dans la 1.x n'importe quel joueur situé à moins de 10 blocs de Laura pouvait changer son skin, avec n'importe quelle adresse, et les clients le téléchargeaient sans limite de taille. Le serveur vérifie maintenant qui demande et quel domaine est utilisé. Les clients limitent la taille (`network.maxSkinDownloadKb`), suivent au plus 3 redirections et refusent les adresses du réseau local.

### Performance

- **Personnalité sur un tick d'une seconde**: les besoins, l'humeur, les désirs et les réactions sont calculés une fois par seconde pour chaque compagne, avec un décalage aléatoire par compagne pour que plusieurs compagnes ne tournent pas toutes sur le même tick. Sa liste de tâches est vérifiée tous les 5 ticks.
- **Recherches espacées**: les recherches d'objets à ramasser, d'un jukebox en marche, d'eau pour un bain et de nourriture dans les rangements se font à intervalles au lieu de chaque tick, et un métier attend avant de parcourir de nouveau sa zone de travail quand il n'a rien trouvé à faire.
- **Détection du jukebox**: elle lit les entités de bloc des chunks chargés au lieu de parcourir les blocs, et son résultat est gardé 3 secondes par compagne.
- **Correspondance du chat**: les déclencheurs sont normalisés une seule fois au chargement des dialogues et gardés dans un index trié du plus précis au moins précis. À chaque recherche, un message est normalisé une fois et réécrit au plus une fois par langue.
- **Entretien du serveur à intervalles**: les rappels en attente sont traités tous les 10 ticks, les retours différés chaque seconde, les compagnes laissées en arrière toutes les 2 secondes. La fiche d'une compagne est rafraîchie toutes les 5 secondes et son état sauvegardé complet toutes les 5 minutes.
- **Caches**: les résultats de fumage sont mis en cache par objet, les recherches de compte pour les skins par nom de joueur sont gardées une heure, et les clients gardent les skins et les modèles téléchargés en mémoire et dans `<dossier du jeu>/lauramod/cache`. Les nouvelles analyses des dossiers de skins et de modèles provoquées par un nom inconnu sont limitées à une toutes les 5 secondes.
- **Plus de thread par message**: la 1.x lançait un thread pour chaque réponse du chat et chaque téléchargement de skin. Le chat est maintenant traité au tick serveur suivant, et les téléchargements de skins partagent un seul thread d'arrière-plan.

---

## [1.1.0] - 2026-02-20

### Added / Ajouté
- **Massive Interaction Database**: Over 500+ unique localized interactions across 11 categories (Greetings, Love, Philosophy, Combat, etc.).
- **6-Block Follow Distance**: Optimized AI to stay at a "partner" distance (approx. 6 blocks) instead of standing on top of the player.
- **Stuck Detection**: Laura now complains in chat if she's stuck or cannot reach the player for more than 10 seconds.
- **Hourly Playful Interaction**: Every hour, Laura playfully hits the player and apologizes with localized dialogue.
- **Portal Support**: Laura can now follow the player through Nether and End portals.
- **Dimension Uniqueness**: Improved `LauraWorldData` ensures only one Laura exists globally across all dimensions.
- **Player Binding**: Laura is strictly bound to her summoner.
- **Anti-Corruption**: Audited NBT persistence to prevent data loss or state corruption.
- **Multilingual Excellence**: Full support for French, English, German, Spanish, Italian, and Portuguese.
- **A.I Optimization**: AI Logic optimized to run conditional checks once per second (using tick modulo), drastically improving server performance.
- **Visual Effects**: Added distinct particles when Laura spawns, takes damage while sad/angry, and when she farts.
- **Chat Formatting**: Laura's chat name is now beautifully formatted in pink.
- **Bug Fixes**: Resolved critical `mods.toml` parsing crash on NeoForge 1.21.1 and fixed missing dependencies for Forge 1.20.1.

---

- **Base d'Interactions Massive**: Plus de 500 interactions localisées uniques à travers 11 catégories.
- **Distance de Suivi de 6 Blocs**: IA optimisée pour maintenir une distance de "partenaire".
- **Détection de Blocage**: Laura se plaint si elle est coincée pendant plus de 10 secondes.
- **Interaction Ludique Horaire**: Toutes les heures, Laura frappe joyeusement le joueur et s'excuse.
- **Support des Portails**: Laura peut maintenant suivre le joueur à travers les portails du Nether et de l'End.
- **Unicité Dimensionnelle**: `LauraWorldData` garantit une seule Laura globalement.
- **Liaison au Joueur**: Laura est liée strictement à son invocateur.
- **Anti-Corruption**: Audit de la persistance NBT.
- **Excellence Multilingue**: Support complet (FR, EN, DE, ES, IT, PT).
- **Optimisation de l'IA**: La logique globale de l'IA a été optimisée grâce à un système de modulo limitant les opérations lourdes à 1 fois par seconde, améliorant drastiquement les performances serveur.
- **Effets Visuels**: Ajout de particules visuelles distinctes lors de l'apparition de l'entité, de blessures (triste/en colère), et lors de flatulences.
- **Pseudo Rose**: Le nom de Laura s'affiche désormais nativement en rose (`<§dLaura§r>`) dans le tchat.
- **Correctifs de Bugs**: Résolution du crash critique de démarrage sur NeoForge 1.21.1 (TOML config) et réparation de la compilation Forge 1.20.1.

## [1.0.0] - 2026-02-16

### Added / Ajouté
- Initial Release with 1000 HP, Regeneration, Inventory, and Social logic.
- 6 Languages support (Basic).
- Inventory System (9 slots).
