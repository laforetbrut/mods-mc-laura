# Configuration

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [COMMANDS.md](COMMANDS.md), [ACTIONS.md](ACTIONS.md), [SKINS.md](SKINS.md), [MODELS.md](MODELS.md), [LANGUAGES.md](LANGUAGES.md).

---

## English

### Files

Everything lives in `config/lauramod/` (in the game folder, or in the server folder).

| File or folder | Side | Purpose |
|---|---|---|
| `lauramod-common.json` | server and singleplayer | Main settings, described below. |
| `lauramod-client.json` | client | Display, controls and download limits. Only affects your game. |
| `gifts.json` | server | Gifts, favourite and disliked foods, gifts she gives back. |
| `desires.json` | server | Items, places and activities she can wish for. |
| `recipes.json` | server | Meals the cook prepares at a crafting table. |
| `dialogues/` | server | Custom spoken lines and chat triggers. See [LANGUAGES.md](LANGUAGES.md). |
| `lang/` | client | Custom interface translations. See [LANGUAGES.md](LANGUAGES.md). |
| `skins/` | server | Skin files shared with every player. See [SKINS.md](SKINS.md). |
| `models/` | server and client | Blockbench models. See [MODELS.md](MODELS.md). |
| `animations/` | client | Optional override of the default animations. See [MODELS.md](MODELS.md). |

All files are created with their default content the first time they are needed.

### How the two settings files work

- The format is JSON with `//` comments. Every option is written with its comment, its range and its default.
- Options are grouped in sections. In this guide an option is written `section.option`, for example `follow.teleportDistance`.
- A missing option gets its default value and the file is rewritten. Deleting a line is the way to restore a default.
- A number outside its range is clamped to the nearest limit. A value of the wrong type gets its default.
- `true` and `false` can also be written `"yes"`, `"no"`, `"on"`, `"off"`. Choice values (for example `GRAVE`) are not case sensitive.
- An unknown option is ignored and reported in the log.
- A file that cannot be parsed is copied to `<file name>.broken`, then rewritten with defaults.

### Reloading

- `/laura reload` reloads `lauramod-common.json`, `gifts.json`, `desires.json`, `recipes.json`, the dialogues, and rescans the `skins` and `models` folders. It needs the permission level set in `permissions.reloadPermissionLevel` (2 by default).
- The same files are reloaded every time a server or a singleplayer world starts.
- `lauramod-client.json` is read when the game starts. The switches of the Settings tab of her menu save the file at once.
- Health, speed and attack damage are applied to every companion once per second, so a reload is enough.
- `general.inventoryRows` is applied when a companion is loaded. Items that no longer fit are dropped at her feet.

### lauramod-common.json

#### general

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `defaultName` | `"Laura"` | text | Name of a newly summoned companion. |
| `maxHealth` | `100.0` | 1 to 100000 | Maximum health points. |
| `regenPerSecond` | `0.0` | 0 to 10000 | Health regenerated every second without eating. With 0 she must eat to heal. |
| `invulnerable` | `false` | true / false | She never takes damage. |
| `movementSpeed` | `0.4` | 0.05 to 1.5 | Base walking speed attribute. |
| `attackDamage` | `4.0` | 0 to 1000 | Base attack damage, before the weapon she holds. |
| `maxPerPlayer` | `3` | 1 to 100 | Companions one player can have. |
| `maxPerWorld` | `0` | 0 to 10000 | Limit for the whole world. 0 means no limit. 1 means a single companion for everybody. |
| `extraNames` | `["Emma", "Chloe", "Lea", "Jade", "Lina", "Mia", "Zoe", "Luna", "Ines", "Rose"]` | list of text | Names given to the second, third and following companions. |
| `reviveMode` | `GRAVE` | GRAVE, TIMER, NONE | What happens when she dies. GRAVE: lay a flower on a Laura's Gravestone. TIMER: she comes back next to you after a delay. NONE: she is gone and drops her things. |
| `respawnDelaySeconds` | `30` | 0 to 3600 | Delay before she comes back in TIMER mode. |
| `reviveItems` | `["#minecraft:small_flowers", "#minecraft:flowers"]` | list of item ids or `#tags` | Items that revive her when used on a gravestone. |
| `inventoryRows` | `3` | 1 to 6 | Rows of 9 slots in her personal inventory. |
| `dismissAffectionPenalty` | `20` | 0 to 1000 | Affection lost when she is dismissed. She comes back at the next summon. |

#### summoning

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `chatSummon` | `true` | true / false | Saying "I feel lonely" (in any supported language) summons her. |
| `summonPermissionLevel` | `0` | 0 to 4 | Permission level needed for `/laura summon` and the chat summon. 0 means everyone. |
| `summonCooldownSeconds` | `5` | 0 to 86400 | Delay between two new summons by the same player. |

#### follow

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `startDistance` | `7.0` | 2 to 64 | She starts walking to you when you are further than this (blocks). |
| `stopDistance` | `3.5` | 1 to 32 | She stops when she is this close. |
| `teleportDistance` | `128.0` | 8 to 1024 | She only teleports next to you when she is further than this. Closer, she walks. |
| `teleportWhenStuck` | `false` | true / false | Also teleport when she has been stuck for `stuckSeconds`. |
| `followAcrossDimensions` | `true` | true / false | She follows you to the Nether, the End and other dimensions. |
| `complainWhenStuck` | `true` | true / false | She tells you when she cannot reach you. |
| `stuckSeconds` | `10` | 3 to 600 | Time stuck before she complains. |

#### home

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `radius` | `12` | 2 to 64 | How far she wanders around her home. |
| `teleportDistance` | `48` | 8 to 100000 | Sent home from further than this, she teleports instead of walking. |
| `sleepAtNight` | `true` | true / false | At home she lies down in a nearby bed at night. |

#### personality

