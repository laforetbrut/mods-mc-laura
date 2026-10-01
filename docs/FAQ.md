# Frequently asked questions

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Guides : [ACTIONS.md](ACTIONS.md), [COMMANDS.md](COMMANDS.md), [CONFIG.md](CONFIG.md), [SKINS.md](SKINS.md), [MODELS.md](MODELS.md), [LANGUAGES.md](LANGUAGES.md), [KUBEJS.md](KUBEJS.md), [COMPATIBILITY.md](COMPATIBILITY.md), [BUILDING.md](BUILDING.md).

Issues and suggestions / Problèmes et suggestions : <https://github.com/laforetbrut/lauramod/issues>

---

## English

### Getting started

**How do I get a companion?**
Three ways: say "I feel lonely" in chat, run `/laura summon`, or smelt a pink tulip in a furnace and use the Laura's Heart you obtain.

**I said the phrase and nothing happened.**
Check these points:

- The phrase must be written in the language of your game, or in English.
- `summoning.chatSummon` must be true, and you need the permission level of `summoning.summonPermissionLevel`.
- Two new summons must be 5 seconds apart.
- You may already have the maximum (3 by default). Then your nearest companion within 64 blocks answers that she is already here.
- The world may have reached `general.maxPerWorld`.

**How many companions can I have?**
3 by default (`general.maxPerPlayer`). The second and the following ones get the names of `general.extraNames`. Laura's Heart only creates your first companion: ask for the next ones with `/laura summon` or the chat phrase.

**How do I rename her?**
`/laura name <name>`, the pencil next to her name in the menu, or a name tag.

### Talking and orders

**She does not react to what I write.**

- She only hears you within 64 blocks and in the same dimension (`dialogue.chatRange`).
- She listens in your game language and in English. Run `/laura lang` to see which language she uses with you.
- With `dialogue.requireName` you must write her name.
- When she is gagged, every answer is a muffled sound.
- Only her partner can give her orders, unless `permissions.othersCanInteract` is true.

**She refuses my orders.**
She does that when she is sad, angry, jealous, sulking or hungry, and only with the `NEEDY` and `UNBEARABLE` levels of `needs.annoyance`. Give the same order again within 20 seconds and she obeys. To stop it for good, set `needs.refuseOrders` to false.

**She sulks. What now?**
Write "sorry" in chat, fulfil her current desire, or wait a few minutes (`personality.sulkMinutes`).

**She is too demanding.**
In `lauramod-common.json`:

| Goal | Setting |
|---|---|
| Calmer in general | `needs.annoyance`: `CHILL` or `NORMAL` |
| No needs at all | `needs.enabled`: false |
| No desires | `needs.desires`: false |
| Fewer random messages | `personality.chatterMinutes`: a larger number, or 0 |
| No love question | `personality.loveCheckMinutes`: 0 |
| No fart sound, no playful hit | `personality.farts`: false, `personality.playfulHit`: false |

**How do I keep her quiet for a moment?**
Right click her with a hay bale. Remove it with shears, or `/laura ungag`. To hide her speech bubbles only for you, use the Settings tab of her menu.

**How do I write an item or a file name in a command?**
As it is, without quotes: `/laura fetch minecraft:oak_log 16`, `/laura fetch #minecraft:logs`, `/laura fetch bread`, `/laura skin file uploads/my_skin`. In a fetch command the count and the word `queue` go at the end of the line.

### Following and finding her

**She does not teleport to me any more.**
That is intended in 2.0.0. She walks, and teleports only beyond 128 blocks (`follow.teleportDistance`). When she is stuck she tells you. Use `/laura come`, lower `follow.teleportDistance`, or set `follow.teleportWhenStuck` to true.

**Where is she?**
`/laura where` gives the position, dimension and state of each companion. `/laura come` calls the selected one from anywhere. Laura's Heart does the same and makes her happy.

**How do I call back a companion that stayed far away?**
`/laura come`, the call key and Laura's Heart bring back the companions you already have, even from unloaded chunks or after a dismissal. They create a companion only when you have none.

**How do I part with a companion?**
`/laura dismiss` puts her away: she loses 20 affection, still counts toward your limit, and comes back at the next summon. `/laura release`, then `/laura release confirm`, removes her for good: she drops everything she carries and nothing brings her back.

**Does she follow me to the Nether and the End?**
Yes, in follow mode (`follow.followAcrossDimensions`). She is also brought along after a waystone, a teleport command or an ender pearl.

**Can she despawn?**
No. She never despawns, and the world keeps a record of every companion. If she cannot be found when you call her, she is restored from her last saved state.

### Health, death and inventory

**She does not regenerate.**
There is no free regeneration by default. Feed her: with hunger at 60 or more she heals 0.5 health per second. For the old behaviour set `general.regenPerSecond` above 0.

