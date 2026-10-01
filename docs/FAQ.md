# Frequently asked questions

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Guides : [ACTIONS.md](ACTIONS.md), [COMMANDS.md](COMMANDS.md), [CONFIG.md](CONFIG.md), [SKINS.md](SKINS.md), [MODELS.md](MODELS.md), [LANGUAGES.md](LANGUAGES.md), [KUBEJS.md](KUBEJS.md), [COMPATIBILITY.md](COMPATIBILITY.md), [BUILDING.md](BUILDING.md).

Issues and suggestions / Problèmes et suggestions : <https://github.com/laforetbrut/mods-mc-laura/issues>

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
- Over the void or over lava there is no safe place for her: nothing is created and you read "There is no safe place for her here. Stand on solid ground and call her again." The summon cooldown is not used up. The same message is shown when a dismissed companion, a revived one or one restored from her saved state has nowhere to stand. In `TIMER` mode she waits and tries again every 5 seconds until you stand somewhere safe.

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
- She only answers the chat of her own partner. `permissions.othersCanInteract` opens her menu and the right click to other players, not chat orders or `/laura` commands.

**She refuses my orders.**
She does that when she is sad, angry, jealous, sulking or hungry, and only with the `NEEDY` and `UNBEARABLE` levels of `needs.annoyance`. Give the same order again within 20 seconds and she obeys. An order sent from out of earshot (`/laura come` or the call key from far away or from another dimension) is never refused. To stop refusals for good, set `needs.refuseOrders` to false.

**She refuses my hug or my kiss.**
A hug is never refused because she is hungry, sad or jealous: it comforts her. She only pushes it away while she sulks or is still angry (after a hit, an insult, a gift or a food she hates): ask again within 20 seconds and she takes it. A kiss has to be earned: she refuses it while she is gagged, while she sulks or is still angry, and when affection is under 200. `needs.refuseOrders` does not change these two rules.

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
As it is, without quotes: `/laura fetch minecraft:oak_log 16`, `/laura fetch #minecraft:logs`, `/laura fetch bread`, `/laura skin file summer/beach`. In a fetch command the count and the word `queue` go at the end of the line.

**How do I ask for a precise quantity in chat?**
Write the number in the order: "bring me 32 bread". Without a number she brings up to one stack, 16 at most.

### Following and finding her

**She does not teleport to me any more.**
That is intended in 2.0.0. She walks, and teleports only beyond 128 blocks (`follow.teleportDistance`). When she is stuck she tells you. Use `/laura come`, lower `follow.teleportDistance`, or set `follow.teleportWhenStuck` to true.

**Where is she?**
`/laura where` gives the position, dimension and state of each companion. `/laura come` calls the selected one from anywhere. Laura's Heart does the same and makes her happy.

**How do I call back a companion that stayed far away?**
`/laura come`, the call key and Laura's Heart bring back the companions you already have, even from unloaded chunks or after a dismissal. They create a companion only when you have none. They work for every player, operator or not, including when she is in another dimension.

**I call her and she says she is stuck.**
She only teleports to a safe spot next to you: solid ground, the surface of the water when you swim or sit in a boat, or the ground below you when you fly. Over the void or a lake of lava there is none, so she stays where she is and tells you. Move to a place where she can stand and call her again. While she follows you she tries again by herself every 2 seconds.

**How do I part with a companion?**
`/laura dismiss` puts her away: she loses 20 affection, still counts toward your limit, and comes back at the next summon. `/laura release`, then `/laura release confirm`, removes her for good: she drops everything she carries and nothing brings her back.

**Does she follow me to the Nether and the End?**
Yes, in follow mode (`follow.followAcrossDimensions`). She arrives on a safe spot next to you, at once when you change dimension. She is also brought along after a waystone, a teleport command or an ender pearl. The Nether and End advancements are granted as soon as she is next to you there (16 blocks), however each of you got there.

**She went through the portal before me.**
A portal lets her through at once and holds a player for a few seconds. She waits for you on the other side for about 10 seconds. If you do not follow, she comes back to you. With `follow.followAcrossDimensions` set to false she does not take portals herself.