Timers that accept 0 are disabled by 0.

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `chatterMinutes` | `10` | 0 to 1440 | Average minutes between two random messages. |
| `loveCheckMinutes` | `15` | 0 to 1440 | Average minutes between two "Do you still love me?" questions. |
| `loveAnswerSeconds` | `30` | 5 to 600 | Time you have to answer her. |
| `weatherComplaints` | `true` | true / false | She comments on rain, thunder, nightfall and morning. |
| `farts` | `true` | true / false | Rare fart sound with a smoke puff. |
| `fartMinutes` | `20` | 1 to 1440 | Average minutes between two of them. |
| `playfulHit` | `true` | true / false | Now and then she hits you for 1 damage point, then apologizes. |
| `playfulHitMinutes` | `60` | 1 to 1440 | Minutes between two playful hits. |
| `jealousOfVillagers` | `true` | true / false | While she follows you nearby, villagers within 5 blocks of her walk away, except when they sleep or trade. |
| `danceToJukebox` | `true` | true / false | She dances when a jukebox plays music within 12 blocks. |
| `greetOnJoin` | `true` | true / false | She greets you when you join the world. |
| `worryWhenOwnerHurt` | `true` | true / false | She worries when your health is low. |
| `ownerHealthThreshold` | `0.3` | 0.05 to 0.95 | Health fraction under which she worries. |
| `feedOwner` | `true` | true / false | When you are hurt and hungry, she gives you food from her inventory. |
| `keepOwnerItemsOnDeath` | `true` | true / false | When you die near her, she collects your items and gives them back. |
| `anniversaryDays` | `7` | 0 to 3650 | She celebrates every N in-game days together. |
| `hitsBeforeSulking` | `3` | 1 to 100 | Hits from her partner before she sulks. |
| `sulkMinutes` | `3` | 1 to 1440 | How long she sulks if you do not apologize. |
| `startAffection` | `500` | 0 to 1000 | Affection when she is summoned. |
| `spontaneousEmotes` | `true` | true / false | She plays an animation on her own now and then. |
| `spontaneousEmoteSeconds` | `90` | 10 to 3600 | Average seconds between two spontaneous animations. |

#### needs

Needs go from 100 (satisfied) to 0 (desperate). The minutes are the time a need takes to go from full to empty, before the `annoyance` multiplier.

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `enabled` | `true` | true / false | Enables hunger, energy, fun, attention and hygiene. |
| `annoyance` | `UNBEARABLE` | CHILL, NORMAL, NEEDY, UNBEARABLE | How demanding she is. See the table below. |
| `hungerMinutes` | `40` | 1 to 10080 | Hunger. |
| `energyMinutes` | `60` | 1 to 10080 | Energy. |
| `funMinutes` | `30` | 1 to 10080 | Fun. |
| `attentionMinutes` | `20` | 1 to 10080 | Attention. |
| `hygieneMinutes` | `120` | 1 to 10080 | Hygiene. |
| `healWhenFedPerSecond` | `0.5` | 0 to 1000 | Health regenerated every second while her hunger is 60 or more. |
| `starvationHurts` | `true` | true / false | She slowly loses health when her hunger is at 0. |
| `starvationCanKill` | `false` | true / false | If false, starvation never takes her below 1 health. |
| `autoEat` | `true` | true / false | She eats the food of her inventory when she is hungry. |
| `searchFoodInContainers` | `true` | true / false | When starving, she looks for food in nearby storage. |
| `stealFood` | `true` | true / false | When starving and next to her partner, she takes food from their inventory. |
| `desires` | `true` | true / false | She regularly wishes for an item, a place or an activity. |
| `desireMinutes` | `12` | 1 to 1440 | Average minutes between two desires. |
| `desireDeadlineMinutes` | `15` | 1 to 1440 | Time you have to fulfil a desire. |
| `refuseOrders` | `true` | true / false | When she is unhappy she sometimes refuses an order. Asking twice always works. |
| `commentActivities` | `true` | true / false | She comments on what you do (mining, fighting, eating alone, being idle). |
| `jealousOfPlayers` | `true` | true / false | She gets jealous when you stay close to, or chat with, other players. |
| `afkMinutes` | `5` | 1 to 1440 | Minutes without moving before she decides you are ignoring her. |
| `giftCooldownSeconds` | `300` | 0 to 86400 | The same kind of gift or favorite food makes her fonder only once in this time, per companion. In between she still takes the gift and eats the food, and a wish is still fulfilled, but she gains no affection and no fun, and gives nothing back. A gift she dislikes always counts. 0 means no limit. |

Effect of `annoyance`:

| Value | Need decay | Nagging frequency | Chance to refuse an order when unhappy |
|---|---|---|---|
| `CHILL` | x 0.5 | x 0.25 | 0 % |
| `NORMAL` | x 1.0 | x 0.6 | 0 % |
| `NEEDY` | x 1.25 | x 1.0 | 15 % |
| `UNBEARABLE` | x 1.5 | x 1.6 | 30 % |

#### combat

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `defaultMode` | `PASSIVE` | PASSIVE, DEFENSIVE, AGGRESSIVE | Combat mode of a new companion. PASSIVE avoids monsters, DEFENSIVE protects you, AGGRESSIVE also attacks nearby monsters. |
| `retaliate` | `true` | true / false | She hits back anyone (except her partner) who hits her, unless she is PASSIVE. |

#### fetch

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `enabled` | `true` | true / false | Enables the fetch order. |
| `radius` | `24` | 4 to 64 | Search radius around her, in blocks. |
| `fromContainers` | `true` | true / false | She may take the item from nearby storage. |
| `breakBlocks` | `true` | true / false | She may harvest blocks of the `lauramod:fetch_harvestable` block tag. Needs the `mobGriefing` game rule. |
| `replantCrops` | `true` | true / false | She replants the crops she harvests. |
| `maxItems` | `64` | 1 to 576 | Maximum number of items per request. |
| `timeoutSeconds` | `60` | 10 to 600 | She gives up after this time. |

