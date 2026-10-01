# Commands

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [ACTIONS.md](ACTIONS.md), [CONFIG.md](CONFIG.md), [SKINS.md](SKINS.md), [MODELS.md](MODELS.md).

---

## English

### General rules

- Everything is under `/laura`. `/laura` alone, or `/laura help`, prints a short reminder.
- Every command is open to all players, except `/laura reload` and `/laura admin` (see [Permissions](#permissions)).
- A command acts on one of **your own** companions: the selected one if she is within 96 blocks in your dimension, otherwise the nearest one within 96 blocks. If none is close enough the command answers that she is not near.
- `/laura come` and `/laura home` (or `/laura home go`) are the exceptions: they reach the selected companion (or the nearest loaded one) at any distance and in any dimension. An order sent from out of earshot is never refused, and what she answers when she goes home or cannot come reaches you even there. These two commands return 0 when she refuses, which a command block or a script can test.
- Select a companion with `/laura select <name>`, with the clickable `/laura list`, or simply by writing her name in chat.
- Item ids, tags, file names and internet addresses are typed as they are, without quotes: `/laura fetch minecraft:bread 3`, `/laura fetch #minecraft:logs 16 queue`. A plain item name works too: `/laura fetch bread` means `minecraft:bread`.

### Companions

| Command | Effect |
|---|---|
| `/laura summon` | A dismissed companion comes back first. Otherwise a new companion is summoned, up to `general.maxPerPlayer`. When you already have the maximum, all your companions are called to you. |
| `/laura dismiss` | She leaves the world and loses 20 affection (`general.dismissAffectionPenalty`). Her data is kept and the next `/laura summon` brings her back. |
| `/laura release` | Asks you to confirm. |
| `/laura release confirm` | She leaves for good: she drops everything she carries and her record is deleted. Nothing brings her back. |
| `/laura list` | Lists your companions with a clickable select button. The selected one is marked with `>`. |
| `/laura select <name>` | Chooses which companion answers your commands. |
| `/laura where` | Position, dimension and state of each companion: here, far away, at her grave, coming back, dismissed. The position is refreshed every second while she is active, at once after a teleport or a change of dimension, and when her chunk unloads. |
| `/laura name <name>` | Renames her. 1 to 32 characters. |
| `/laura info` | Mood, relationship, health, needs, current desire, mode, jobs, to-do list, home and days together. |
| `/laura inventory` | Opens her inventory. |
| `/laura desire` | She tells you what she currently wants. |
| `/laura lang` | Shows your language, the dialogue search order, the available dialogue languages and the one she uses with you. |

### Orders

| Command | Effect |
|---|---|
| `/laura follow` | She follows you. |
| `/laura stay` | She sits and waits where she is. |
| `/laura wander` | She walks around freely near where she was left. |
| `/laura come` | She comes to you. Further than `follow.teleportDistance`, in another dimension, or when no path exists, she teleports to a safe spot next to you: at the surface of the water when you swim, on the ground below you when you fly. When there is no safe spot (the void, lava) she says so and stays where she is. If none of your companions is loaded, the ones you already have are called back: teleported, recalled from unloaded chunks, or returned from dismissal. A new companion is created only when you have none. |
| `/laura stop` | Cancels the fetch in progress, clears the to-do list and stops the current animation. |
| `/laura sleep` | She goes to sleep in a free bed within 8 blocks, or on the floor. She refuses during the day when her energy is above 85. A sleep you order lasts until she is rested: daylight alone does not wake her. |
| `/laura wakeup` | Wakes her up (costs 2 affection). |
| `/laura eat` | She eats the first suitable food of her inventory. |
| `/laura hug` | Hug. Never refused because she is hungry, sad or jealous: it comforts her. She only pushes it away while she sulks or is still angry, and asking again within 20 seconds gets through. |
| `/laura kiss` | Kiss. Refused when she is gagged, when she sulks or is still angry, or when affection is under 200. |
| `/laura compliment` | Compliment. |
| `/laura ungag` | Removes the hay gag. |
| `/laura emote <emote>` | Plays an emote. See the list in [ACTIONS.md](ACTIONS.md). The `hug` and `kiss` emotes are a real hug and a real kiss, with the same rules. |
| `/laura mode passive` / `defensive` / `aggressive` | Sets her combat mode. |
| `/laura pickup on` / `off` | She collects items lying within 8 blocks into her inventory. |
| `/laura answer yes` / `no` | Answers her "Do you still love me?" question. |

### Home

| Command | Effect |
|---|---|
| `/laura home set` | Her home becomes the block where you stand, in your dimension. |
| `/laura home` or `/laura home go` | Sends her home, from anywhere. From further than `home.teleportDistance` or from another dimension she teleports to a safe spot next to her home; when there is none she says so and stays where she is. When none of your companions is loaded, the command only answers that you have no companion: call her first with `/laura come`. |
| `/laura home clear` | Forgets the home. If she was in home mode she follows you again. |

### Fetch

| Command | Effect |
|---|---|
| `/laura fetch <item>` | She brings you the item. Without a count she brings up to one stack, 16 at most. |
| `/laura fetch <item> <count>` | Count from 1 to 576, limited by `fetch.maxItems`. |
| `/laura fetch <item> [count] queue` | Adds the request to her to-do list instead of starting now. |

`<item>` can be an item id (`bread`, `minecraft:oak_log`), an item tag (`#minecraft:logs`), `held` (the item in your main hand), or words known by the dialogue files (`some wood`). The count and the word `queue` are read from the end of the line. Examples: `/laura fetch minecraft:bread 3`, `/laura fetch #minecraft:logs 16 queue`, `/laura fetch held`.

The same order can be written in chat, where a number in the message is the quantity: "bring me 32 bread" does what `/laura fetch bread 32` does.

### Tasks and to-do list

A task is done once and the result is brought back to you.

| Command | Effect |
|---|---|
| `/laura chop [radius]` | Fell one tree around you. |
| `/laura harvest [radius]` | Harvest the ripe crops around you once. |
| `/laura cook [radius]` | Cook what she can once. |
| `/laura queue list` | Shows the current task and the waiting ones. |
| `/laura queue add chop` / `harvest` / `cook` / `come` / `home` / `follow` / `stay` / `work` | Adds a step at the end of the list. |
| `/laura queue remove <index>` | Removes a waiting task. Index from 1 to 16. |
| `/laura queue clear` | Empties the list and stops the current task. |

`radius` goes from 3 to 64 and is limited by `work.maxRadius`. Without it, `work.defaultRadius` is used. The to-do list holds 16 tasks.

### Jobs

A job is continuous work in an area centered on the place where you stand when you give the order.

| Command | Effect |
|---|---|
| `/laura job lumberjack [radius]` | Starts the lumberjack job here. |
| `/laura job farmer [radius]` | Starts the farmer job here. |
| `/laura job cook [radius]` | Starts the cook job here. |
| `/laura job stop` | Stops every job. |
| `/laura job stop lumberjack` / `farmer` / `cook` | Stops one job. |
| `/laura job list` | Lists active jobs with their area. |
| `/laura work` | Sends her back to her jobs after another order. |

### Chests

Look at a container (6 blocks at most), then:

| Command | Effect |
|---|---|
| `/laura chest wood` / `harvest` / `seeds` / `ingredients` / `fuel` / `meals` / `pantry` / `storage` | Assigns the container to that purpose. |
| `/laura chest remove` | Forgets the container you look at. |
| `/laura chest clear` | Forgets every container. |
| `/laura chest list` | Lists assigned containers. |

### Look

| Command | Effect |
|---|---|
| `/laura skin list` | Built-in skins and skin files of the server. |
| `/laura skin builtin <name>` | `laura`, `laura_summer`, `laura_winter`, `laura_night`, `laura_sporty`, `laura_gothic`. |
| `/laura skin file <name> [slim]` | A file of the server's `config/lauramod/skins` folder, without `.png`. Add `true` or `false` at the end for slim arms (`true` by default). |
| `/laura skin player <name>` | The skin of a Minecraft account. |
| `/laura skin url <url> [slim]` | A skin from an internet address. Add `true` or `false` at the end for slim arms (`false` by default). |
| `/laura skin reset` | Applies `skins.defaultSkin`. |
| `/laura model list` | Models of the server. |
| `/laura model <name>` | Applies a model. The name is typed as it is, for example `/laura model maid`. |
| `/laura model reset` | Applies `models.defaultModel` (the default player-like model when it is empty). |

### Permissions

| Command | Requirement |
|---|---|
| `/laura summon` and the chat summon | `summoning.summonPermissionLevel` (0 by default: everyone). |
| `/laura reload` | `permissions.reloadPermissionLevel` (2 by default). |
| `/laura admin ...` | `permissions.reloadPermissionLevel` (2 by default). |

### Administration

| Command | Effect |
|---|---|
| `/laura reload` | Reloads the common config, gifts, desires, recipes, dialogues, and rescans the skin and model folders. |
| `/laura admin list` | Every companion of the world: owner, name, state (`active`, `dismissed`, `grave`, `respawning`), position, dimension. |
| `/laura admin remove <player>` | Deletes every companion of that player for good. A loaded companion leaves what she carries on the ground; one in an unloaded chunk is removed, and drops her things, as soon as that chunk loads. What a dismissed companion or one waiting on a grave carried is deleted with her. |
| `/laura admin affection <player> <value>` | Sets the affection (0 to 1000) of that player's companion. |
| `/laura admin need <player> <need> <value>` | Sets a need (0 to 100). Needs: `hunger`, `energy`, `fun`, `attention`, `hygiene`. |
| `/laura admin desire <player> clear` | Removes her current desire. |
| `/laura admin desire <player> roll` | Gives her a new random desire now. |
| `/laura admin emote <player> <emote>` | Plays any emote on that player's companion. |

Admin commands target the selected companion of the player, or the nearest loaded one.

### Key bindings

| Action | Default key | Effect |
|---|---|---|
| Open Laura's menu | K | Opens the menu of the nearest companion within 64 blocks. |
| Emote wheel | G | Opens the emote wheel for the nearest companion within 32 blocks. |
| Call Laura | not bound | Sends `/laura come`. |

### Worked example

```
/laura summon
/laura name Jade
/laura home set
/laura job farmer 12
/laura chest harvest
/laura fetch #minecraft:logs 32
/laura queue add cook
/laura queue add home
```

She is renamed, gets a home, works a field of radius 12, stores crops in the chest you look at, brings 32 logs, then cooks once and goes home.

---

## Français

### Règles générales

- Tout passe par `/laura`. `/laura` seul, ou `/laura help`, affiche un court rappel.
- Toutes les commandes sont ouvertes à tous les joueurs, sauf `/laura reload` et `/laura admin` (voir [Permissions](#permissions-1)).
- Une commande agit sur l'une de **vos** compagnes : celle qui est sélectionnée si elle est à moins de 96 blocs dans votre dimension, sinon la plus proche à moins de 96 blocs. Si aucune n'est assez près, la commande répond qu'elle n'est pas à proximité.
- `/laura come` et `/laura home` (ou `/laura home go`) font exception : elles atteignent la compagne sélectionnée (ou la plus proche chargée) à n'importe quelle distance et dans n'importe quelle dimension. Un ordre envoyé hors de portée de voix n'est jamais refusé, et ce qu'elle répond quand elle rentre à la maison ou ne peut pas venir vous parvient même là. Ces deux commandes renvoient 0 quand elle refuse, ce qu'un bloc de commande ou un script peut tester.
- Sélectionnez une compagne avec `/laura select <nom>`, avec la liste cliquable de `/laura list`, ou simplement en écrivant son nom dans le chat.
- Les identifiants d'objets, les tags, les noms de fichiers et les adresses internet s'écrivent tels quels, sans guillemets : `/laura fetch minecraft:bread 3`, `/laura fetch #minecraft:logs 16 queue`. Un nom d'objet simple fonctionne aussi : `/laura fetch bread` signifie `minecraft:bread`.

### Compagnes

| Commande | Effet |
|---|---|
| `/laura summon` | Une compagne congédiée revient en premier. Sinon une nouvelle compagne est invoquée, jusqu'à `general.maxPerPlayer`. Quand vous avez déjà le maximum, toutes vos compagnes sont appelées. |
| `/laura dismiss` | Elle quitte le monde et perd 20 points d'affection (`general.dismissAffectionPenalty`). Ses données sont conservées et le prochain `/laura summon` la ramène. |
| `/laura release` | Vous demande de confirmer. |
| `/laura release confirm` | Elle part pour de bon : elle lâche tout ce qu'elle porte et sa fiche est supprimée. Rien ne la ramène. |
| `/laura list` | Liste vos compagnes avec un bouton de sélection cliquable. La sélectionnée est marquée par `>`. |
| `/laura select <nom>` | Choisit la compagne qui répond à vos commandes. |
| `/laura where` | Position, dimension et état de chaque compagne : ici, loin, sur sa tombe, de retour bientôt, congédiée. La position est rafraîchie chaque seconde tant qu'elle est active, aussitôt après une téléportation ou un changement de dimension, et quand son chunk se décharge. |
| `/laura name <nom>` | La renomme. De 1 à 32 caractères. |
| `/laura info` | Humeur, relation, vie, besoins, désir en cours, mode, métiers, liste de tâches, maison et jours passés ensemble. |
| `/laura inventory` | Ouvre son inventaire. |
| `/laura desire` | Elle vous dit ce qu'elle veut en ce moment. |
| `/laura lang` | Affiche votre langue, l'ordre de recherche des dialogues, les langues de dialogue disponibles et celle qu'elle utilise avec vous. |

### Ordres

| Commande | Effet |
|---|---|
| `/laura follow` | Elle vous suit. |
| `/laura stay` | Elle s'assoit et attend sur place. |
| `/laura wander` | Elle se promène librement autour de l'endroit où vous l'avez laissée. |
| `/laura come` | Elle vient à vous. Au-delà de `follow.teleportDistance`, dans une autre dimension, ou quand aucun chemin n'existe, elle se téléporte à un endroit sûr à côté de vous : à la surface de l'eau quand vous nagez, au sol sous vous quand vous volez. Quand il n'y a aucun endroit sûr (le vide, la lave) elle le dit et reste où elle est. Si aucune de vos compagnes n'est chargée, celles que vous avez déjà sont rappelées : téléportées, rappelées depuis des chunks déchargés, ou de retour après avoir été congédiées. Une nouvelle compagne n'est créée que si vous n'en avez aucune. |
| `/laura stop` | Annule la recherche en cours, vide la liste de tâches et arrête l'animation en cours. |
| `/laura sleep` | Elle va dormir dans un lit libre à moins de 8 blocs, ou par terre. Elle refuse en journée quand son énergie dépasse 85. Un sommeil que vous ordonnez dure jusqu'à ce qu'elle soit reposée : le jour seul ne la réveille pas. |
| `/laura wakeup` | La réveille (coûte 2 points d'affection). |
| `/laura eat` | Elle mange le premier aliment convenable de son inventaire. |
| `/laura hug` | Câlin. Jamais refusé parce qu'elle a faim, qu'elle est triste ou jalouse : il la réconforte. Elle ne le repousse que lorsqu'elle boude ou est encore en colère, et le redemander dans les 20 secondes passe. |
| `/laura kiss` | Bisou. Refusé quand elle est bâillonnée, quand elle boude ou est encore en colère, ou quand l'affection est sous 200. |
| `/laura compliment` | Compliment. |
| `/laura ungag` | Retire le bâillon de foin. |
| `/laura emote <émote>` | Joue une émote. Voir la liste dans [ACTIONS.md](ACTIONS.md). Les émotes `hug` et `kiss` sont un vrai câlin et un vrai bisou, avec les mêmes règles. |
| `/laura mode passive` / `defensive` / `aggressive` | Règle son mode de combat. |
| `/laura pickup on` / `off` | Elle ramasse dans son inventaire les objets au sol à moins de 8 blocs. |
| `/laura answer yes` / `no` | Répond à sa question « Tu m'aimes encore ? ». |

### Maison

| Commande | Effet |
|---|---|
| `/laura home set` | Sa maison devient le bloc où vous vous tenez, dans votre dimension. |
| `/laura home` ou `/laura home go` | L'envoie à la maison, depuis n'importe où. Depuis plus loin que `home.teleportDistance` ou depuis une autre dimension, elle se téléporte à un endroit sûr à côté de sa maison ; quand il n'y en a pas, elle le dit et reste où elle est. Quand aucune de vos compagnes n'est chargée, la commande répond seulement que vous n'avez pas de compagne : appelez-la d'abord avec `/laura come`. |
| `/laura home clear` | Oublie la maison. Si elle était en mode maison, elle vous suit de nouveau. |

### Rapporter

| Commande | Effet |
|---|---|
| `/laura fetch <objet>` | Elle vous rapporte l'objet. Sans quantité elle rapporte jusqu'à une pile, 16 au plus. |
| `/laura fetch <objet> <quantité>` | Quantité de 1 à 576, limitée par `fetch.maxItems`. |
| `/laura fetch <objet> [quantité] queue` | Ajoute la demande à sa liste de tâches au lieu de commencer tout de suite. |

`<objet>` peut être un identifiant d'objet (`bread`, `minecraft:oak_log`), un tag d'objets (`#minecraft:logs`), `held` (l'objet de votre main principale), ou des mots connus des fichiers de dialogues (`du bois`). La quantité et le mot `queue` sont lus à la fin de la ligne. Exemples : `/laura fetch minecraft:bread 3`, `/laura fetch #minecraft:logs 16 queue`, `/laura fetch held`.

Le même ordre peut s'écrire dans le chat, où un nombre dans le message est la quantité : « apporte-moi 32 pains » fait ce que fait `/laura fetch bread 32`.

### Tâches et liste de tâches

Une tâche est faite une fois et le résultat vous est rapporté.

| Commande | Effet |
|---|---|
| `/laura chop [rayon]` | Abattre un arbre autour de vous. |
| `/laura harvest [rayon]` | Récolter une fois les cultures mûres autour de vous. |
| `/laura cook [rayon]` | Cuisiner une fois ce qu'elle peut. |
| `/laura queue list` | Affiche la tâche en cours et celles en attente. |
| `/laura queue add chop` / `harvest` / `cook` / `come` / `home` / `follow` / `stay` / `work` | Ajoute une étape à la fin de la liste. |
| `/laura queue remove <index>` | Retire une tâche en attente. Index de 1 à 16. |
| `/laura queue clear` | Vide la liste et arrête la tâche en cours. |

`rayon` va de 3 à 64 et est limité par `work.maxRadius`. Sans rayon, `work.defaultRadius` est utilisé. La liste contient 16 tâches au plus.

### Métiers

Un métier est un travail continu dans une zone centrée sur l'endroit où vous vous tenez quand vous donnez l'ordre.

| Commande | Effet |
|---|---|
| `/laura job lumberjack [rayon]` | Démarre le métier de bûcheronne ici. |
| `/laura job farmer [rayon]` | Démarre le métier de fermière ici. |
| `/laura job cook [rayon]` | Démarre le métier de cuisinière ici. |
| `/laura job stop` | Arrête tous les métiers. |
| `/laura job stop lumberjack` / `farmer` / `cook` | Arrête un métier. |
| `/laura job list` | Liste les métiers actifs avec leur zone. |
| `/laura work` | La renvoie à ses métiers après un autre ordre. |

### Coffres

Regardez un conteneur (6 blocs au plus), puis :

| Commande | Effet |
|---|---|
| `/laura chest wood` / `harvest` / `seeds` / `ingredients` / `fuel` / `meals` / `pantry` / `storage` | Attribue le conteneur à cet usage. |
| `/laura chest remove` | Oublie le conteneur que vous regardez. |
| `/laura chest clear` | Oublie tous les conteneurs. |
| `/laura chest list` | Liste les conteneurs attribués. |

### Apparence

| Commande | Effet |
|---|---|
| `/laura skin list` | Skins intégrés et fichiers de skin du serveur. |
| `/laura skin builtin <nom>` | `laura`, `laura_summer`, `laura_winter`, `laura_night`, `laura_sporty`, `laura_gothic`. |
| `/laura skin file <nom> [slim]` | Un fichier du dossier `config/lauramod/skins` du serveur, sans `.png`. Ajoutez `true` ou `false` à la fin pour les bras fins (`true` par défaut). |
| `/laura skin player <pseudo>` | Le skin d'un compte Minecraft. |
| `/laura skin url <url> [slim]` | Un skin depuis une adresse internet. Ajoutez `true` ou `false` à la fin pour les bras fins (`false` par défaut). |
| `/laura skin reset` | Applique `skins.defaultSkin`. |
| `/laura model list` | Modèles du serveur. |
| `/laura model <nom>` | Applique un modèle. Le nom s'écrit tel quel, par exemple `/laura model maid`. |
| `/laura model reset` | Applique `models.defaultModel` (le modèle par défaut de type joueur quand il est vide). |

### Permissions

| Commande | Condition |
|---|---|
| `/laura summon` et l'invocation par le chat | `summoning.summonPermissionLevel` (0 par défaut : tout le monde). |
| `/laura reload` | `permissions.reloadPermissionLevel` (2 par défaut). |
| `/laura admin ...` | `permissions.reloadPermissionLevel` (2 par défaut). |

### Administration

| Commande | Effet |
|---|---|
| `/laura reload` | Recharge la configuration commune, les cadeaux, les désirs, les recettes, les dialogues, et relit les dossiers de skins et de modèles. |
| `/laura admin list` | Toutes les compagnes du monde : propriétaire, nom, état (`active`, `dismissed`, `grave`, `respawning`), position, dimension. |
| `/laura admin remove <joueur>` | Supprime définitivement toutes les compagnes de ce joueur. Une compagne chargée laisse au sol ce qu'elle portait ; une compagne dans un tronçon non chargé est supprimée, et lâche ses affaires, dès que ce tronçon se charge. Ce que portait une compagne renvoyée ou en attente sur une tombe est supprimé avec elle. |
| `/laura admin affection <joueur> <valeur>` | Règle l'affection (0 à 1000) de la compagne de ce joueur. |
| `/laura admin need <joueur> <besoin> <valeur>` | Règle un besoin (0 à 100). Besoins : `hunger`, `energy`, `fun`, `attention`, `hygiene`. |
| `/laura admin desire <joueur> clear` | Retire son désir en cours. |
| `/laura admin desire <joueur> roll` | Lui donne tout de suite un nouveau désir aléatoire. |
| `/laura admin emote <joueur> <émote>` | Joue n'importe quelle émote sur la compagne de ce joueur. |

Les commandes d'administration visent la compagne sélectionnée du joueur, ou la plus proche chargée.

### Touches

| Action | Touche par défaut | Effet |
|---|---|---|
| Ouvrir le menu de Laura | K | Ouvre le menu de la compagne la plus proche à moins de 64 blocs. |
| Roue des émotes | G | Ouvre la roue des émotes pour la compagne la plus proche à moins de 32 blocs. |
| Appeler Laura | non assignée | Envoie `/laura come`. |

### Exemple complet

```
/laura summon
/laura name Jade
/laura home set
/laura job farmer 12
/laura chest harvest
/laura fetch #minecraft:logs 32
/laura queue add cook
/laura queue add home
```

Elle est renommée, reçoit une maison, cultive un champ de rayon 12, range les récoltes dans le coffre que vous regardez, rapporte 32 bûches, puis cuisine une fois et rentre à la maison.