**She died. How do I get her back?**
With the default mode (`GRAVE`): craft a Laura's Gravestone (one stone bricks on top, three stone bricks in the middle, three cobblestone slabs at the bottom), place it anywhere and right click it with a flower. She comes back with her inventory. See [ACTIONS.md](ACTIONS.md) for the other modes.

**How do I give her armor, a weapon or a backpack?**
Sneak and right click her to open her inventory: four armor slots, both hands and a back slot. A backpack can also be given with a plain right click.

**Does she lose her items when she dies?**
No with `GRAVE` and `TIMER`. Yes with `NONE`: she drops everything.

### Work and fetch

**How do I make her work?**

1. Stand in the middle of the field, the forest or the kitchen.
2. Run `/laura job farmer`, `/laura job lumberjack` or `/laura job cook` (or use the Work tab).
3. Look at a chest and assign it, for example `/laura chest harvest`.

**The lumberjack or the farmer does nothing.**

- The game rule `mobGriefing` must be true.
- She does not work at night unless `work.workAtNight` is true.
- The lumberjack only fells trees that carry natural leaves.
- The work area is a circle of 10 blocks around the spot where you gave the order. Give a radius to change it: `/laura job farmer 16`.
- She does not work while she sulks, sleeps or sits.

**The cook does nothing.**
She needs raw food or meal ingredients (in her inventory, or in a chest assigned to `ingredients`), a smoker, a furnace or a lit campfire in the area, fuel (in her inventory or in a `fuel` chest), and a crafting table for the meals of `recipes.json`.

**She cannot find what I ask her to fetch.**
She searches 24 blocks around **her** (`fetch.radius`): items on the ground, then storage, then blocks she may harvest. Breaking blocks needs the `mobGriefing` game rule and only concerns the block tag `lauramod:fetch_harvestable`.

**How do I stop her from breaking blocks?**
Set the game rule `mobGriefing` to false, or set `fetch.breakBlocks` to false and do not give her the lumberjack and farmer jobs.

**How do I fulfil her wish for fireworks?**
Launch a firework rocket near her. Any rocket within 32 blocks around her counts, whoever launched it.

### Look and language

**My skin shows the default skin.**

- File: it must be a PNG of 64x64, 64x32 or a larger multiple, and at most `skins.maxSkinKb`.
- URL: the domain must be in `skins.urlDomainWhitelist`, and your client must allow downloads (`display.remoteSkins`).
- Upload: you must be allowed to change her look, and each player may keep 10 skin files on the server (`skins.maxUploadsPerPlayer`).
- A slow download shows the default skin until it is finished.

**My custom model does not show.**

- `models.allowCustomModels` (server) and `display.customModels` (client) must be true.
- The model must be smaller than `models.maxModelKb` on the server and `network.maxModelDownloadKb` on the client.
- Only cubes are read, 4096 at most.
- Look at the game log: an invalid model is reported there.