#### work

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `enabled` | `true` | true / false | Enables jobs and tasks. |
| `defaultRadius` | `10` | 3 to 64 | Default radius of a work area. |
| `maxRadius` | `24` | 3 to 64 | Largest work area a player can ask for. |
| `onlyNaturalTrees` | `true` | true / false | Only fell trees that carry natural leaves, never logs used in builds. |
| `breakLeaves` | `true` | true / false | Also clear the leaves of a felled tree. |
| `replantSaplings` | `true` | true / false | Plant a sapling where the tree stood. |
| `maxLogsPerTree` | `200` | 1 to 2000 | Safety limit of logs cut in one tree. |
| `plantEmptyFarmland` | `true` | true / false | As a farmer, sow empty farmland with seeds she carries. |
| `useBoneMeal` | `true` | true / false | As a farmer, use bone meal she carries on growing crops. |
| `depositInChests` | `true` | true / false | Store the production in any container of the work area when no chest is assigned. |
| `workAtNight` | `false` | true / false | Keep working at night. |
| `speedPercent` | `100` | 10 to 1000 | Work speed. 200 is twice as fast. |

#### gag

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `enabled` | `true` | true / false | Enables the hay gag (a way to keep her quiet). |
| `gagItems` | `["minecraft:hay_block"]` | list of item ids or `#tags` | Items that gag her on right click. |
| `ungagItems` | `["minecraft:shears"]` | list of item ids or `#tags` | Items that remove the gag. Sneaking with an empty hand works too. |
| `durationSeconds` | `300` | 0 to 86400 | She removes the gag herself after this time. 0 means never. |
| `affectionPenalty` | `15` | 0 to 1000 | Affection lost when gagged. |

#### dialogue

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `chatRange` | `64` | 4 to 512 | Proximity chat: she hears you, and you hear her, within this distance and in the same dimension. |
| `requireName` | `false` | true / false | Chat orders must contain her name ("Laura, follow me"). |
| `fallbackLanguage` | `"en_us"` | language code | Language used when a player's language has no dialogue file. |
| `matchAllLanguages` | `false` | true / false | False: she listens in each player's own language plus English. True: in every language she knows. |
| `forcedLanguage` | `""` | language code or empty | Forces one dialogue language for everyone (for example `fr_fr`). |
| `customDialoguesReplace` | `false` | true / false | False: files of `config/lauramod/dialogues` add to the built-in ones. True: they replace them. |
| `nameColor` | `"light_purple"` | colour name or `#RRGGBB` | Colour of her name in chat. |
| `speechBubbles` | `true` | true / false | Sends what she says as a bubble above her head. |

#### skins

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `defaultSkin` | `"builtin:laura"` | `builtin:<name>`, `server:<file name>`, `url:<link>`, `player:<name>` | Skin of a newly summoned companion, also applied by "reset". See [SKINS.md](SKINS.md). |
| `allowUrlSkins` | `true` | true / false | Allows skins from an internet address. |
| `urlDomainWhitelist` | `["textures.minecraft.net", "i.imgur.com", "s.namemc.com", "namemc.com", "mc-heads.net", "minotar.net", "crafatar.com", "raw.githubusercontent.com", "cdn.discordapp.com", "media.discordapp.net"]` | list of domains | Domains allowed for URL skins, sub-domains included. `"*"` allows every domain. |
| `allowPlayerNameSkins` | `true` | true / false | Allows copying the skin of a Minecraft account by name. |
| `allowUploads` | `true` | true / false | Allows players to upload a skin file to the server. |
| `maxSkinKb` | `256` | 8 to 4096 | Maximum size of a skin file, in kilobytes. |
| `maxUploadsPerPlayer` | `10` | 1 to 1000 | Skin files one player may keep on the server. Uploading a file with the same name replaces it. |
| `othersCanChangeSkin` | `false` | true / false | Allows players other than her partner to change her look. |

#### models

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `allowCustomModels` | `true` | true / false | Allows custom models. |
| `allowUploads` | `false` | true / false | Allows players to upload their own model files to the server. |
| `maxModelKb` | `2048` | 16 to 16384 | Maximum size of a model (all its files), in kilobytes. |
| `maxUploadsPerPlayer` | `3` | 1 to 1000 | Model files one player may keep on the server. Uploading a file with the same name replaces it. |
| `defaultModel` | `""` | model name or empty | Model of a newly summoned companion, also applied by "reset". Empty means the default player-like model. A name is a server file if one exists, otherwise a resource pack model. |

#### permissions

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `othersCanInteract` | `false` | true / false | Allows other players to open her menu and give her orders. |
| `reloadPermissionLevel` | `2` | 0 to 4 | Permission level needed for `/laura reload` and `/laura admin`. |

### lauramod-client.json

#### display

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `speechBubbles` | `true` | true / false | Shows what she says in a bubble above her head. |
| `bubbleSeconds` | `6` | 1 to 60 | How long a speech bubble stays visible. |
| `thoughtBubbles` | `true` | true / false | Shows what she desires above her head. |
| `animations` | `true` | true / false | Plays her animations. |
| `customModels` | `true` | true / false | Renders custom Blockbench models. If false she always uses the default model. |
| `remoteSkins` | `true` | true / false | Downloads skins from the internet. If false, URL skins show the default skin. |
| `particles` | `true` | true / false | Hearts, tears, sparkles and other particles. |
| `showNeedsHud` | `false` | true / false | Shows her needs in a small overlay while she is within 32 blocks. |

#### controls

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `menuOnRightClick` | `true` | true / false | Right click with an empty hand opens her menu. Sneak + right click always opens her inventory. |

#### network

| Option | Default | Range or values | Meaning |
|---|---|---|---|
| `maxSkinDownloadKb` | `512` | 8 to 8192 | Largest skin you accept to download, in kilobytes. |
| `maxModelDownloadKb` | `4096` | 16 to 32768 | Largest model you accept to receive from a server, in kilobytes. |

### gifts.json

Top-level keys:

| Key | Content |
|---|---|
| `gifts` | List of gift entries. The first matching entry is used. |
| `foods.favorites` | Item ids or `#tags` she loves to eat. |
| `foods.disliked` | Item ids or `#tags` she hates to eat. |
| `returnGifts` | Things she may give back: `item`, `count`, `weight`. |