**Can she despawn?**
No. She never despawns, and the world keeps a record of every companion. If she cannot be found when you call her, she is restored from her last saved state. Only a call of yours does that (`/laura come`, the call key, Laura's Heart): the automatic recall of a companion who follows you never creates a copy.

**My world was played with a pre-release build of 2.0.0. Is there anything to clean up?**
Two things can remain from those builds. Recalls could leave a chunk force loaded for good: list them with `/forceload query` and free them with `/forceload remove`. The release only uses a temporary chunk ticket that is never saved. A companion could also exist twice, in two dimensions: the copy that loaded last is removed by itself after 5 seconds, with a warning in the server log.

**Does she fight other players?**
Only where the server allows fights between players. She never fights you, your other companions or your pets. She fights another player, or the companion or pet of another player, only when PvP is enabled and the two players are not teammates without friendly fire. Where those fights are forbidden the other side cannot hurt her either, creative mode players excepted. The PvP switch is the `pvp` setting of `server.properties` on Minecraft 1.20.1 and 1.21.1, and the `pvp` game rule on Minecraft 26.1.2. Set `combat.attackPlayers` to false to keep her out of every fight with players, and `combat.shieldWithoutPvp` to false to remove her protection.

### Health, death and inventory

**She does not regenerate.**
There is no free regeneration by default. Feed her: with hunger at 60 or more she heals 0.5 health per second. For the old behaviour set `general.regenPerSecond` above 0.

**Her hygiene is low. How do I wash her?**
Right click her with a water bucket: she gains 60 hygiene and the bucket comes back empty. She also bathes on her own in water within 14 blocks, and rain washes her. The "Squeaky Clean" advancement is granted when water really washes her, not only when she decides to bathe: a water bucket, 4 seconds in a row in water, or 20 seconds in a row in the rain, as long as her hygiene was under 100 % at the start.

**I gave her the same gift again and her affection did not move.**
The same kind of gift, or the same favourite food, makes her fonder only once every 300 seconds per companion (`needs.giftCooldownSeconds`). In between she still takes it, and a wish is still fulfilled. Set the option to 0 to remove the limit.

**I hug or kiss her again and again and her affection does not move.**
A hug makes her fonder only once every 60 seconds per companion, and so does a kiss (`needs.cuddleCooldownSeconds`). In between she still hugs and kisses back, her need for attention is still met, a wish for a hug or a kiss is still fulfilled and the advancements still count. Set the option to 0 to remove the limit.

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

- The game rule `mobGriefing` (`mob_griefing` on Minecraft 26.1.2) must be true.
- She does not work at night unless `work.workAtNight` is true.
- The lumberjack only fells natural trees: on soil, made of one kind of log, with natural leaves at the top (`work.onlyNaturalTrees`). Logs of a build that touch a tree are left standing.
- The work area is a circle of 10 blocks around the spot where you gave the order. Give a radius to change it: `/laura job farmer 16`.
- She does not work while she sulks, sleeps or sits.
- When she finds nothing to do she says so once, then looks again by herself after 1 minute, then 2, then every 4 minutes.

**The cook does nothing.**
She needs raw food or meal ingredients (in her inventory, or in a chest assigned to `ingredients`), a smoker, a furnace or a lit campfire in the area, fuel (in her inventory or in a `fuel` chest), and a crafting table for the meals of `recipes.json`. She only takes food out of a furnace: what somebody else smelts there stays in place.

**She does not use one of my chests.**
She leaves a container alone when it is locked (vanilla `Lock`) and neither she nor you hold its key, and when you could not open it yourself: spawn protection, world border, or a claim of a protection mod. A container that is not assigned to her is only used while you are in the same dimension. Such a container cannot be assigned either.

**She cannot find what I ask her to fetch.**
She searches 24 blocks around **her** (`fetch.radius`): items on the ground, then storage, then blocks she may harvest. Breaking blocks needs the `mobGriefing` game rule (`mob_griefing` on Minecraft 26.1.2) and only concerns the block tag `lauramod:fetch_harvestable`.

**How do I stop her from breaking blocks?**
Set the game rule `mobGriefing` (`mob_griefing` on Minecraft 26.1.2) to false, or set `fetch.breakBlocks` to false and do not give her the lumberjack and farmer jobs. The game rule also stops at once a lumberjack or farmer job she already has, and a task under way: the tree or the field is left as it is, she says so once and leaves the blocks alone.

**How do I fulfil her wish for fireworks?**
Launch a firework rocket near her. Any rocket within 32 blocks around her counts, whoever launched it.

### Look and language

**My skin shows the default skin.**

- File: it must be a PNG of 64x64, 64x32 or a larger multiple, and at most `skins.maxSkinKb`.
- URL: the domain must be in `skins.urlDomainWhitelist`, and your client must allow downloads (`display.remoteSkins`).
- Upload: you must be allowed to change her look, and each player may keep 10 skin files on the server (`skins.maxUploadsPerPlayer`). A player can send one file every 3 seconds.
- A slow download shows the default skin until it is finished. The server sends skins and models at 640 KB per second to each player, and your game asks again after 10 seconds without an answer.
- "Not so fast": a companion changes skin or model at most once every 2 seconds, and a player can ask for one player name skin every 5 seconds.

**My custom model does not show.**

- `models.allowCustomModels` (server) and `display.customModels` (client) must be true.
- The model must be smaller than `models.maxModelKb` on the server and `network.maxModelDownloadKb` on the client.
- Only cubes are read, 4096 at most. A model also has limits for bones, textures and nesting, listed in [MODELS.md](MODELS.md): beyond them it is refused.
- Look at the game log: an invalid model is reported there.

**I am disconnected with "My Girlfriend Laura is not installed on this server".**
The mod is needed on both sides. NeoForge and Forge refuse such a connection themselves. Where the loader lets it through (Fabric), the mod makes your game leave the server after 5 seconds with this message. Ask the server to install the mod, or remove it from your `mods` folder to play there.

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
- Au-dessus du vide ou de la lave il n'y a aucun endroit sûr pour elle : rien n'est créé et vous lisez « Il n'y a aucun endroit sûr pour elle ici. Pose-toi sur un sol solide et rappelle-la. » Le délai entre deux invocations n'est pas entamé. Le même message s'affiche quand une compagne congédiée, ramenée à la vie ou restaurée depuis son état sauvegardé n'a nulle part où se tenir. En mode `TIMER` elle attend et réessaie toutes les 5 secondes jusqu'à ce que vous soyez à un endroit sûr.

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
- Elle ne répond qu'au chat de son propre partenaire. `permissions.othersCanInteract` ouvre son menu et le clic droit aux autres joueurs, pas les ordres du chat ni les commandes `/laura`.

**Elle refuse mes ordres.**
Elle le fait quand elle est triste, en colère, jalouse, boudeuse ou affamée, et seulement avec les niveaux `NEEDY` et `UNBEARABLE` de `needs.annoyance`. Redonnez le même ordre dans les 20 secondes et elle obéit. Un ordre envoyé hors de portée de voix (`/laura come` ou la touche d'appel de loin ou depuis une autre dimension) n'est jamais refusé. Pour empêcher définitivement les refus, mettez `needs.refuseOrders` à false.

**Elle refuse mon câlin ou mon bisou.**
Un câlin n'est jamais refusé parce qu'elle a faim, qu'elle est triste ou jalouse : il la réconforte. Elle ne le repousse que lorsqu'elle boude ou est encore en colère (après un coup, une insulte, un cadeau ou un aliment qu'elle déteste) : redemandez dans les 20 secondes et elle l'accepte. Un bisou se mérite : elle le refuse quand elle est bâillonnée, quand elle boude ou est encore en colère, et quand l'affection est sous 200. `needs.refuseOrders` ne change pas ces deux règles.

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
Tel quel, sans guillemets : `/laura fetch minecraft:oak_log 16`, `/laura fetch #minecraft:logs`, `/laura fetch bread`, `/laura skin file summer/beach`. Dans une commande fetch, la quantité et le mot `queue` se placent à la fin de la ligne.

**Comment demander une quantité précise dans le chat ?**
Écrivez le nombre dans l'ordre : « apporte-moi 32 pains ». Sans nombre, elle rapporte jusqu'à une pile, 16 au plus.

### La suivre et la retrouver

**Elle ne se téléporte plus vers moi.**
C'est voulu en 2.0.0. Elle marche, et ne se téléporte qu'au-delà de 128 blocs (`follow.teleportDistance`). Quand elle est bloquée, elle vous le dit. Utilisez `/laura come`, baissez `follow.teleportDistance`, ou mettez `follow.teleportWhenStuck` à true.

**Où est-elle ?**
`/laura where` donne la position, la dimension et l'état de chaque compagne. `/laura come` appelle la compagne sélectionnée depuis n'importe où. Le Cœur de Laura fait de même et la rend heureuse.

**Comment rappeler une compagne restée loin ?**
`/laura come`, la touche d'appel et le Cœur de Laura ramènent les compagnes que vous avez déjà, même depuis des chunks déchargés ou après avoir été congédiées. Ils ne créent une compagne que si vous n'en avez aucune. Ils fonctionnent pour tous les joueurs, opérateurs ou non, y compris quand elle est dans une autre dimension.

**Je l'appelle et elle dit qu'elle est bloquée.**
Elle ne se téléporte qu'à un endroit sûr à côté de vous : un sol solide, la surface de l'eau quand vous nagez ou êtes en bateau, ou le sol sous vous quand vous volez. Au-dessus du vide ou d'un lac de lave il n'y en a pas : elle reste donc où elle est et vous le dit. Allez à un endroit où elle peut se tenir et rappelez-la. Tant qu'elle vous suit, elle réessaie d'elle-même toutes les 2 secondes.

**Comment me séparer d'une compagne ?**
`/laura dismiss` la met de côté : elle perd 20 points d'affection, compte toujours dans votre limite, et revient à la prochaine invocation. `/laura release`, puis `/laura release confirm`, la retire pour de bon : elle lâche tout ce qu'elle porte et rien ne la ramène.

**Me suit-elle dans le Nether et l'End ?**
Oui, en mode suivre (`follow.followAcrossDimensions`). Elle arrive à un endroit sûr à côté de vous, aussitôt quand vous changez de dimension. Elle est aussi ramenée après un waystone, une commande de téléportation ou une perle de l'Ender. Les progrès du Nether et de l'End sont accordés dès qu'elle est à côté de vous là-bas (16 blocs), quelle que soit la façon dont chacun y est arrivé.

**Elle a pris le portail avant moi.**
Un portail la laisse passer aussitôt et retient un joueur quelques secondes. Elle vous attend de l'autre côté pendant environ 10 secondes. Si vous ne suivez pas, elle revient près de vous. Avec `follow.followAcrossDimensions` à false elle ne prend plus les portails d'elle-même.

**Peut-elle disparaître ?**
Non. Elle ne disparaît jamais, et le monde garde une fiche de chaque compagne. Si elle est introuvable quand vous l'appelez, elle est restaurée à partir de son dernier état enregistré. Seul un appel de votre part fait cela (`/laura come`, la touche d'appel, le Cœur de Laura) : le rappel automatique d'une compagne qui vous suit ne crée jamais de copie.

**Mon monde a été joué avec une préversion de la 2.0.0. Y a-t-il quelque chose à nettoyer ?**
Deux choses peuvent rester de ces versions. Les rappels pouvaient laisser un chunk chargé de force pour de bon : listez-les avec `/forceload query` et libérez-les avec `/forceload remove`. La version publiée n'utilise qu'un ticket de chunk temporaire, jamais sauvegardé. Une compagne pouvait aussi exister en double, dans deux dimensions : la copie chargée en dernier est retirée d'elle-même après 5 secondes, avec un avertissement dans le journal du serveur.

**Se bat-elle contre les autres joueurs ?**
Seulement là où le serveur autorise les combats entre joueurs. Elle ne se bat jamais contre vous, vos autres compagnes ou vos animaux. Elle ne se bat contre un autre joueur, ou contre la compagne ou l'animal d'un autre joueur, que si le PvP est activé et que les deux joueurs ne sont pas coéquipiers sans tir ami. Là où ces combats sont interdits, l'autre camp ne peut pas la blesser non plus, sauf les joueurs en mode créatif. L'interrupteur du PvP est le réglage `pvp` de `server.properties` sur Minecraft 1.20.1 et 1.21.1, et la règle de jeu `pvp` sur Minecraft 26.1.2. Mettez `combat.attackPlayers` à false pour la tenir à l'écart de tout combat avec des joueurs, et `combat.shieldWithoutPvp` à false pour retirer sa protection.

### Vie, mort et inventaire

**Elle ne régénère pas sa vie.**
Il n'y a pas de régénération gratuite par défaut. Nourrissez-la : avec une faim de 60 ou plus elle récupère 0,5 point de vie par seconde. Pour retrouver l'ancien comportement, mettez `general.regenPerSecond` au-dessus de 0.

**Son hygiène est basse. Comment la laver ?**
Faites un clic droit sur elle avec un seau d'eau : elle gagne 60 points d'hygiène et le seau revient vide. Elle se baigne aussi d'elle-même dans de l'eau à moins de 14 blocs, et la pluie la lave. Le progrès « Toute propre » est accordé quand de l'eau la lave vraiment, pas seulement quand elle décide de se baigner : un seau d'eau, 4 secondes d'affilée dans l'eau, ou 20 secondes d'affilée sous la pluie, tant que son hygiène était sous 100 % au début.

**Je lui ai redonné le même cadeau et son affection n'a pas bougé.**
Un même type de cadeau, ou un même aliment préféré, ne la rend plus affectueuse qu'une fois toutes les 300 secondes par compagne (`needs.giftCooldownSeconds`). Entre-temps elle le prend quand même, et un désir est quand même réalisé. Mettez l'option à 0 pour retirer la limite.

**Je lui fais câlin sur câlin ou bisou sur bisou et son affection ne bouge pas.**
Un câlin ne la rend plus affectueuse qu'une fois toutes les 60 secondes par compagne, et un bisou de même (`needs.cuddleCooldownSeconds`). Entre-temps elle rend quand même le câlin et le bisou, son besoin d'attention est quand même comblé, un désir de câlin ou de bisou est quand même réalisé et les progrès comptent toujours. Mettez l'option à 0 pour retirer la limite.

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

- La règle de jeu `mobGriefing` (`mob_griefing` sur Minecraft 26.1.2) doit valoir true.
- Elle ne travaille pas la nuit, sauf si `work.workAtNight` vaut true.
- La bûcheronne n'abat que les arbres naturels : sur de la terre, faits d'une seule sorte de bûche, avec des feuilles naturelles au sommet (`work.onlyNaturalTrees`). Les bûches d'une construction qui touchent un arbre restent en place.
- La zone de travail est un cercle de 10 blocs autour de l'endroit où vous avez donné l'ordre. Indiquez un rayon pour le changer : `/laura job farmer 16`.
- Elle ne travaille pas quand elle boude, dort ou est assise.
- Quand elle ne trouve rien à faire, elle le dit une fois, puis regarde de nouveau d'elle-même après 1 minute, puis 2, puis toutes les 4 minutes.

**La cuisinière ne fait rien.**
Il lui faut de la nourriture crue ou des ingrédients de plats (dans son inventaire, ou dans un coffre attribué à `ingredients`), un fumoir, un fourneau ou un feu de camp allumé dans la zone, du combustible (dans son inventaire ou dans un coffre `fuel`), et un établi pour les plats de `recipes.json`. Elle ne sort que de la nourriture d'un fourneau : ce qu'un autre y fait cuire reste en place.

**Elle n'utilise pas l'un de mes coffres.**
Elle ne touche pas à un conteneur quand il est verrouillé (`Lock` du jeu de base) et que ni elle ni vous ne tenez sa clé, ni quand vous ne pourriez pas l'ouvrir vous-même : protection du spawn, bordure du monde, ou zone d'un mod de protection. Un conteneur qui ne lui est pas attribué n'est utilisé que si vous êtes dans la même dimension. Un tel conteneur ne peut pas non plus être attribué.

**Elle ne trouve pas ce que je lui demande de rapporter.**
Elle cherche dans un rayon de 24 blocs autour d'**elle** (`fetch.radius`) : les objets au sol, puis les rangements, puis les blocs qu'elle peut récolter. Casser des blocs demande la règle de jeu `mobGriefing` (`mob_griefing` sur Minecraft 26.1.2) et ne concerne que le tag de blocs `lauramod:fetch_harvestable`.

**Comment l'empêcher de casser des blocs ?**
Mettez la règle de jeu `mobGriefing` (`mob_griefing` sur Minecraft 26.1.2) à false, ou mettez `fetch.breakBlocks` à false et ne lui donnez pas les métiers de bûcheronne et de fermière. La règle de jeu arrête aussi tout de suite un métier de bûcheronne ou de fermière qu'elle a déjà, et une tâche en cours : l'arbre ou le champ est laissé tel quel, elle le dit une fois et ne touche plus aux blocs.

**Comment réaliser son désir de feux d'artifice ?**
Lancez une fusée de feu d'artifice près d'elle. Toute fusée dans un rayon de 32 blocs autour d'elle compte, peu importe qui l'a lancée.

### Apparence et langue

**Mon skin affiche le skin par défaut.**

- Fichier : ce doit être un PNG de 64x64, 64x32 ou d'un multiple plus grand, et d'au plus `skins.maxSkinKb`.
- URL : le domaine doit figurer dans `skins.urlDomainWhitelist`, et votre client doit autoriser les téléchargements (`display.remoteSkins`).
- Envoi : vous devez avoir le droit de changer son apparence, et chaque joueur peut garder 10 fichiers de skin sur le serveur (`skins.maxUploadsPerPlayer`). Un joueur peut envoyer un fichier toutes les 3 secondes.
- Un téléchargement lent affiche le skin par défaut jusqu'à ce qu'il soit terminé. Le serveur envoie les skins et les modèles à 640 Ko par seconde à chaque joueur, et votre jeu redemande après 10 secondes sans réponse.
- « Pas si vite » : une compagne change de skin ou de modèle au plus une fois toutes les 2 secondes, et un joueur peut demander un skin par pseudo toutes les 5 secondes.

**Mon modèle personnalisé ne s'affiche pas.**

- `models.allowCustomModels` (serveur) et `display.customModels` (client) doivent valoir true.
- Le modèle doit être plus petit que `models.maxModelKb` sur le serveur et que `network.maxModelDownloadKb` sur le client.
- Seuls les cubes sont lus, 4096 au plus. Un modèle a aussi des limites d'os, de textures et d'imbrication, listées dans [MODELS.md](MODELS.md) : au-delà, il est refusé.
- Regardez le journal du jeu : un modèle invalide y est signalé.

**Je suis déconnecté avec « My Girlfriend Laura n'est pas installé sur ce serveur ».**
Le mod est nécessaire des deux côtés. NeoForge et Forge refusent d'eux-mêmes une telle connexion. Là où le chargeur la laisse passer (Fabric), le mod fait quitter le serveur à votre jeu après 5 secondes avec ce message. Demandez au serveur d'installer le mod, ou retirez-le de votre dossier `mods` pour jouer là-bas.

**Mon modèle ne bouge ni les bras ni les jambes.**
Nommez les os pour qu'ils soient reconnus (`head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, ou l'un des noms listés dans [MODELS.md](MODELS.md)), ou fournissez vos propres animations.

**Comment changer la langue qu'elle parle ?**
Elle utilise la langue du jeu de chaque joueur. Pour imposer une langue à tout le monde, réglez `dialogue.forcedLanguage`. Pour ajouter ou modifier des répliques, voir [LANGUAGES.md](LANGUAGES.md).

### Réglages

**Où sont les réglages ?**
Dans `config/lauramod/`. `lauramod-common.json` pour le serveur ou votre monde solo, `lauramod-client.json` pour votre affichage. Appliquez les changements côté serveur avec `/laura reload`.

**Comment rétablir une valeur par défaut ?**
Supprimez la ligne de l'option, ou le fichier entier. Il est réécrit avec les valeurs par défaut.