**My model does not move its arms and legs.**
Name the bones so that they are recognised (`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, or one of the names listed in [MODELS.md](MODELS.md)), or ship your own animations.

**How do I change the language she speaks?**
She uses the language of each player's game. To force one language for everybody, set `dialogue.forcedLanguage`. To add or change lines, see [LANGUAGES.md](LANGUAGES.md).

### Settings

**Where are the settings?**
In `config/lauramod/`. `lauramod-common.json` for the server or your singleplayer world, `lauramod-client.json` for your display. Apply server changes with `/laura reload`.

**How do I restore a default value?**
Delete the line of the option, or the whole file. It is rewritten with defaults.

---

## Français

### Premiers pas

**Comment obtenir une compagne ?**
Trois moyens : dire « je me sens seul » dans le chat, lancer `/laura summon`, ou faire cuire une tulipe rose dans un four et utiliser le Cœur de Laura obtenu.

**J'ai dit la phrase et rien ne s'est passé.**
Vérifiez ces points :

- La phrase doit être écrite dans la langue de votre jeu, ou en anglais.
- `summoning.chatSummon` doit valoir true, et il vous faut le niveau de permission de `summoning.summonPermissionLevel`.
- Deux nouvelles invocations doivent être espacées de 5 secondes.
- Vous avez peut-être déjà le maximum (3 par défaut). Dans ce cas votre compagne la plus proche à moins de 64 blocs répond qu'elle est déjà là.
- Le monde a peut-être atteint `general.maxPerWorld`.

**Combien de compagnes puis-je avoir ?**
3 par défaut (`general.maxPerPlayer`). La deuxième et les suivantes reçoivent les noms de `general.extraNames`. Le Cœur de Laura ne crée que votre première compagne : demandez les suivantes avec `/laura summon` ou la phrase du chat.

**Comment la renommer ?**
`/laura name <nom>`, le crayon à côté de son nom dans le menu, ou une étiquette.

### Lui parler et lui donner des ordres

**Elle ne réagit pas à ce que j'écris.**

- Elle ne vous entend que dans un rayon de 64 blocs et dans la même dimension (`dialogue.chatRange`).
- Elle écoute la langue de votre jeu et l'anglais. Lancez `/laura lang` pour voir quelle langue elle utilise avec vous.
- Avec `dialogue.requireName` il faut écrire son nom.
- Quand elle est bâillonnée, chaque réponse est un son étouffé.
- Seul son partenaire peut lui donner des ordres, sauf si `permissions.othersCanInteract` vaut true.

**Elle refuse mes ordres.**
Elle le fait quand elle est triste, en colère, jalouse, boudeuse ou affamée, et seulement avec les niveaux `NEEDY` et `UNBEARABLE` de `needs.annoyance`. Redonnez le même ordre dans les 20 secondes et elle obéit. Pour l'empêcher définitivement, mettez `needs.refuseOrders` à false.

**Elle boude. Que faire ?**
Écrivez « pardon » dans le chat, réalisez son désir en cours, ou attendez quelques minutes (`personality.sulkMinutes`).

**Elle est trop exigeante.**
Dans `lauramod-common.json` :

| Objectif | Réglage |
|---|---|
| Plus calme en général | `needs.annoyance` : `CHILL` ou `NORMAL` |
| Aucun besoin | `needs.enabled` : false |
| Aucun désir | `needs.desires` : false |
| Moins de messages aléatoires | `personality.chatterMinutes` : un nombre plus grand, ou 0 |
| Pas de question d'amour | `personality.loveCheckMinutes` : 0 |
| Pas de son de pet, pas de coup taquin | `personality.farts` : false, `personality.playfulHit` : false |

**Comment la faire taire un moment ?**
Faites un clic droit sur elle avec une botte de foin. Retirez-la avec des cisailles, ou `/laura ungag`. Pour masquer ses bulles seulement chez vous, utilisez l'onglet Réglages de son menu.

**Comment écrire un objet ou un nom de fichier dans une commande ?**
Tel quel, sans guillemets : `/laura fetch minecraft:oak_log 16`, `/laura fetch #minecraft:logs`, `/laura fetch bread`, `/laura skin file uploads/my_skin`. Dans une commande fetch, la quantité et le mot `queue` se placent à la fin de la ligne.

### La suivre et la retrouver

**Elle ne se téléporte plus vers moi.**
C'est voulu en 2.0.0. Elle marche, et ne se téléporte qu'au-delà de 128 blocs (`follow.teleportDistance`). Quand elle est bloquée, elle vous le dit. Utilisez `/laura come`, baissez `follow.teleportDistance`, ou mettez `follow.teleportWhenStuck` à true.

**Où est-elle ?**
`/laura where` donne la position, la dimension et l'état de chaque compagne. `/laura come` appelle la compagne sélectionnée depuis n'importe où. Le Cœur de Laura fait de même et la rend heureuse.

**Comment rappeler une compagne restée loin ?**
`/laura come`, la touche d'appel et le Cœur de Laura ramènent les compagnes que vous avez déjà, même depuis des chunks déchargés ou après avoir été congédiées. Ils ne créent une compagne que si vous n'en avez aucune.

**Comment me séparer d'une compagne ?**
`/laura dismiss` la met de côté : elle perd 20 points d'affection, compte toujours dans votre limite, et revient à la prochaine invocation. `/laura release`, puis `/laura release confirm`, la retire pour de bon : elle lâche tout ce qu'elle porte et rien ne la ramène.

**Me suit-elle dans le Nether et l'End ?**
Oui, en mode suivre (`follow.followAcrossDimensions`). Elle est aussi ramenée après un waystone, une commande de téléportation ou une perle de l'Ender.

**Peut-elle disparaître ?**
Non. Elle ne disparaît jamais, et le monde garde une fiche de chaque compagne. Si elle est introuvable quand vous l'appelez, elle est restaurée à partir de son dernier état enregistré.

### Vie, mort et inventaire

**Elle ne régénère pas sa vie.**
Il n'y a pas de régénération gratuite par défaut. Nourrissez-la : avec une faim de 60 ou plus elle récupère 0,5 point de vie par seconde. Pour retrouver l'ancien comportement, mettez `general.regenPerSecond` au-dessus de 0.

**Elle est morte. Comment la récupérer ?**
Avec le mode par défaut (`GRAVE`) : fabriquez une Tombe de Laura (une pierre taillée en haut, trois pierres taillées au milieu, trois dalles de pierres en bas), posez-la n'importe où et faites un clic droit dessus avec une fleur. Elle revient avec son inventaire. Voir [ACTIONS.md](ACTIONS.md) pour les autres modes.

**Comment lui donner une armure, une arme ou un sac à dos ?**
Accroupissez-vous et faites un clic droit sur elle pour ouvrir son inventaire : quatre emplacements d'armure, les deux mains et un emplacement de dos. Un sac à dos peut aussi être donné par un simple clic droit.

**Perd-elle ses objets en mourant ?**
Non avec `GRAVE` et `TIMER`. Oui avec `NONE` : elle lâche tout.

### Travail et recherche d'objets

**Comment la faire travailler ?**

1. Placez-vous au milieu du champ, de la forêt ou de la cuisine.
2. Lancez `/laura job farmer`, `/laura job lumberjack` ou `/laura job cook` (ou utilisez l'onglet Travail).
3. Regardez un coffre et attribuez-le, par exemple `/laura chest harvest`.

**La bûcheronne ou la fermière ne fait rien.**

- La règle de jeu `mobGriefing` doit valoir true.
- Elle ne travaille pas la nuit, sauf si `work.workAtNight` vaut true.
- La bûcheronne n'abat que les arbres qui portent des feuilles naturelles.
- La zone de travail est un cercle de 10 blocs autour de l'endroit où vous avez donné l'ordre. Indiquez un rayon pour le changer : `/laura job farmer 16`.
- Elle ne travaille pas quand elle boude, dort ou est assise.

**La cuisinière ne fait rien.**
Il lui faut de la nourriture crue ou des ingrédients de plats (dans son inventaire, ou dans un coffre attribué à `ingredients`), un fumoir, un fourneau ou un feu de camp allumé dans la zone, du combustible (dans son inventaire ou dans un coffre `fuel`), et un établi pour les plats de `recipes.json`.

**Elle ne trouve pas ce que je lui demande de rapporter.**
Elle cherche dans un rayon de 24 blocs autour d'**elle** (`fetch.radius`) : les objets au sol, puis les rangements, puis les blocs qu'elle peut récolter. Casser des blocs demande la règle de jeu `mobGriefing` et ne concerne que le tag de blocs `lauramod:fetch_harvestable`.

**Comment l'empêcher de casser des blocs ?**
Mettez la règle de jeu `mobGriefing` à false, ou mettez `fetch.breakBlocks` à false et ne lui donnez pas les métiers de bûcheronne et de fermière.

**Comment réaliser son désir de feux d'artifice ?**
Lancez une fusée de feu d'artifice près d'elle. Toute fusée dans un rayon de 32 blocs autour d'elle compte, peu importe qui l'a lancée.

### Apparence et langue

**Mon skin affiche le skin par défaut.**

- Fichier : ce doit être un PNG de 64x64, 64x32 ou d'un multiple plus grand, et d'au plus `skins.maxSkinKb`.
- URL : le domaine doit figurer dans `skins.urlDomainWhitelist`, et votre client doit autoriser les téléchargements (`display.remoteSkins`).
- Envoi : vous devez avoir le droit de changer son apparence, et chaque joueur peut garder 10 fichiers de skin sur le serveur (`skins.maxUploadsPerPlayer`).
- Un téléchargement lent affiche le skin par défaut jusqu'à ce qu'il soit terminé.

**Mon modèle personnalisé ne s'affiche pas.**

- `models.allowCustomModels` (serveur) et `display.customModels` (client) doivent valoir true.
- Le modèle doit être plus petit que `models.maxModelKb` sur le serveur et que `network.maxModelDownloadKb` sur le client.
- Seuls les cubes sont lus, 4096 au plus.
- Regardez le journal du jeu : un modèle invalide y est signalé.

**Mon modèle ne bouge ni les bras ni les jambes.**
Nommez les os pour qu'ils soient reconnus (`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, ou l'un des noms listés dans [MODELS.md](MODELS.md)), ou fournissez vos propres animations.

**Comment changer la langue qu'elle parle ?**
Elle utilise la langue du jeu de chaque joueur. Pour imposer une langue à tout le monde, réglez `dialogue.forcedLanguage`. Pour ajouter ou modifier des répliques, voir [LANGUAGES.md](LANGUAGES.md).

### Réglages

**Où sont les réglages ?**
Dans `config/lauramod/`. `lauramod-common.json` pour le serveur ou votre monde solo, `lauramod-client.json` pour votre affichage. Appliquez les changements côté serveur avec `/laura reload`.

**Comment rétablir une valeur par défaut ?**
Supprimez la ligne de l'option, ou le fichier entier. Il est réécrit avec les valeurs par défaut.