Fields of a gift entry:

| Field | Default | Meaning |
|---|---|---|
| `match` | required | Item id or `#item_tag`. |
| `affection` | `5` | Affection gained or lost. Affection always stays between 0 and 1000. |
| `fun` | `5` | Fun gained. |
| `tier` | `NICE` | `GROSS`, `MEH`, `NICE`, `GREAT` or `AMAZING`. Picks her reaction line. |
| `returnChance` | `0.0` | Chance from 0 to 1 that she gives something back. It is multiplied by 0.5 plus her affection divided by 1000. |
| `food` | `0` | Nutrition if she can eat an item that is not normally edible (the cake for example). |

Worked example: make a block of emerald an amazing gift, and make bread a favourite food.

```json
{
  "gifts": [
    { "match": "minecraft:emerald_block", "affection": 60, "fun": 30, "tier": "AMAZING", "returnChance": 0.5 },
    { "match": "minecraft:cake", "affection": 35, "fun": 30, "tier": "AMAZING", "returnChance": 0.3, "food": 14 }
  ],
  "foods": {
    "favorites": [ "minecraft:cake", "minecraft:bread" ],
    "disliked": [ "minecraft:rotten_flesh" ]
  },
  "returnGifts": [
    { "item": "minecraft:poppy", "count": 1, "weight": 20 },
    { "item": "minecraft:cookie", "count": 3, "weight": 15 }
  ]
}
```

The file replaces the default lists, so keep the entries you still want. Put the more specific entries before the tags. Apply with `/laura reload`.

### desires.json

| Key | Content |
|---|---|
| `categoryWeights` | `items`, `places`, `activities`: how often each kind is picked. Defaults: 45, 20, 35. |
| `items` | Entries with `item` (id or `#tag`), `eat` (true: she must eat it, false: giving it is enough) and `weight` (default 10). |
| `places` | Entries with `id`, `biomes`, `icon` (item id shown in her thought bubble) and `weight`. `biomes` accepts biome ids, `#biome_tags` and `dimension:<id>` (for example `dimension:minecraft:the_nether`). |
| `activities` | Entries with `id` and `weight`. The ids are fixed: `dance`, `hug`, `kiss`, `compliment`, `sleep_together`, `sunset`, `stargaze`, `music`, `boat_ride`, `swim`, `pet`, `new_outfit`, `fireworks`, `campfire`, `flowers`, `walk`. A weight of 0 disables one. |

A new place needs a name: add the key `lauramod.place.<id>` in `config/lauramod/lang/<language>.json` (see [LANGUAGES.md](LANGUAGES.md)).

### recipes.json

| Key | Default | Content |
|---|---|---|
| `requireCraftingTable` | `true` | She needs a crafting table in her work area to prepare a meal. |
| `useCampfires` | `true` | She may cook raw food on lit campfires. |
| `meals` | 10 recipes | Entries with `id`, `result` (item id), `count` (default 1), `ingredients` (item id or `#tag` to amount) and optional `returns` (item id to amount, for example the empty buckets of a cake). Meals are tried in the order of the file. |

Raw food cooked in furnaces, smokers and campfires uses the game's own recipes and needs no entry.

```json
{ "id": "bread", "result": "minecraft:bread", "count": 1, "ingredients": { "minecraft:wheat": 3 } }
```

### Worked example: a calm companion

```json
{
  "needs": {
    "annoyance": "CHILL",
    "desireMinutes": 30,
    "refuseOrders": false
  },
  "personality": {
    "loveCheckMinutes": 0,
    "farts": false,
    "playfulHit": false
  }
}
```

Only the listed options change. Every other option keeps its default and is written back to the file.

---

## Français

### Fichiers

Tout se trouve dans `config/lauramod/` (dans le dossier du jeu, ou dans celui du serveur).

| Fichier ou dossier | Côté | Rôle |
|---|---|---|
| `lauramod-common.json` | serveur et solo | Réglages principaux, décrits plus bas. |
| `lauramod-client.json` | client | Affichage, contrôles et limites de téléchargement. Ne concerne que votre jeu. |
| `gifts.json` | serveur | Cadeaux, aliments préférés et détestés, cadeaux qu'elle offre en retour. |
| `desires.json` | serveur | Objets, lieux et activités qu'elle peut désirer. |
| `recipes.json` | serveur | Plats que la cuisinière prépare sur un établi. |
| `dialogues/` | serveur | Répliques et déclencheurs de chat personnalisés. Voir [LANGUAGES.md](LANGUAGES.md). |
| `lang/` | client | Traductions d'interface personnalisées. Voir [LANGUAGES.md](LANGUAGES.md). |
| `skins/` | serveur | Fichiers de skin partagés avec tous les joueurs. Voir [SKINS.md](SKINS.md). |
| `models/` | serveur et client | Modèles Blockbench. Voir [MODELS.md](MODELS.md). |
| `animations/` | client | Remplacement facultatif des animations par défaut. Voir [MODELS.md](MODELS.md). |

Tous les fichiers sont créés avec leur contenu par défaut la première fois qu'ils sont nécessaires.

### Fonctionnement des deux fichiers de réglages

- Le format est du JSON avec des commentaires `//`. Chaque option est écrite avec son commentaire, sa plage et sa valeur par défaut.
- Les options sont rangées par sections. Dans ce guide une option s'écrit `section.option`, par exemple `follow.teleportDistance`.
- Une option absente reprend sa valeur par défaut et le fichier est réécrit. Supprimer une ligne suffit donc à rétablir une valeur par défaut.
- Un nombre hors plage est ramené à la limite la plus proche. Une valeur du mauvais type reprend la valeur par défaut.
- `true` et `false` peuvent aussi s'écrire `"yes"`, `"no"`, `"on"`, `"off"`. Les valeurs à choix (par exemple `GRAVE`) ne tiennent pas compte de la casse.
- Une option inconnue est ignorée et signalée dans le journal.
- Un fichier illisible est copié en `<nom du fichier>.broken`, puis réécrit avec les valeurs par défaut.

### Rechargement

- `/laura reload` recharge `lauramod-common.json`, `gifts.json`, `desires.json`, `recipes.json`, les dialogues, et relit les dossiers `skins` et `models`. La commande demande le niveau de permission défini par `permissions.reloadPermissionLevel` (2 par défaut).
- Les mêmes fichiers sont rechargés à chaque démarrage d'un serveur ou d'un monde solo.
- `lauramod-client.json` est lu au lancement du jeu. Les interrupteurs de l'onglet Réglages de son menu enregistrent le fichier aussitôt.
- La vie, la vitesse et les dégâts sont appliqués à chaque compagne une fois par seconde : un rechargement suffit.
- `general.inventoryRows` est appliqué au chargement d'une compagne. Les objets qui ne rentrent plus tombent à ses pieds.

### lauramod-common.json

#### general

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `defaultName` | `"Laura"` | texte | Nom d'une compagne nouvellement invoquée. |
| `maxHealth` | `100.0` | 1 à 100000 | Points de vie maximum. |
| `regenPerSecond` | `0.0` | 0 à 10000 | Vie régénérée chaque seconde sans manger. Avec 0 elle doit manger pour se soigner. |
| `invulnerable` | `false` | true / false | Elle ne subit jamais de dégâts. |
| `movementSpeed` | `0.4` | 0.05 à 1.5 | Attribut de vitesse de marche. |
| `attackDamage` | `4.0` | 0 à 1000 | Dégâts d'attaque de base, avant l'arme qu'elle tient. |
| `maxPerPlayer` | `3` | 1 à 100 | Nombre de compagnes par joueur. |
| `maxPerWorld` | `0` | 0 à 10000 | Limite pour tout le monde. 0 : aucune limite. 1 : une seule compagne pour tous. |
| `extraNames` | `["Emma", "Chloe", "Lea", "Jade", "Lina", "Mia", "Zoe", "Luna", "Ines", "Rose"]` | liste de textes | Noms donnés à la deuxième compagne, à la troisième et aux suivantes. |
| `reviveMode` | `GRAVE` | GRAVE, TIMER, NONE | Ce qui se passe à sa mort. GRAVE : déposer une fleur sur une Tombe de Laura. TIMER : elle revient près de vous après un délai. NONE : elle disparaît et lâche ses affaires. |
| `respawnDelaySeconds` | `30` | 0 à 3600 | Délai avant son retour en mode TIMER. |
| `reviveItems` | `["#minecraft:small_flowers", "#minecraft:flowers"]` | liste d'identifiants ou de `#tags` | Objets qui la ramènent quand on les utilise sur une tombe. |
| `inventoryRows` | `3` | 1 à 6 | Rangées de 9 emplacements dans son inventaire. |
| `dismissAffectionPenalty` | `20` | 0 à 1000 | Affection perdue quand elle est congédiée. Elle revient à la prochaine invocation. |

#### summoning

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `chatSummon` | `true` | true / false | Dire « je me sens seul » (dans une langue gérée) l'invoque. |
| `summonPermissionLevel` | `0` | 0 à 4 | Niveau de permission requis pour `/laura summon` et l'invocation par le chat. 0 : tout le monde. |
| `summonCooldownSeconds` | `5` | 0 à 86400 | Délai entre deux nouvelles invocations par le même joueur. |

#### follow

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `startDistance` | `7.0` | 2 à 64 | Elle se met en marche quand vous êtes plus loin que cela (en blocs). |
| `stopDistance` | `3.5` | 1 à 32 | Elle s'arrête à cette distance. |
| `teleportDistance` | `128.0` | 8 à 1024 | Elle ne se téléporte près de vous qu'au-delà de cette distance. Plus près, elle marche. |
| `teleportWhenStuck` | `false` | true / false | Téléporte aussi quand elle est bloquée depuis `stuckSeconds`. |
| `followAcrossDimensions` | `true` | true / false | Elle vous suit dans le Nether, l'End et les autres dimensions. |
| `complainWhenStuck` | `true` | true / false | Elle vous prévient quand elle ne peut pas vous rejoindre. |
| `stuckSeconds` | `10` | 3 à 600 | Durée de blocage avant qu'elle se plaigne. |

#### home

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `radius` | `12` | 2 à 64 | Distance à laquelle elle se promène autour de sa maison. |
| `teleportDistance` | `48` | 8 à 100000 | Envoyée à la maison depuis plus loin, elle se téléporte au lieu de marcher. |
| `sleepAtNight` | `true` | true / false | À la maison, elle se couche la nuit dans un lit proche. |

#### personality

Les minuteries qui acceptent 0 sont désactivées par 0.

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `chatterMinutes` | `10` | 0 à 1440 | Minutes en moyenne entre deux messages aléatoires. |
| `loveCheckMinutes` | `15` | 0 à 1440 | Minutes en moyenne entre deux questions « Tu m'aimes encore ? ». |
| `loveAnswerSeconds` | `30` | 5 à 600 | Temps dont vous disposez pour répondre. |
| `weatherComplaints` | `true` | true / false | Elle commente la pluie, l'orage, la tombée de la nuit et le matin. |
| `farts` | `true` | true / false | Son de pet rare avec un nuage de fumée. |
| `fartMinutes` | `20` | 1 à 1440 | Minutes en moyenne entre deux de ces sons. |
| `playfulHit` | `true` | true / false | De temps en temps elle vous donne un coup de 1 point de dégât, puis s'excuse. |
| `playfulHitMinutes` | `60` | 1 à 1440 | Minutes entre deux coups taquins. |
| `jealousOfVillagers` | `true` | true / false | Quand elle vous suit de près, les villageois à moins de 5 blocs d'elle s'éloignent, sauf s'ils dorment ou commercent. |
| `danceToJukebox` | `true` | true / false | Elle danse quand un jukebox joue de la musique à moins de 12 blocs. |
| `greetOnJoin` | `true` | true / false | Elle vous salue quand vous rejoignez le monde. |
| `worryWhenOwnerHurt` | `true` | true / false | Elle s'inquiète quand votre vie est basse. |
| `ownerHealthThreshold` | `0.3` | 0.05 à 0.95 | Fraction de vie sous laquelle elle s'inquiète. |
| `feedOwner` | `true` | true / false | Quand vous êtes blessé et affamé, elle vous donne de la nourriture de son inventaire. |
| `keepOwnerItemsOnDeath` | `true` | true / false | Quand vous mourez près d'elle, elle ramasse vos objets et vous les rend. |
| `anniversaryDays` | `7` | 0 à 3650 | Elle fête chaque période de N jours de jeu passés ensemble. |
| `hitsBeforeSulking` | `3` | 1 à 100 | Coups de son partenaire avant qu'elle boude. |
| `sulkMinutes` | `3` | 1 à 1440 | Durée de la bouderie sans excuses. |
| `startAffection` | `500` | 0 à 1000 | Affection à l'invocation. |
| `spontaneousEmotes` | `true` | true / false | Elle joue de temps en temps une animation d'elle-même. |
| `spontaneousEmoteSeconds` | `90` | 10 à 3600 | Secondes en moyenne entre deux animations spontanées. |

#### needs

Les besoins vont de 100 (satisfait) à 0 (désespéré). Les minutes indiquent le temps qu'un besoin met à passer de plein à vide, avant le multiplicateur de `annoyance`.

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `enabled` | `true` | true / false | Active la faim, l'énergie, l'amusement, l'attention et l'hygiène. |
| `annoyance` | `UNBEARABLE` | CHILL, NORMAL, NEEDY, UNBEARABLE | Son niveau d'exigence. Voir le tableau plus bas. |
| `hungerMinutes` | `40` | 1 à 10080 | Faim. |
| `energyMinutes` | `60` | 1 à 10080 | Énergie. |
| `funMinutes` | `30` | 1 à 10080 | Amusement. |
| `attentionMinutes` | `20` | 1 à 10080 | Attention. |
| `hygieneMinutes` | `120` | 1 à 10080 | Hygiène. |
| `healWhenFedPerSecond` | `0.5` | 0 à 1000 | Vie régénérée chaque seconde tant que sa faim vaut 60 ou plus. |
| `starvationHurts` | `true` | true / false | Elle perd lentement de la vie quand sa faim est à 0. |
| `starvationCanKill` | `false` | true / false | Si false, la famine ne la fait jamais descendre sous 1 point de vie. |
| `autoEat` | `true` | true / false | Elle mange la nourriture de son inventaire quand elle a faim. |
| `searchFoodInContainers` | `true` | true / false | Affamée, elle cherche à manger dans les rangements proches. |
| `stealFood` | `true` | true / false | Affamée et à côté de son partenaire, elle prend de la nourriture dans son inventaire. |
| `desires` | `true` | true / false | Elle désire régulièrement un objet, un lieu ou une activité. |
| `desireMinutes` | `12` | 1 à 1440 | Minutes en moyenne entre deux désirs. |
| `desireDeadlineMinutes` | `15` | 1 à 1440 | Temps dont vous disposez pour réaliser un désir. |
| `refuseOrders` | `true` | true / false | Mécontente, elle refuse parfois un ordre. Demander deux fois marche toujours. |
| `commentActivities` | `true` | true / false | Elle commente ce que vous faites (miner, combattre, manger seul, rester immobile). |
| `jealousOfPlayers` | `true` | true / false | Elle devient jalouse quand vous restez près d'autres joueurs ou discutez avec eux. |
| `afkMinutes` | `5` | 1 à 1440 | Minutes sans bouger avant qu'elle estime que vous l'ignorez. |
| `giftCooldownSeconds` | `300` | 0 à 86400 | Un même type de cadeau ou de nourriture préférée ne la rend plus affectueuse qu'une fois pendant ce délai, par compagne. Entre-temps elle prend quand même le cadeau et mange la nourriture, et un désir est quand même réalisé, mais elle ne gagne ni affection ni amusement et n'offre rien en retour. Un cadeau qu'elle déteste compte toujours. 0 : aucune limite. |

Effet de `annoyance` :

| Valeur | Baisse des besoins | Fréquence des rappels | Chance de refuser un ordre quand elle est mécontente |
|---|---|---|---|
| `CHILL` | x 0.5 | x 0.25 | 0 % |
| `NORMAL` | x 1.0 | x 0.6 | 0 % |
| `NEEDY` | x 1.25 | x 1.0 | 15 % |
| `UNBEARABLE` | x 1.5 | x 1.6 | 30 % |

#### combat

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `defaultMode` | `PASSIVE` | PASSIVE, DEFENSIVE, AGGRESSIVE | Mode de combat d'une nouvelle compagne. PASSIVE évite les monstres, DEFENSIVE vous protège, AGGRESSIVE attaque aussi les monstres proches. |
| `retaliate` | `true` | true / false | Elle rend les coups à quiconque la frappe (sauf son partenaire), sauf en mode PASSIVE. |

#### fetch

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `enabled` | `true` | true / false | Active l'ordre « rapporte ». |
| `radius` | `24` | 4 à 64 | Rayon de recherche autour d'elle, en blocs. |
| `fromContainers` | `true` | true / false | Elle peut prendre l'objet dans les rangements proches. |
| `breakBlocks` | `true` | true / false | Elle peut récolter les blocs du tag de blocs `lauramod:fetch_harvestable`. Demande la règle de jeu `mobGriefing`. |
| `replantCrops` | `true` | true / false | Elle replante les cultures qu'elle récolte. |
| `maxItems` | `64` | 1 à 576 | Nombre maximum d'objets par demande. |
| `timeoutSeconds` | `60` | 10 à 600 | Elle abandonne après ce délai. |

#### work

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `enabled` | `true` | true / false | Active les métiers et les tâches. |
| `defaultRadius` | `10` | 3 à 64 | Rayon par défaut d'une zone de travail. |
| `maxRadius` | `24` | 3 à 64 | Plus grande zone de travail qu'un joueur peut demander. |
| `onlyNaturalTrees` | `true` | true / false | N'abat que les arbres qui portent des feuilles naturelles, jamais les bûches des constructions. |
| `breakLeaves` | `true` | true / false | Retire aussi les feuilles d'un arbre abattu. |
| `replantSaplings` | `true` | true / false | Plante une pousse à la place de l'arbre. |
| `maxLogsPerTree` | `200` | 1 à 2000 | Limite de sécurité de bûches coupées par arbre. |
| `plantEmptyFarmland` | `true` | true / false | Fermière, elle sème la terre labourée vide avec les graines qu'elle porte. |
| `useBoneMeal` | `true` | true / false | Fermière, elle utilise la poudre d'os qu'elle porte sur les cultures. |
| `depositInChests` | `true` | true / false | Range la production dans n'importe quel conteneur de la zone quand aucun coffre n'est attribué. |
| `workAtNight` | `false` | true / false | Continue de travailler la nuit. |
| `speedPercent` | `100` | 10 à 1000 | Vitesse de travail. 200 : deux fois plus vite. |

#### gag

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `enabled` | `true` | true / false | Active le bâillon de foin (un moyen de la faire taire). |
| `gagItems` | `["minecraft:hay_block"]` | liste d'identifiants ou de `#tags` | Objets qui la bâillonnent au clic droit. |
| `ungagItems` | `["minecraft:shears"]` | liste d'identifiants ou de `#tags` | Objets qui retirent le bâillon. S'accroupir avec la main vide marche aussi. |
| `durationSeconds` | `300` | 0 à 86400 | Elle retire le bâillon elle-même après ce délai. 0 : jamais. |
| `affectionPenalty` | `15` | 0 à 1000 | Affection perdue quand elle est bâillonnée. |

#### dialogue

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `chatRange` | `64` | 4 à 512 | Chat de proximité : elle vous entend, et vous l'entendez, dans ce rayon et dans la même dimension. |
| `requireName` | `false` | true / false | Les ordres du chat doivent contenir son nom (« Laura, suis-moi »). |
| `fallbackLanguage` | `"en_us"` | code de langue | Langue utilisée quand la langue d'un joueur n'a pas de fichier de dialogues. |
| `matchAllLanguages` | `false` | true / false | False : elle écoute la langue de chaque joueur plus l'anglais. True : toutes les langues qu'elle connaît. |
| `forcedLanguage` | `""` | code de langue ou vide | Impose une langue de dialogue à tout le monde (par exemple `fr_fr`). |
| `customDialoguesReplace` | `false` | true / false | False : les fichiers de `config/lauramod/dialogues` s'ajoutent aux dialogues intégrés. True : ils les remplacent. |
| `nameColor` | `"light_purple"` | nom de couleur ou `#RRGGBB` | Couleur de son nom dans le chat. |
| `speechBubbles` | `true` | true / false | Envoie ce qu'elle dit sous forme de bulle au-dessus de sa tête. |

#### skins

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `defaultSkin` | `"builtin:laura"` | `builtin:<nom>`, `server:<nom de fichier>`, `url:<lien>`, `player:<pseudo>` | Skin d'une compagne nouvellement invoquée, aussi appliqué par « reset ». Voir [SKINS.md](SKINS.md). |
| `allowUrlSkins` | `true` | true / false | Autorise les skins depuis une adresse internet. |
| `urlDomainWhitelist` | `["textures.minecraft.net", "i.imgur.com", "s.namemc.com", "namemc.com", "mc-heads.net", "minotar.net", "crafatar.com", "raw.githubusercontent.com", "cdn.discordapp.com", "media.discordapp.net"]` | liste de domaines | Domaines autorisés pour les skins par URL, sous-domaines compris. `"*"` autorise tous les domaines. |
| `allowPlayerNameSkins` | `true` | true / false | Autorise la copie du skin d'un compte Minecraft par son pseudo. |
| `allowUploads` | `true` | true / false | Autorise les joueurs à envoyer un fichier de skin au serveur. |
| `maxSkinKb` | `256` | 8 à 4096 | Taille maximale d'un fichier de skin, en kilo-octets. |
| `maxUploadsPerPlayer` | `10` | 1 à 1000 | Nombre de fichiers de skin qu'un joueur peut garder sur le serveur. Envoyer un fichier du même nom le remplace. |
| `othersCanChangeSkin` | `false` | true / false | Autorise d'autres joueurs que son partenaire à changer son apparence. |

#### models

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `allowCustomModels` | `true` | true / false | Autorise les modèles personnalisés. |
| `allowUploads` | `false` | true / false | Autorise les joueurs à envoyer leurs propres modèles au serveur. |
| `maxModelKb` | `2048` | 16 à 16384 | Taille maximale d'un modèle (tous ses fichiers), en kilo-octets. |
| `maxUploadsPerPlayer` | `3` | 1 à 1000 | Nombre de fichiers de modèle qu'un joueur peut garder sur le serveur. Envoyer un fichier du même nom le remplace. |
| `defaultModel` | `""` | nom de modèle ou vide | Modèle d'une compagne nouvellement invoquée, aussi appliqué par « reset ». Vide : le modèle par défaut de type joueur. Un nom désigne un fichier du serveur s'il existe, sinon un modèle de pack de ressources. |

#### permissions

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `othersCanInteract` | `false` | true / false | Autorise les autres joueurs à ouvrir son menu et à lui donner des ordres. |
| `reloadPermissionLevel` | `2` | 0 à 4 | Niveau de permission requis pour `/laura reload` et `/laura admin`. |

### lauramod-client.json

#### display

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `speechBubbles` | `true` | true / false | Affiche ce qu'elle dit dans une bulle au-dessus de sa tête. |
| `bubbleSeconds` | `6` | 1 à 60 | Durée d'affichage d'une bulle. |
| `thoughtBubbles` | `true` | true / false | Affiche ce qu'elle désire au-dessus de sa tête. |
| `animations` | `true` | true / false | Joue ses animations. |
| `customModels` | `true` | true / false | Affiche les modèles Blockbench personnalisés. Si false elle garde toujours le modèle par défaut. |
| `remoteSkins` | `true` | true / false | Télécharge les skins depuis internet. Si false, les skins par URL affichent le skin par défaut. |
| `particles` | `true` | true / false | Cœurs, larmes, étincelles et autres particules. |
| `showNeedsHud` | `false` | true / false | Affiche ses besoins dans un petit cadre tant qu'elle est à moins de 32 blocs. |

#### controls

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `menuOnRightClick` | `true` | true / false | Le clic droit à main vide ouvre son menu. S'accroupir + clic droit ouvre toujours son inventaire. |

#### network

| Option | Défaut | Plage ou valeurs | Signification |
|---|---|---|---|
| `maxSkinDownloadKb` | `512` | 8 à 8192 | Plus gros skin que vous acceptez de télécharger, en kilo-octets. |
| `maxModelDownloadKb` | `4096` | 16 à 32768 | Plus gros modèle que vous acceptez de recevoir d'un serveur, en kilo-octets. |

### gifts.json

Clés de premier niveau :

| Clé | Contenu |
|---|---|
| `gifts` | Liste des cadeaux. La première entrée qui correspond est utilisée. |
| `foods.favorites` | Identifiants ou `#tags` des aliments qu'elle adore. |
| `foods.disliked` | Identifiants ou `#tags` des aliments qu'elle déteste. |
| `returnGifts` | Ce qu'elle peut offrir en retour : `item`, `count`, `weight`. |

Champs d'une entrée de cadeau :

| Champ | Défaut | Signification |
|---|---|---|
| `match` | obligatoire | Identifiant d'objet ou `#tag_d_objets`. |
| `affection` | `5` | Affection gagnée ou perdue. L'affection reste toujours entre 0 et 1000. |
| `fun` | `5` | Amusement gagné. |
| `tier` | `NICE` | `GROSS`, `MEH`, `NICE`, `GREAT` ou `AMAZING`. Choisit sa réplique de réaction. |
| `returnChance` | `0.0` | Chance de 0 à 1 qu'elle offre quelque chose en retour. Elle est multipliée par 0.5 plus son affection divisée par 1000. |
| `food` | `0` | Valeur nutritive si elle peut manger un objet qui n'est pas comestible d'habitude (le gâteau par exemple). |

Exemple complet : faire du bloc d'émeraude un cadeau exceptionnel, et du pain un aliment préféré.

```json
{
  "gifts": [
    { "match": "minecraft:emerald_block", "affection": 60, "fun": 30, "tier": "AMAZING", "returnChance": 0.5 },
    { "match": "minecraft:cake", "affection": 35, "fun": 30, "tier": "AMAZING", "returnChance": 0.3, "food": 14 }
  ],
  "foods": {
    "favorites": [ "minecraft:cake", "minecraft:bread" ],
    "disliked": [ "minecraft:rotten_flesh" ]
  },
  "returnGifts": [
    { "item": "minecraft:poppy", "count": 1, "weight": 20 },
    { "item": "minecraft:cookie", "count": 3, "weight": 15 }
  ]
}
```

Le fichier remplace les listes par défaut : gardez les entrées que vous voulez conserver. Placez les entrées précises avant les tags. Appliquez avec `/laura reload`.

### desires.json

| Clé | Contenu |
|---|---|
| `categoryWeights` | `items`, `places`, `activities` : fréquence de chaque catégorie. Valeurs par défaut : 45, 20, 35. |
| `items` | Entrées avec `item` (identifiant ou `#tag`), `eat` (true : elle doit le manger, false : le lui donner suffit) et `weight` (10 par défaut). |
| `places` | Entrées avec `id`, `biomes`, `icon` (identifiant de l'objet affiché dans sa bulle de pensée) et `weight`. `biomes` accepte des identifiants de biomes, des `#tags_de_biomes` et `dimension:<id>` (par exemple `dimension:minecraft:the_nether`). |
| `activities` | Entrées avec `id` et `weight`. Les identifiants sont fixes : `dance`, `hug`, `kiss`, `compliment`, `sleep_together`, `sunset`, `stargaze`, `music`, `boat_ride`, `swim`, `pet`, `new_outfit`, `fireworks`, `campfire`, `flowers`, `walk`. Un poids de 0 en désactive une. |

Un nouveau lieu a besoin d'un nom : ajoutez la clé `lauramod.place.<id>` dans `config/lauramod/lang/<langue>.json` (voir [LANGUAGES.md](LANGUAGES.md)).

### recipes.json

| Clé | Défaut | Contenu |
|---|---|---|
| `requireCraftingTable` | `true` | Il lui faut un établi dans sa zone de travail pour préparer un plat. |
| `useCampfires` | `true` | Elle peut cuire la nourriture crue sur des feux de camp allumés. |
| `meals` | 10 recettes | Entrées avec `id`, `result` (identifiant d'objet), `count` (1 par défaut), `ingredients` (identifiant ou `#tag` vers une quantité) et `returns` facultatif (identifiant vers une quantité, par exemple les seaux vides d'un gâteau). Les plats sont essayés dans l'ordre du fichier. |

La nourriture crue cuite dans les fourneaux, les fumoirs et les feux de camp utilise les recettes du jeu et n'a pas besoin d'entrée.

```json
{ "id": "bread", "result": "minecraft:bread", "count": 1, "ingredients": { "minecraft:wheat": 3 } }
```

### Exemple complet : une compagne tranquille

```json
{
  "needs": {
    "annoyance": "CHILL",
    "desireMinutes": 30,
    "refuseOrders": false
  },
  "personality": {
    "loveCheckMinutes": 0,
    "farts": false,
    "playfulHit": false
  }
}
```

Seules les options listées changent. Toutes les autres gardent leur valeur par défaut et sont réécrites dans le fichier.
