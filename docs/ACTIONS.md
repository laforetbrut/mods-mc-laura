# Actions and behaviour

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [COMMANDS.md](COMMANDS.md), [CONFIG.md](CONFIG.md), [LANGUAGES.md](LANGUAGES.md), [FAQ.md](FAQ.md).

---

## English

Option names such as `follow.teleportDistance` refer to `config/lauramod/lauramod-common.json`. Numbers given here are the defaults.

### 1. Meeting her

| Way | Details |
|---|---|
| Chat phrase | Say "I feel lonely" in chat (each language has its own phrase). Works from anywhere. Controlled by `summoning.chatSummon`. |
| Command | `/laura summon`. |
| Laura's Heart | Smelt a pink tulip in a furnace, then use the heart (right click). |
| Spawn egg | Creative only. Right click an unclaimed companion to make her yours. |

- A new companion follows you, gets the name `general.defaultName` (then the names of `general.extraNames`), the skin `skins.defaultSkin`, 500 affection, and offers a clickable rename button.
- She appears on a safe spot next to you: on the ground below you when you fly, at the surface of the water when you swim. Only when there is no such place (over the void) does she appear where you are.
- One player can have 3 companions (`general.maxPerPlayer`). `general.maxPerWorld` can limit the whole world.
- Two new summons by the same player must be 5 seconds apart (`summoning.summonCooldownSeconds`).
- Laura's Heart used while a companion is loaded calls her to you (she teleports if she is further than 12 blocks or in another dimension), gives 50 affection and 40 attention, and makes her happy. The heart is consumed. When there is no safe place for her next to you, she tells you so, stays where she is, and the heart is kept.
- With no companion loaded, Laura's Heart, `/laura come` and the call key bring back the companions you already have: loaded ones are teleported, the others are recalled from unloaded chunks, a dismissed one returns. A new companion is created only when you have none. More companions come from `/laura summon` or the chat phrase.
- `/laura dismiss` (or the Dismiss button of the Settings tab, clicked twice) removes her from the world and keeps her data. It costs 20 affection (`general.dismissAffectionPenalty`) and some attention. The next summon brings her back.
- `/laura release`, then `/laura release confirm`, makes her leave for good: she drops everything she carries and her record is deleted.

### 2. Talking to her

She reads the public chat. This is a proximity chat: she hears you, and everybody hears her, within 64 blocks in the same dimension (`dialogue.chatRange`).

- She listens in your game language plus English. `dialogue.matchAllLanguages` makes her listen in every language she knows.
- Case, accents and punctuation are ignored. Common shortcuts and mistakes are understood through the `spelling` rules of the language (see [LANGUAGES.md](LANGUAGES.md)).
- With `dialogue.requireName` you must write her name ("Laura, follow me").
- Talking to her raises her attention need a little.

**Chained orders.** Join several orders with a connector word: "bring me wood then come here". English connectors: `then`, `and then`, `after that`, `afterwards`, `next`, `and after`. The first order starts now. The following ones go to her to-do list when she is busy.

**What she understands** (English examples, every language file has its own phrases):

| You write | She does |
|---|---|
| "I feel lonely" | Summon. |
| "follow me", "stay here", "come here", "go play" | Follow, stay, come, wander. |
| "go home", "this is our home" | Go home, set the home where you stand. |
| "stop" | Stops what she is doing. |
| "bring me bread", "fetch wood" | Fetch. Without an item name she fetches the item you hold. A number written in the message is the quantity ("bring me 32 bread"). |
| "chop a tree", "harvest", "cook" | One task. |
| "be a lumberjack", "be a farmer", "be the cook" | Starts a job where you stand. |
| "stop working", "back to work" | Stops all jobs, or resumes them. |
| "this chest is for wood" | Assigns the container you look at (wood, harvest, seeds, ingredients, fuel, meals, pantry, storage). |
| "hug", "kiss" | Hug, kiss. |
| "you're beautiful", "I love you", "I missed you", "thank you" | Compliment. |
| "sorry" | Apology: ends sulking and anger. |
| "eat something", "go to sleep", "wake up" | Eat, sleep, wake up. |
| "what do you want", "how are you", "where are you" | She tells her desire, her mood, or answers. |
| "I'm hungry" | She gives you food from her inventory. |
| "yes", "no" | Answer to her question. |
| "dance", "wave", "clap", "twirl"... | Plays the emote. |
| "hello", "good night", "tell me a joke", "marry me"... | Small talk. |

If she does not understand a message that contains her name, she says so.

### 3. Hands, menu and keys

**Right click on her**, in this order:

| What you hold | Result |
|---|---|
| A lead, or a name tag that carries a name | Vanilla behaviour: the name tag renames her. A name tag without a name is an item like any other. |
| A gag item (hay bale) | Gags her. See section 12. |
| Shears while she is gagged, or empty hand while sneaking | Removes the gag. |
| A water bucket | Washes her: +60 hygiene, and the bucket comes back empty (it stays full in creative mode). |
| A backpack while her back is free | She wears it. |
| A trinket (with Curios) | She wears it. |
| Food, a gift, or what she wishes for | She eats it, accepts it, or stores the food for later. |
| Anything else while sneaking | Opens her inventory. |
| Empty hand | Opens her menu (`controls.menuOnRightClick`). |

**Her menu** (key K, or right click) has seven tabs:

| Tab | Content |
|---|---|
| Home | Hug, kiss, compliment, inventory, info. Her needs and her current desire with the time left. |
| Orders | Follow, stay, wander, come, go home, set home, sleep, wake up, eat, stop, remove gag, forget home. Combat mode. Item pickup switch. |
| Emotes | Every emote. |
| Work | Jobs page (start or stop a job, work radius from 3 to 24, back to work, stop every job). Errands page (chop, harvest, cook, the to-do list). Chests page (assign the container you were looking at when you opened the menu). Shift + click an errand to add it to the to-do list. |
| Fetch | Search an item, choose a count (1, 8, 16, 32, 64), go now or add to the list, or fetch the item you hold. |
| Style | Skins and models. See [SKINS.md](SKINS.md) and [MODELS.md](MODELS.md). |
| Settings | Rename, client display switches, open the config folder, call her, dismiss her. |

With several companions, two arrows at the top switch between them.

**Emote wheel** (key G): point at an emote and release the key, or click. The mouse wheel changes the page. Emotes: `wave`, `hug`, `kiss`, `dance`, `clap`, `laugh`, `cry`, `blush`, `facepalm`, `jump`, `bow`, `think`, `shrug`, `stomp`, `yawn`, `celebrate`, `snap`, `twirl`, `hum`, `stretch`, `tap_foot`, `blow_kiss`, `sneeze`, `hair_flip`, `check_nails`, `air_guitar`, `hiccup`, `pout`, `shiver`, `fan`.

**Needs overlay**: enable `display.showNeedsHud` (client) to see the face, name, health and needs of your nearest companion in the top left corner. A need under 20 blinks.

**Inventory**: 3 rows by default (`general.inventoryRows`), four armor slots, both hands and a back slot.

### 4. Orders and modes

| Mode | Behaviour |
|---|---|
| Follow | Walks to you when you are further than 7 blocks, stops at 3.5 blocks. |
| Stay | Sits where she is. |
| Wander | Walks within 16 blocks of where she was left (32 when she is bored). |
| Home | Lives around her home. See section 13. |
| Work | Does her jobs. See section 7. |

Teleport rules while following:

- She walks. She only teleports next to you beyond 128 blocks (`follow.teleportDistance`).
- When stuck for 10 seconds she tells you (`follow.complainWhenStuck`). She teleports in that case only if `follow.teleportWhenStuck` is true.
- `/laura come` (or "come here") makes her walk when a path exists, and teleport when none exists.
- When you change dimension, the companions in follow mode arrive with you at once (`follow.followAcrossDimensions`), a sulking one included. She stays behind when she is not in follow mode (stay, home, wander, work), when she sleeps or when she is fetching something. Once she is next to you in the Nether, the End or back in the Overworld, she comments on the place.
- A portal lets her through at once and holds you for a few seconds, so she may arrive before you. She then waits for you on the other side for about 10 seconds; if you do not come, she returns to you. With `follow.followAcrossDimensions` set to false she does not follow you to another dimension and does not take portals herself.
- After any other long teleport (waystone, command, ender pearl) she is brought to you within a few seconds, even from unloaded chunks.
- A companion left in unloaded chunks is recalled by loading the place where she was last seen: 5 by 5 chunks, for 15 seconds at most. Nothing stays loaded afterwards, and calling her twice starts a single recall.

**Safe teleports.** Every teleport puts her on a safe spot next to you: at the surface of the water when you swim or sit in a boat, on the ground below you when you fly. She takes no fall damage from it. When there is no safe place at all (the void, a lake of lava) she stays where she is: after a call (`/laura come`, the call key, Laura's Heart, `/laura home`) she tells you so, and while she follows you she tries again every 2 seconds.

**Refusals.** With `needs.refuseOrders`, when her mood is sad, angry, jealous, sulking or hungry, she may refuse follow, stay, wander, home, come, sleep and eat, a fetch asked from her menu, and an emote asked with `/laura emote`, from her menu or from the emote wheel. A fetch or an emote asked in chat, and `/laura fetch`, are never refused. The chance depends on `needs.annoyance` (0 % for CHILL and NORMAL, 15 % for NEEDY, 30 % for UNBEARABLE) and doubles while she sulks. Giving the same order again within 20 seconds always works. She only argues face to face: an order sent from out of earshot (further than `dialogue.chatRange` or from another dimension, with a command or the call key) is never refused. What she answers when she is sent home or cannot come to you reaches you even there.

**Hug and kiss.** They are not orders and her needs never count against them, whether you ask in chat, with a command, from the menu or from the emote wheel. A hug is never refused because she is hungry, sad or jealous: it comforts her. She only pushes it away while she sulks or is still angry (after a hit, an insult, a gift or a food she hates), and asking again within 20 seconds gets through. A kiss is refused while she is gagged, while she sulks or is still angry, and when affection is under 200.

**Combat modes**: passive (avoids monsters, never fights), defensive (fights what hurts you or what you attack), aggressive (also attacks monsters that come close, except creepers).

**Other players.** She never fights you, nor your other companions and your pets. She fights another player, or the companion or pet of another player, only when `combat.attackPlayers` is true, PvP is enabled on the server and the two players are not teammates without friendly fire. Where those fights are forbidden, the other side cannot hurt her either (`combat.shieldWithoutPvp`); creative mode players still can.

**Villagers.** While she follows you nearby, villagers within 5 blocks of her walk away, except when they sleep or trade (`personality.jealousOfVillagers`).

### 5. Fetch

"Bring me 16 bread", `/laura fetch bread 16`, or the Fetch tab. A number written in a chat order is the quantity; without one she brings up to one stack, 16 at most.

She looks, in this order, within 24 blocks (`fetch.radius`):

1. Items lying on the ground.
2. Storage blocks: chests, barrels and modded storage (`fetch.fromContainers`).
3. Blocks of the `lauramod:fetch_harvestable` tag that drop the item: logs, flowers, ripe crops and more (`fetch.breakBlocks`, needs the `mobGriefing` game rule, named `mob_griefing` on Minecraft 26.1.2, at most 20 blocks away). Harvested crops are replanted. The bottom of sugar cane, bamboo and cactus is kept.

She then walks back and gives you the items. After 60 seconds (`fetch.timeoutSeconds`) she brings what she has, or gives up. She does one fetch at a time: use `queue` to line up several requests.

What she already carries for a fetch is saved with her: when the fetch is cut short (her chunk unloads, she dies, is dismissed or changes dimension), the items go back into her inventory, and what does not fit is dropped at her feet.

**Containers she may open.** This holds for fetching, for her jobs and for the food she looks for when she is starving:

- A container locked with the vanilla `Lock` is never used, unless she or you hold its key in the main hand.
- A container you assigned to her (section 7) is hers to use.
- Any other container is only used on your behalf: you must be in the same dimension and be allowed to open it yourself (spawn protection, world border, and the claims of protection mods).

### 6. To-do list

- Tasks (fetch, chop a tree, harvest, cook, come, go home, follow, stay, back to work) run one after the other.
- The list holds 16 tasks. See it with `/laura queue list`, `/laura info` or the Errands page.
- A new chop, harvest or cook order replaces the current one, unless it is queued (chat connector, `/laura queue add`, Shift + click).
- "stop" clears the list.
- When the list is empty and she is in work mode, she goes back to her jobs.
- A chop, harvest or cook task that finds nothing to do says so once and is removed. The tasks of the same kind still waiting in the list are tried later (after 1 minute, then 2, 4 and at most 8) and without a word, while the other tasks go on. A new direct order is always tried at once.
- In the menu, the same errand or fetch request sent twice within half a second counts as one click.

### 7. Jobs and chests

A job is continuous work inside an area: a circle around the block where you stood, 10 blocks of radius by default, 24 at most (`work.defaultRadius`, `work.maxRadius`).

| Job | What she does | Needs |
|---|---|---|
| Lumberjack | Fells natural trees (logs and leaves), replants a sapling, stores the wood. Never touches logs used in builds. | `mobGriefing` game rule (`mob_griefing` on 26.1.2). |
| Farmer | Harvests ripe crops, replants, sows empty farmland, uses bone meal, stores the harvest. | `mobGriefing` game rule (`mob_griefing` on 26.1.2). |
| Cook | Cooks raw food in smokers, furnaces and lit campfires, prepares the meals of `recipes.json` at a crafting table, stores the dishes and keeps 4 for herself. | A cooking station in the area. A crafting table for meals. |

- She can hold several jobs and rotates between them.
- She does not work at night (`work.workAtNight`), while sulking, sleeping, sitting or fighting.
- Another order (follow, stay...) pauses the jobs. "back to work" or `/laura work` resumes them.
- When none of her jobs finds anything to do, she says so once and pauses: 1 minute, then 2, then 4 minutes at most, until a job keeps her busy again.
- The lumberjack and the farmer stop when the `mobGriefing` game rule is turned off, even for a job given earlier. The rule is read each time she starts a round of a job or a task: the round under way is finished first (it lasts as long as she finds trees or crops to work on), and `/laura stop` ends it at once. She then says so once and leaves the blocks alone. The cook goes on.
- With `work.onlyNaturalTrees`, a tree she fells stands on soil (dirt, grass and the other blocks of the dirt tag, or mangrove roots), is made of a single kind of log, spreads at most 6 blocks sideways from the foot of its trunk and carries at least 4 natural leaves in its upper half. She only takes the logs that lead up to a log topped by natural leaves, so a wall, a beam or a roof of logs that touches the tree stays in place. A log pillar of a build with natural leaves growing right on top of it cannot be told from a trunk.
- As a cook she only takes food out of a furnace or a smoker: ingots, glass or charcoal smelted there by someone else stay where they are.

**Chests by purpose.** Look at a container and assign it (`/laura chest <purpose>`, "this chest is for ...", or the Chests page). She remembers 48 containers. A container you could not open yourself (locked, protected or claimed by someone else) cannot be assigned.

| Purpose | Use |
|---|---|
| `wood` | Lumberjack output: logs, saplings, apples, sticks. |
| `harvest` | Farmer output: crops. |
| `seeds` | Farmer input: seeds and bone meal. |
| `ingredients` | Cook input: raw food and meal ingredients. |
| `fuel` | Cook input: fuel for furnaces and smokers. |
| `meals` | Cook output: cooked food and meals. |
| `pantry` | Her own food. She eats from it when hungry. |
| `storage` | Everything that has no other place. |

Where a production goes: a chest of the right purpose with room, then a `storage` chest, then, if nothing is assigned for it and `work.depositInChests` is true, any container of the work area.

### 8. Needs

Five needs go from 100 (satisfied) to 0. Disable them all with `needs.enabled`.

| Need | Empties in | How to refill | What happens when low |
|---|---|---|---|
| Hunger | 40 min | Feed her by hand, or let her eat from her inventory or pantry chest. | Under 35 she eats on her own. At 12 or less she searches nearby storage. Under 8 she takes food from your inventory. At 0 she loses health. |
| Energy | 60 min | Sleep (about 3 minutes for a full night). | Under 20 she walks slower. Under 8 she falls asleep on the spot. |
| Fun | 30 min | Dancing, jukebox music, emotes, gifts, favourite food. | Under 30 she is bored. |
| Attention | 20 min | Talk to her, hug, kiss, compliment, gifts, stay close. | Under 25 she follows you much closer, then stands in front of you, waves and pokes. |
| Hygiene | 120 min | Water or rain, or a water bucket: right click her with it to wash her (+60, the bucket comes back empty). | At 25 or less she walks to water within 14 blocks for a bath. |

- Water washes her wherever it comes from: the bath she takes by herself, a pool you lead or push her into, the rain. After 4 seconds in a row in water, or 20 seconds in a row in the rain, she has had her bath, provided her hygiene was under 100 % when it started.
- With hunger at 60 or more she heals 0.5 health per second (`needs.healWhenFedPerSecond`). There is no free regeneration by default (`general.regenPerSecond` is 0).
- She reminds you when a need drops under 30, and more urgently under 15.
- `needs.annoyance` scales how fast needs drop and how often she reminds you.
- She sleeps at night when you sleep nearby, when she is at home, or when she is very tired.

### 9. Mood and relationship

**Moods**, from the highest priority: sulking, angry, jealous, hungry, tired, sad, bored, happy or in love, calm. The mood chooses her idle animation and her random lines.

**Affection** goes from 0 to 1000 and gives the relationship:

| Affection | Relationship |
|---|---|
| 0 to 149 | Hates you |
| 150 to 349 | Cold |
| 350 to 549 | Friendly |
| 550 to 749 | Close |
| 750 to 899 | In love |
| 900 to 1000 | Soulmates |

| Event | Affection |
|---|---|
| Hug, kiss | +4, +5 (+1 while she sulks) |
| Compliment | +3 |
| Desire fulfilled | +20 to +40 |
| Desire missed | -20, and she sulks 1 to 3 minutes |
| "Do you still love me?": yes, no, no answer | +10, -40 and sulking, -5 |
| Hitting her | -10, then -30 and sulking at the third hit |
| Insult | -15 |
| Gag | -15 |
| Dismissing her | -20 |
| Laura's Heart | +50 |
| Revival at her grave | +20 |
| Gifts | See `gifts.json` |

While she sulks she does not follow, work or fight. Apologize in chat ("sorry"), or fulfil a desire.

### 10. Desires

Every 12 minutes on average (`needs.desireMinutes`) she wishes for something, shown in a thought bubble. You have 15 minutes (`needs.desireDeadlineMinutes`). She reminds you as time runs out. The pool is `config/lauramod/desires.json`.

| Kind | How to fulfil |
|---|---|
| Item with `eat: true` | She must eat it: feed it to her by hand (right click). |
| Item with `eat: false` | Give it to her (right click). It counts even if she eats it on the spot. |
| Place | Take her into the biome or dimension. |
| Activity | See the table below. |

| Activity | Condition |
|---|---|
| `dance` | Ask her to dance, or let her dance near a jukebox. |
| `hug`, `kiss`, `compliment` | Do it. |
| `flowers` | Give her a small flower. |
| `new_outfit` | Change her skin or her model. |
| `sleep_together` | Sleep in a bed within 8 blocks of her. |
| `sunset` | 20 seconds at sunset, no rain, under the open sky, within 10 blocks of you. |
| `stargaze` | 30 seconds at night, no rain, under the open sky, within 10 blocks of you. |
| `music` | 20 seconds near a jukebox playing within 12 blocks. |
| `boat_ride` | 15 seconds in a boat. |
| `swim` | 8 seconds in water. |
| `pet` | 5 seconds with one of your tamed animals within 8 blocks of her. |
| `campfire` | 20 seconds at night within 4 blocks of a lit campfire. |
| `walk` | Walk 250 blocks with her following within 12 blocks. |
| `fireworks` | A firework rocket flies near her: within 32 blocks around, 64 above or below. Anybody can launch it. |

### 11. Gifts and food

Right click her with an item.

- **Food**: she eats it if she is hungry, if it is a favourite, or if she wishes for it. Otherwise it goes to her inventory for later. Raw meat and fish are kept for cooking unless she is starving.
- **Favourite food**: more fun, +5 affection. **Disliked food**: -10 affection and 20 seconds of anger.
- **Gift** (an entry of `gifts.json`): affection and fun change, she reacts according to the tier, and she may give something back. A `GROSS` gift makes her angry for 30 seconds and is not kept. An entry matches an item id, an `#item_tag` or the built-in group `@music_disc`: with the default file, any music disc is a gift, with or without mods.
- **Same gift again**: the same kind of gift, or the same favourite food, makes her fonder only once every 300 seconds per companion (`needs.giftCooldownSeconds`, 0 for no limit). In between she still takes the gift and eats the food, and a wish is still fulfilled, but she gains no affection and no fun, gives nothing back, and the gift does not count for the gift advancements. A gift she dislikes always counts.
- A gift is never lost: when her inventory is full it lands at her feet. What she gives you and does not fit in your inventory lands at your feet.
- She says one line for each item you give her: the gift she gives back, the wish it fulfils, her full inventory, or what she thinks of it.

Worked example of a gift entry (see [CONFIG.md](CONFIG.md) for every field):

```json
{ "match": "minecraft:emerald_block", "affection": 60, "fun": 30, "tier": "AMAZING", "returnChance": 0.5 }
```

She also gives: food when you are hurt and hungry (`personality.feedOwner`), and your items back when you die within 32 blocks of her (`personality.keepOwnerItemsOnDeath`, not with the `keepInventory` game rule, named `keep_inventory` on Minecraft 26.1.2).

### 12. The hay gag

The gag is only a way to keep her quiet for a while.

- Right click her with a hay bale (`gag.gagItems`). The item is consumed.
- While gagged, everything she says is replaced by a muffled line. She cannot eat or kiss and does not ask her love question.
- It costs 15 affection (`gag.affectionPenalty`) and 10 fun.
- Remove it with shears (`gag.ungagItems`), by sneaking and right clicking with an empty hand, with `/laura ungag`, or from the Orders tab. She removes it herself after 300 seconds (`gag.durationSeconds`, 0 for never).
- Disable the feature with `gag.enabled`.

### 13. Home

1. Stand where she should live and use `/laura home set`, or say "this is our home".
2. Send her there with `/laura home`, "go home", or the Orders tab.

At home she wanders within 12 blocks (`home.radius`), sleeps in a free bed within 10 blocks at night (`home.sleepAtNight`), and teleports back if she ends up further than 48 blocks (`home.teleportDistance`) or in another dimension. `/laura home clear` forgets the home.

`/laura home` works from anywhere, another dimension included, and her answer reaches you even out of earshot. She arrives on a safe spot next to her home; when there is none (the place is walled in or gone) she tells you so and stays where she is.

### 14. Death, grave and revival

`general.reviveMode` decides what happens when she dies:

| Mode | Result |
|---|---|
| `GRAVE` (default) | She waits. Craft a Laura's Gravestone, place it anywhere, and right click it with a flower (`general.reviveItems`). She comes back there with her inventory, her memories and +20 affection. |
| `TIMER` | She comes back next to you after 30 seconds (`general.respawnDelaySeconds`), with her belongings. |
| `NONE` | She is gone for good and drops her inventory and equipment. |

Laura's Gravestone recipe (crafting table): stone bricks `B`, cobblestone slabs `S`.

```
 B
BBB
SSS
```

- The flower is consumed. When several companions are dead, the one who died first comes back.
- While a companion is dead and no other is with you, Laura's Heart does not work and reminds you of the grave.
- With `GRAVE` and `TIMER` you are told about her death wherever you are, another dimension included.

### 15. Several companions

- Each companion is bound to the player who summoned her. Other players cannot give her orders, unless `permissions.othersCanInteract` is true: it opens her menu and the right click to them. Chat orders and `/laura` commands always stay reserved to her partner.
- A player who may not command her gets her refusal as a private message, at most once every 5 seconds: nobody else reads it and no speech bubble is shown.
- Commands and the menu act on the selected companion. Select one with `/laura list`, `/laura select <name>`, or by writing her name in chat.
- A chat message goes to the companion whose name it contains, to every companion in range with a word such as "everyone" (English words: `everyone`, `everybody`, `girls`, `all of you`, `you all`, `ladies`, `y'all`), otherwise to the selected or nearest one.
- `/laura where` lists them all. The position kept for each companion is refreshed every second while she is active, at once after a teleport or a change of dimension, and when her chunk unloads.
- `/laura dismiss` puts one away until the next summon. `/laura release`, then `/laura release confirm`, removes one for good.

### 16. Advancements

The tab "My Girlfriend Laura" holds 58 advancements. Ids are `lauramod:laura/<id>`. Counters are kept per player and survive restarts and companion changes.

| Title | How | Id |
|---|---|---|
| Never Alone Again | Meet your companion. | `root` |
| What's in a Name? | Rename her with `/laura name` or the menu. | `rename` |
| Warm Welcome | Hug her. | `first_hug` |
| First Kiss | Kiss her. | `first_kiss` |
| Just Friends | Reach 350 affection. | `relation_friend` |
| Getting Closer | Reach 550 affection. | `relation_close` |
| Head Over Heels | Reach 750 affection. | `relation_love` |
| Soulmates | Reach 900 affection. | `relation_soulmate` |
| Say Yes! (hidden) | Say "marry me" in chat. | `marry` |
| Happy Anniversary | 7 in-game days together. | `anniversary` |
| One Month Together | 30 days. | `days_30` |
| One Hundred Days | 100 days. | `days_100` |
| Forever Yours | 365 days. | `days_365` |
| Wish Granted | Fulfil a desire in time. | `desire_fulfilled` |
| Wish Maker | 10 desires. | `desire_10` |
| Fairy Godparent | 50 desires. | `desire_50` |
| Road Trip | Fulfil a place desire. | `desire_place` |
| Quality Time | Fulfil an activity desire. | `desire_activity` |
| Forgot Again (hidden) | Let a desire run out of time. | `desire_failed` |
| Room Service | Feed her by hand. | `feed` |
| You Know Me So Well | Give her a favourite food. | `favorite_food` |
| Yuck! (hidden) | Give her a food she dislikes. | `disliked_food` |
| Forgot Something? (hidden) | Let her get really hungry. | `starving` |
| Snack Thief (hidden) | Let her take food from your inventory. | `steal_food` |
| A Little Something | Give her a gift. | `gift_first` |
| Best Gift Ever | Give her an `AMAZING` gift. | `gift_amazing` |
| Spoiled Rotten | 50 gifts. | `gift_50` |
| It's the Thought that Counts | Receive a gift from her. | `gift_return` |
| Special Delivery | Have her bring you something. | `fetch` |
| Personal Shopper | 100 items fetched. | `fetch_100` |
| Employee of the Month | Give her a job. | `job` |
| To-Do List | Have 5 tasks waiting in her list. | `queue_5` |
| Tidy Home | Assign a chest for each of the 8 purposes. | `chests_all` |
| Timber! | Have her chop wood. | `task_chop_tree` |
| Lumber Queen | 100 logs. | `chop_100` |
| Harvest Moon | Have her harvest crops. | `task_harvest` |
| Golden Fields | 500 crops. | `harvest_500` |
| Chef Laura | Have her cook. | `task_cook` |
| Master Chef | 100 dishes. | `cook_100` |
| Show Me Your Moves | Ask for an emote. | `emote_first` |
| Choreographer | Ask for each of the 30 emotes. | `emote_all` |
| Our Song | Let her dance to a jukebox. | `dance_jukebox` |
| New Look | Change her skin. | `new_look` |
| Total Makeover | Give her a custom model. | `makeover` |
| Pack Mule | Give her a backpack to wear. | `backpack` |
| Put a Ring on It (hidden) | Give her a trinket to wear. | `curio` |
| Hot Date | Be next to her (16 blocks) in the Nether, however each of you got there: your portal, a portal she took herself, `/laura come`, the call key, Laura's Heart or a teleport command. | `nether` |
| Date at the End of the World | The same in the End. | `end` |
| Squeaky Clean | Water washes her: 4 seconds in a row in water or 20 seconds in the rain while her hygiene is under 100 %, or a water bucket you pour over her. | `bath` |
| Sweet Dreams | Let her sleep in a real bed. | `sleep_bed` |
| I'm Sorry, Okay? | Get forgiven with an apology. | `apology` |
| The Silent Treatment (hidden) | Make her sulk. | `sulk` |
| Heartbreaker (hidden) | Answer "no" to her love question. | `love_no` |
| Green-Eyed Girlfriend (hidden) | Make her jealous of another player. | `jealous` |
| Peace and Quiet (hidden) | Use the hay gag. | `gag` |
| Love Beyond the Grave (hidden) | Revive her with a flower on a gravestone. | `revive` |
| It's Complicated (hidden) | Have more than one companion. | `harem` |
| Full House (hidden) | Have three companions. | `harem_3` |

---

## Français

Les noms d'options comme `follow.teleportDistance` renvoient à `config/lauramod/lauramod-common.json`. Les nombres donnés ici sont les valeurs par défaut.

### 1. La rencontrer

| Moyen | Détails |
|---|---|
| Phrase dans le chat | Dites « je me sens seul » dans le chat (chaque langue a sa phrase). Fonctionne de partout. Réglé par `summoning.chatSummon`. |
| Commande | `/laura summon`. |
| Cœur de Laura | Faites cuire une tulipe rose dans un four, puis utilisez le cœur (clic droit). |
| Œuf d'apparition | Créatif uniquement. Faites un clic droit sur une compagne sans propriétaire pour qu'elle devienne la vôtre. |

- Une nouvelle compagne vous suit, reçoit le nom `general.defaultName` (puis les noms de `general.extraNames`), le skin `skins.defaultSkin`, 500 points d'affection, et propose un bouton cliquable pour la renommer.
- Elle apparaît à un endroit sûr à côté de vous : au sol sous vous quand vous volez, à la surface de l'eau quand vous nagez. C'est seulement quand un tel endroit n'existe pas (au-dessus du vide) qu'elle apparaît là où vous êtes.
- Un joueur peut avoir 3 compagnes (`general.maxPerPlayer`). `general.maxPerWorld` peut limiter le monde entier.
- Deux nouvelles invocations par le même joueur doivent être espacées de 5 secondes (`summoning.summonCooldownSeconds`).
- Le Cœur de Laura utilisé alors qu'une compagne est chargée l'appelle à vous (elle se téléporte si elle est à plus de 12 blocs ou dans une autre dimension), donne 50 points d'affection et 40 d'attention, et la rend heureuse. Le cœur est consommé. Quand il n'y a aucun endroit sûr pour elle à côté de vous, elle vous le dit, reste où elle est, et le cœur est conservé.
- Sans compagne chargée, le Cœur de Laura, `/laura come` et la touche d'appel ramènent les compagnes que vous avez déjà : celles qui sont chargées sont téléportées, les autres sont rappelées depuis des chunks déchargés, une compagne congédiée revient. Une nouvelle compagne n'est créée que si vous n'en avez aucune. Les compagnes supplémentaires viennent de `/laura summon` ou de la phrase du chat.
- `/laura dismiss` (ou le bouton Congédier de l'onglet Réglages, cliqué deux fois) la retire du monde et conserve ses données. Cela coûte 20 points d'affection (`general.dismissAffectionPenalty`) et un peu d'attention. La prochaine invocation la ramène.
- `/laura release`, puis `/laura release confirm`, la fait partir pour de bon : elle lâche tout ce qu'elle porte et sa fiche est supprimée.

### 2. Lui parler

Elle lit le chat public. C'est un chat de proximité : elle vous entend, et tout le monde l'entend, dans un rayon de 64 blocs dans la même dimension (`dialogue.chatRange`).

- Elle écoute la langue de votre jeu plus l'anglais. `dialogue.matchAllLanguages` la fait écouter toutes les langues qu'elle connaît.
- La casse, les accents et la ponctuation sont ignorés. Les abréviations et fautes courantes sont comprises grâce aux règles `spelling` de la langue (voir [LANGUAGES.md](LANGUAGES.md)).
- Avec `dialogue.requireName` il faut écrire son nom (« Laura, suis-moi »).
- Lui parler remonte un peu son besoin d'attention.

**Ordres enchaînés.** Reliez plusieurs ordres par un mot de liaison : « apporte-moi du bois puis viens ici ». Mots de liaison français : `puis`, `et puis`, `ensuite`, `et ensuite`, `après`, `après ça`, `et après`. Le premier ordre démarre tout de suite. Les suivants vont dans sa liste de tâches quand elle est occupée.

**Ce qu'elle comprend** (exemples français, chaque fichier de langue a ses propres phrases) :

| Vous écrivez | Elle fait |
|---|---|
| « je me sens seul » | Invocation. |
| « suis-moi », « reste ici », « viens ici », « va jouer » | Suivre, rester, venir, se promener. |
| « rentre à la maison », « c'est notre maison » | Rentrer, fixer la maison là où vous êtes. |
| « stop », « arrête » | Arrête ce qu'elle fait. |
| « apporte-moi du pain », « va chercher du bois » | Rapporter. Sans nom d'objet elle rapporte l'objet que vous tenez. Un nombre écrit dans le message est la quantité (« apporte-moi 32 pains »). |
| « coupe un arbre », « récolte », « cuisine » | Une tâche. |
| « sois bûcheronne », « sois fermière », « sois cuisinière » | Démarre un métier là où vous êtes. |
| « arrête de travailler », « au travail » | Arrête tous les métiers, ou les reprend. |
| « ce coffre est pour le bois » | Attribue le conteneur que vous regardez (bois, récolte, graines, ingrédients, combustible, plats, garde-manger, stockage). |
| « câlin », « bisou » | Câlin, bisou. |
| Un compliment, « je t'aime », un remerciement | Compliment. |
| « pardon », « désolé » | Excuses : met fin à la bouderie et à la colère. |
| « mange quelque chose », « va dormir », « réveille-toi » | Manger, dormir, se réveiller. |
| « tu veux quoi », « ça va », « t'es où » | Elle dit son désir, son humeur, ou répond. |
| « j'ai faim » | Elle vous donne de la nourriture de son inventaire. |
| « oui », « non » | Réponse à sa question. |
| Le nom d'une émote (danser, saluer, applaudir...) | Joue l'émote. |
| Salutations, blagues, demande en mariage... | Bavardage. |

Si elle ne comprend pas un message qui contient son nom, elle le dit.

### 3. Mains, menu et touches

**Clic droit sur elle**, dans cet ordre :

| Ce que vous tenez | Résultat |
|---|---|
| Une laisse, ou une étiquette qui porte un nom | Comportement du jeu de base : l'étiquette la renomme. Une étiquette sans nom est un objet comme un autre. |
| Un objet bâillon (botte de foin) | La bâillonne. Voir la section 12. |
| Des cisailles quand elle est bâillonnée, ou main vide en étant accroupi | Retire le bâillon. |
| Un seau d'eau | La lave : +60 d'hygiène, et le seau revient vide (il reste plein en mode créatif). |
| Un sac à dos quand son dos est libre | Elle le porte. |
| Un bijou (avec Curios) | Elle le porte. |
| De la nourriture, un cadeau, ou ce qu'elle désire | Elle mange, accepte, ou range la nourriture pour plus tard. |
| Autre chose en étant accroupi | Ouvre son inventaire. |
| Main vide | Ouvre son menu (`controls.menuOnRightClick`). |

**Son menu** (touche K, ou clic droit) a sept onglets :

| Onglet | Contenu |
|---|---|
| Accueil | Câlin, bisou, compliment, inventaire, infos. Ses besoins et son désir en cours avec le temps restant. |
| Ordres | Suivre, rester, se promener, venir, rentrer, fixer la maison, dormir, réveiller, manger, stop, retirer le bâillon, oublier la maison. Mode de combat. Interrupteur de ramassage. |
| Émotes | Toutes les émotes. |
| Travail | Page des métiers (démarrer ou arrêter un métier, rayon de travail de 3 à 24, retour au travail, tout arrêter). Page des tâches (couper, récolter, cuisiner, la liste de tâches). Page des coffres (attribue le conteneur que vous regardiez en ouvrant le menu). Maj + clic sur une tâche l'ajoute à la liste. |
| Chercher | Chercher un objet, choisir une quantité (1, 8, 16, 32, 64), y aller tout de suite ou ajouter à la liste, ou rapporter l'objet que vous tenez. |
| Style | Skins et modèles. Voir [SKINS.md](SKINS.md) et [MODELS.md](MODELS.md). |
| Réglages | Renommer, interrupteurs d'affichage du client, ouvrir le dossier de configuration, l'appeler, la congédier. |

Avec plusieurs compagnes, deux flèches en haut passent de l'une à l'autre.

**Roue des émotes** (touche G) : visez une émote et relâchez la touche, ou cliquez. La molette change de page. Émotes : `wave`, `hug`, `kiss`, `dance`, `clap`, `laugh`, `cry`, `blush`, `facepalm`, `jump`, `bow`, `think`, `shrug`, `stomp`, `yawn`, `celebrate`, `snap`, `twirl`, `hum`, `stretch`, `tap_foot`, `blow_kiss`, `sneeze`, `hair_flip`, `check_nails`, `air_guitar`, `hiccup`, `pout`, `shiver`, `fan`.

**Cadre des besoins** : activez `display.showNeedsHud` (client) pour voir le visage, le nom, la vie et les besoins de votre compagne la plus proche en haut à gauche. Un besoin sous 20 clignote.

**Inventaire** : 3 rangées par défaut (`general.inventoryRows`), quatre emplacements d'armure, les deux mains et un emplacement de dos.

### 4. Ordres et modes

| Mode | Comportement |
|---|---|
| Suivre | Marche vers vous quand vous êtes à plus de 7 blocs, s'arrête à 3,5 blocs. |
| Rester | S'assoit sur place. |
| Se promener | Marche dans un rayon de 16 blocs autour de l'endroit où elle a été laissée (32 quand elle s'ennuie). |
| Maison | Vit autour de sa maison. Voir la section 13. |
| Travail | Fait ses métiers. Voir la section 7. |

Règles de téléportation quand elle suit :

- Elle marche. Elle ne se téléporte près de vous qu'au-delà de 128 blocs (`follow.teleportDistance`).
- Bloquée pendant 10 secondes, elle vous le dit (`follow.complainWhenStuck`). Elle ne se téléporte dans ce cas que si `follow.teleportWhenStuck` vaut true.
- `/laura come` (ou « viens ici ») la fait marcher quand un chemin existe, et se téléporter quand il n'y en a pas.
- Quand vous changez de dimension, les compagnes en mode suivre arrivent avec vous aussitôt (`follow.followAcrossDimensions`), y compris une compagne qui boude. Elle reste sur place quand elle n'est pas en mode suivre (rester, maison, se promener, travail), quand elle dort ou quand elle est partie chercher un objet. Une fois à côté de vous dans le Nether, dans l'End ou de retour dans l'Overworld, elle commente l'endroit.
- Un portail la laisse passer aussitôt et vous retient quelques secondes : elle peut donc arriver avant vous. Elle vous attend alors de l'autre côté pendant environ 10 secondes ; si vous ne venez pas, elle revient près de vous. Avec `follow.followAcrossDimensions` à false elle ne vous suit pas dans une autre dimension et ne prend plus les portails d'elle-même.
- Après toute autre téléportation lointaine (waystone, commande, perle de l'Ender) elle est ramenée près de vous en quelques secondes, même depuis des chunks déchargés.
- Une compagne restée dans des chunks déchargés est rappelée en chargeant l'endroit où elle a été vue pour la dernière fois : 5 chunks sur 5, pendant 15 secondes au plus. Rien ne reste chargé ensuite, et l'appeler deux fois ne lance qu'un seul rappel.

**Téléportations sûres.** Chaque téléportation la pose à un endroit sûr à côté de vous : à la surface de l'eau quand vous nagez ou êtes en bateau, au sol sous vous quand vous volez. Elle n'en subit aucun dégât de chute. Quand il n'y a aucun endroit sûr (le vide, un lac de lave) elle reste où elle est : après un appel (`/laura come`, la touche d'appel, le Cœur de Laura, `/laura home`) elle vous le dit, et tant qu'elle vous suit elle réessaie toutes les 2 secondes.

**Refus.** Avec `needs.refuseOrders`, quand son humeur est triste, en colère, jalouse, boudeuse ou affamée, elle peut refuser suivre, rester, se promener, maison, venir, dormir et manger, un objet à rapporter demandé par son menu, et une émote demandée avec `/laura emote`, par son menu ou par la roue des émotes. Un objet à rapporter ou une émote demandés dans le chat, et `/laura fetch`, ne sont jamais refusés. La chance dépend de `needs.annoyance` (0 % pour CHILL et NORMAL, 15 % pour NEEDY, 30 % pour UNBEARABLE) et double quand elle boude. Redonner le même ordre dans les 20 secondes marche toujours. Elle ne discute qu'en face à face : un ordre envoyé hors de portée de voix (au-delà de `dialogue.chatRange` ou depuis une autre dimension, par une commande ou la touche d'appel) n'est jamais refusé. Ce qu'elle répond quand elle est envoyée à la maison ou ne peut pas venir vous parvient même là.

**Câlin et bisou.** Ce ne sont pas des ordres et ses besoins ne jouent jamais contre eux, que vous les demandiez dans le chat, par une commande, par le menu ou par la roue des émotes. Un câlin n'est jamais refusé parce qu'elle a faim, qu'elle est triste ou jalouse : il la réconforte. Elle ne le repousse que lorsqu'elle boude ou est encore en colère (après un coup, une insulte, un cadeau ou un aliment qu'elle déteste), et le redemander dans les 20 secondes passe. Un bisou est refusé quand elle est bâillonnée, quand elle boude ou est encore en colère, et quand l'affection est sous 200.

**Modes de combat** : passive (évite les monstres, ne se bat jamais), defensive (combat ce qui vous blesse ou ce que vous attaquez), aggressive (attaque aussi les monstres qui s'approchent, sauf les creepers).

**Autres joueurs.** Elle ne se bat jamais contre vous, ni contre vos autres compagnes et vos animaux. Elle ne se bat contre un autre joueur, ou contre la compagne ou l'animal d'un autre joueur, que si `combat.attackPlayers` vaut true, que le PvP est activé sur le serveur et que les deux joueurs ne sont pas coéquipiers sans tir ami. Là où ces combats sont interdits, l'autre camp ne peut pas la blesser non plus (`combat.shieldWithoutPvp`) ; les joueurs en mode créatif le peuvent toujours.

**Villageois.** Quand elle vous suit de près, les villageois à moins de 5 blocs d'elle s'éloignent, sauf s'ils dorment ou commercent (`personality.jealousOfVillagers`).

### 5. Rapporter

« Apporte-moi 16 pains », `/laura fetch bread 16`, ou l'onglet Chercher. Un nombre écrit dans un ordre du chat est la quantité ; sans nombre, elle rapporte jusqu'à une pile, 16 au plus.

Elle cherche, dans cet ordre, dans un rayon de 24 blocs (`fetch.radius`) :

1. Les objets au sol.
2. Les blocs de rangement : coffres, tonneaux et rangements de mods (`fetch.fromContainers`).
3. Les blocs du tag `lauramod:fetch_harvestable` qui donnent l'objet : bûches, fleurs, cultures mûres et d'autres (`fetch.breakBlocks`, demande la règle de jeu `mobGriefing`, nommée `mob_griefing` sur Minecraft 26.1.2, à 20 blocs au plus). Les cultures récoltées sont replantées. Le pied de la canne à sucre, du bambou et du cactus est conservé.

Elle revient ensuite et vous donne les objets. Après 60 secondes (`fetch.timeoutSeconds`) elle rapporte ce qu'elle a, ou abandonne. Elle fait une seule recherche à la fois : utilisez `queue` pour enchaîner plusieurs demandes.

Ce qu'elle porte déjà pour une recherche est sauvegardé avec elle : quand la recherche est interrompue (son chunk se décharge, elle meurt, est congédiée ou change de dimension), les objets retournent dans son inventaire, et ce qui n'y rentre pas tombe à ses pieds.

**Les conteneurs qu'elle peut ouvrir.** Cela vaut pour les objets à rapporter, pour ses métiers et pour la nourriture qu'elle cherche quand elle meurt de faim :

- Un conteneur verrouillé par le `Lock` du jeu de base n'est jamais utilisé, sauf si elle ou vous tenez sa clé dans la main principale.
- Un conteneur que vous lui avez attribué (section 7) est à sa disposition.
- Tout autre conteneur n'est utilisé qu'en votre nom : vous devez être dans la même dimension et avoir vous-même le droit de l'ouvrir (protection du spawn, bordure du monde, et les zones des mods de protection).

### 6. Liste de tâches

- Les tâches (rapporter, couper un arbre, récolter, cuisiner, venir, rentrer, suivre, rester, retour au travail) s'exécutent l'une après l'autre.
- La liste contient 16 tâches. Consultez-la avec `/laura queue list`, `/laura info` ou la page des tâches.
- Un nouvel ordre couper, récolter ou cuisiner remplace celui en cours, sauf s'il est mis en file (mot de liaison dans le chat, `/laura queue add`, Maj + clic).
- « stop » vide la liste.
- Quand la liste est vide et qu'elle est en mode travail, elle retourne à ses métiers.
- Une tâche couper, récolter ou cuisiner qui ne trouve rien à faire le dit une fois et est retirée. Les tâches du même type encore en attente dans la liste sont essayées plus tard (après 1 minute, puis 2, 4 et au plus 8) et sans un mot, pendant que les autres tâches continuent. Un nouvel ordre direct est toujours essayé tout de suite.
- Dans le menu, la même tâche ou la même demande d'objet envoyée deux fois en une demi-seconde compte pour un seul clic.

### 7. Métiers et coffres

Un métier est un travail continu dans une zone : un cercle autour du bloc où vous étiez, 10 blocs de rayon par défaut, 24 au plus (`work.defaultRadius`, `work.maxRadius`).

| Métier | Ce qu'elle fait | Conditions |
|---|---|---|
| Bûcheronne | Abat les arbres naturels (bûches et feuilles), replante une pousse, range le bois. Ne touche jamais aux bûches des constructions. | Règle de jeu `mobGriefing` (`mob_griefing` sur la 26.1.2). |
| Fermière | Récolte les cultures mûres, replante, sème la terre labourée vide, utilise la poudre d'os, range la récolte. | Règle de jeu `mobGriefing` (`mob_griefing` sur la 26.1.2). |
| Cuisinière | Cuit la nourriture crue dans les fumoirs, les fourneaux et les feux de camp allumés, prépare les plats de `recipes.json` sur un établi, range les plats et en garde 4 pour elle. | Un poste de cuisson dans la zone. Un établi pour les plats. |

- Elle peut avoir plusieurs métiers et alterne entre eux.
- Elle ne travaille pas la nuit (`work.workAtNight`), ni quand elle boude, dort, est assise ou se bat.
- Un autre ordre (suivre, rester...) met les métiers en pause. « au travail » ou `/laura work` les reprend.
- Quand aucun de ses métiers ne trouve quoi que ce soit à faire, elle le dit une fois et fait une pause : 1 minute, puis 2, puis 4 minutes au plus, jusqu'à ce qu'un métier l'occupe de nouveau.
- La bûcheronne et la fermière s'arrêtent quand la règle de jeu `mobGriefing` est désactivée, même pour un métier donné auparavant. La règle est lue chaque fois qu'elle commence une tournée d'un métier ou une tâche : la tournée en cours est d'abord terminée (elle dure tant qu'elle trouve des arbres ou des cultures à travailler), et `/laura stop` l'arrête tout de suite. Elle le dit alors une fois et ne touche plus aux blocs. La cuisinière continue.
- Avec `work.onlyNaturalTrees`, un arbre qu'elle abat repose sur de la terre (terre, herbe et les autres blocs du tag dirt, ou des racines de palétuvier), est fait d'une seule sorte de bûche, s'étend d'au plus 6 blocs sur les côtés depuis le pied de son tronc et porte au moins 4 feuilles naturelles dans sa moitié haute. Elle ne prend que les bûches qui mènent à une bûche surmontée de feuilles naturelles : un mur, une poutre ou un toit en bûches qui touche l'arbre reste donc en place. Un pilier en bûches d'une construction sur lequel poussent directement des feuilles naturelles ne peut pas être distingué d'un tronc.
- Cuisinière, elle ne sort que de la nourriture d'un fourneau ou d'un fumoir : les lingots, le verre ou le charbon de bois qu'un autre y a fait cuire restent où ils sont.

**Coffres par usage.** Regardez un conteneur et attribuez-le (`/laura chest <usage>`, « ce coffre est pour ... », ou la page des coffres). Elle retient 48 conteneurs. Un conteneur que vous ne pourriez pas ouvrir vous-même (verrouillé, protégé ou revendiqué par quelqu'un d'autre) ne peut pas être attribué.

| Usage | Rôle |
|---|---|
| `wood` | Production de la bûcheronne : bûches, pousses, pommes, bâtons. |
| `harvest` | Production de la fermière : récoltes. |
| `seeds` | Réserve de la fermière : graines et poudre d'os. |
| `ingredients` | Réserve de la cuisinière : nourriture crue et ingrédients des plats. |
| `fuel` | Réserve de la cuisinière : combustible des fourneaux et fumoirs. |
| `meals` | Production de la cuisinière : nourriture cuite et plats. |
| `pantry` | Sa propre nourriture. Elle y mange quand elle a faim. |
| `storage` | Tout ce qui n'a pas d'autre place. |

Où va une production : un coffre du bon usage qui a de la place, puis un coffre `storage`, puis, si rien n'est attribué pour cela et que `work.depositInChests` vaut true, n'importe quel conteneur de la zone de travail.

### 8. Besoins

Cinq besoins vont de 100 (satisfait) à 0. Désactivez-les tous avec `needs.enabled`.

| Besoin | Se vide en | Comment le remplir | Ce qui se passe quand il est bas |
|---|---|---|---|
| Faim | 40 min | La nourrir à la main, ou la laisser manger dans son inventaire ou son coffre garde-manger. | Sous 35 elle mange seule. À 12 ou moins elle fouille les rangements proches. Sous 8 elle prend de la nourriture dans votre inventaire. À 0 elle perd de la vie. |
| Énergie | 60 min | Dormir (environ 3 minutes pour une nuit complète). | Sous 20 elle marche moins vite. Sous 8 elle s'endort sur place. |
| Amusement | 30 min | Danse, musique de jukebox, émotes, cadeaux, aliment préféré. | Sous 30 elle s'ennuie. |
| Attention | 20 min | Lui parler, câlin, bisou, compliment, cadeaux, rester près d'elle. | Sous 25 elle vous suit de beaucoup plus près, puis se plante devant vous, fait signe et vous pousse du doigt. |
| Hygiène | 120 min | Eau ou pluie, ou un seau d'eau : faites un clic droit sur elle avec pour la laver (+60, le seau revient vide). | À 25 ou moins elle marche vers de l'eau à moins de 14 blocs pour un bain. |

- L'eau la lave d'où qu'elle vienne : le bain qu'elle prend d'elle-même, un bassin où vous la menez ou la poussez, la pluie. Après 4 secondes d'affilée dans l'eau, ou 20 secondes d'affilée sous la pluie, elle a pris son bain, à condition que son hygiène ait été sous 100 % au début.
- Avec une faim de 60 ou plus elle récupère 0,5 point de vie par seconde (`needs.healWhenFedPerSecond`). Il n'y a pas de régénération gratuite par défaut (`general.regenPerSecond` vaut 0).
- Elle vous prévient quand un besoin passe sous 30, et avec plus d'insistance sous 15.
- `needs.annoyance` règle la vitesse de baisse des besoins et la fréquence de ses rappels.
- Elle dort la nuit quand vous dormez à proximité, quand elle est à la maison, ou quand elle est très fatiguée.

### 9. Humeur et relation

**Humeurs**, de la plus prioritaire à la moins prioritaire : boude, en colère, jalouse, affamée, fatiguée, triste, s'ennuie, heureuse ou amoureuse, calme. L'humeur choisit son animation de repos et ses répliques aléatoires.

**L'affection** va de 0 à 1000 et donne la relation :

| Affection | Relation |
|---|---|
| 0 à 149 | Te déteste |
| 150 à 349 | Distante |
| 350 à 549 | Amicale |
| 550 à 749 | Proche |
| 750 à 899 | Amoureuse |
| 900 à 1000 | Âmes sœurs |

| Événement | Affection |
|---|---|
| Câlin, bisou | +4, +5 (+1 quand elle boude) |
| Compliment | +3 |
| Désir réalisé | +20 à +40 |
| Désir manqué | -20, et elle boude 1 à 3 minutes |
| « Tu m'aimes encore ? » : oui, non, pas de réponse | +10, -40 et bouderie, -5 |
| La frapper | -10, puis -30 et bouderie au troisième coup |
| Insulte | -15 |
| Bâillon | -15 |
| La congédier | -20 |
| Cœur de Laura | +50 |
| Retour à la vie sur sa tombe | +20 |
| Cadeaux | Voir `gifts.json` |

Quand elle boude, elle ne suit pas, ne travaille pas et ne se bat pas. Excusez-vous dans le chat (« pardon »), ou réalisez un désir.

### 10. Désirs

Toutes les 12 minutes en moyenne (`needs.desireMinutes`) elle désire quelque chose, affiché dans une bulle de pensée. Vous avez 15 minutes (`needs.desireDeadlineMinutes`). Elle vous le rappelle à mesure que le temps passe. La liste se trouve dans `config/lauramod/desires.json`.

| Type | Comment le réaliser |
|---|---|
| Objet avec `eat: true` | Elle doit le manger : donnez-le-lui à la main (clic droit). |
| Objet avec `eat: false` | Donnez-le-lui (clic droit). Cela compte même si elle le mange aussitôt. |
| Lieu | Emmenez-la dans le biome ou la dimension. |
| Activité | Voir le tableau ci-dessous. |

| Activité | Condition |
|---|---|
| `dance` | Demandez-lui de danser, ou laissez-la danser près d'un jukebox. |
| `hug`, `kiss`, `compliment` | Faites-le. |
| `flowers` | Offrez-lui une petite fleur. |
| `new_outfit` | Changez son skin ou son modèle. |
| `sleep_together` | Dormez dans un lit à moins de 8 blocs d'elle. |
| `sunset` | 20 secondes au coucher du soleil, sans pluie, à ciel ouvert, à moins de 10 blocs de vous. |
| `stargaze` | 30 secondes la nuit, sans pluie, à ciel ouvert, à moins de 10 blocs de vous. |
| `music` | 20 secondes près d'un jukebox qui joue à moins de 12 blocs. |
| `boat_ride` | 15 secondes dans un bateau. |
| `swim` | 8 secondes dans l'eau. |
| `pet` | 5 secondes avec un de vos animaux apprivoisés à moins de 8 blocs d'elle. |
| `campfire` | 20 secondes la nuit à moins de 4 blocs d'un feu de camp allumé. |
| `walk` | Marchez 250 blocs pendant qu'elle vous suit à moins de 12 blocs. |
| `fireworks` | Une fusée de feu d'artifice vole près d'elle : dans un rayon de 32 blocs, 64 au-dessus ou en dessous. N'importe qui peut la lancer. |

### 11. Cadeaux et nourriture

Faites un clic droit sur elle avec un objet.

- **Nourriture** : elle la mange si elle a faim, si c'est un aliment préféré, ou si elle le désire. Sinon elle la range dans son inventaire pour plus tard. La viande et le poisson crus sont gardés pour la cuisine sauf si elle meurt de faim.
- **Aliment préféré** : plus d'amusement, +5 d'affection. **Aliment détesté** : -10 d'affection et 20 secondes de colère.
- **Cadeau** (une entrée de `gifts.json`) : l'affection et l'amusement changent, elle réagit selon le niveau du cadeau, et elle peut offrir quelque chose en retour. Un cadeau `GROSS` la met en colère pendant 30 secondes et n'est pas conservé. Une entrée désigne un identifiant d'objet, un `#tag_d_objets` ou le groupe intégré `@music_disc` : avec le fichier par défaut, n'importe quel disque de musique est un cadeau, avec ou sans mods.
- **Le même cadeau à nouveau** : un même type de cadeau, ou un même aliment préféré, ne la rend plus affectueuse qu'une fois toutes les 300 secondes par compagne (`needs.giftCooldownSeconds`, 0 pour aucune limite). Entre-temps elle prend quand même le cadeau et mange la nourriture, et un désir est quand même réalisé, mais elle ne gagne ni affection ni amusement, n'offre rien en retour, et le cadeau ne compte pas pour les progrès des cadeaux. Un cadeau qu'elle déteste compte toujours.
- Un cadeau n'est jamais perdu : quand son inventaire est plein, il tombe à ses pieds. Ce qu'elle vous offre et qui ne rentre pas dans votre inventaire tombe à vos pieds.
- Elle dit une seule réplique par objet que vous lui donnez : le cadeau qu'elle offre en retour, le désir qu'il réalise, son inventaire plein, ou ce qu'elle en pense.

Exemple complet d'une entrée de cadeau (voir [CONFIG.md](CONFIG.md) pour tous les champs) :

```json
{ "match": "minecraft:emerald_block", "affection": 60, "fun": 30, "tier": "AMAZING", "returnChance": 0.5 }
```

Elle donne aussi : de la nourriture quand vous êtes blessé et affamé (`personality.feedOwner`), et vos objets quand vous mourez à moins de 32 blocs d'elle (`personality.keepOwnerItemsOnDeath`, pas avec la règle de jeu `keepInventory`, nommée `keep_inventory` sur Minecraft 26.1.2).

### 12. Le bâillon de foin

Le bâillon est seulement un moyen de la faire taire un moment.

- Faites un clic droit sur elle avec une botte de foin (`gag.gagItems`). L'objet est consommé.
- Tant qu'elle est bâillonnée, tout ce qu'elle dit est remplacé par une réplique étouffée. Elle ne peut ni manger ni faire de bisou et ne pose pas sa question d'amour.
- Cela coûte 15 points d'affection (`gag.affectionPenalty`) et 10 d'amusement.
- Retirez-le avec des cisailles (`gag.ungagItems`), en vous accroupissant et en faisant un clic droit à main vide, avec `/laura ungag`, ou depuis l'onglet Ordres. Elle le retire elle-même après 300 secondes (`gag.durationSeconds`, 0 pour jamais).
- Désactivez la fonction avec `gag.enabled`.

### 13. Maison

1. Placez-vous là où elle doit vivre et utilisez `/laura home set`, ou dites « c'est notre maison ».
2. Envoyez-la là-bas avec `/laura home`, « rentre à la maison », ou l'onglet Ordres.

À la maison elle se promène dans un rayon de 12 blocs (`home.radius`), dort la nuit dans un lit libre à moins de 10 blocs (`home.sleepAtNight`), et se téléporte chez elle si elle se retrouve à plus de 48 blocs (`home.teleportDistance`) ou dans une autre dimension. `/laura home clear` oublie la maison.

`/laura home` fonctionne de partout, y compris depuis une autre dimension, et sa réponse vous parvient même hors de portée de voix. Elle arrive à un endroit sûr à côté de sa maison ; quand il n'y en a pas (l'endroit est muré ou a disparu) elle vous le dit et reste où elle est.

### 14. Mort, tombe et retour à la vie

`general.reviveMode` décide de ce qui se passe à sa mort :

| Mode | Résultat |
|---|---|
| `GRAVE` (par défaut) | Elle attend. Fabriquez une Tombe de Laura, posez-la n'importe où, et faites un clic droit dessus avec une fleur (`general.reviveItems`). Elle revient à cet endroit avec son inventaire, ses souvenirs et +20 d'affection. |
| `TIMER` | Elle revient près de vous après 30 secondes (`general.respawnDelaySeconds`), avec ses affaires. |
| `NONE` | Elle disparaît pour de bon et lâche son inventaire et son équipement. |

Recette de la Tombe de Laura (établi) : pierres taillées `B`, dalles de pierres `S`.

```
 B
BBB
SSS
```

- La fleur est consommée. Quand plusieurs compagnes sont mortes, celle qui est morte en premier revient.
- Tant qu'une compagne est morte et qu'aucune autre n'est avec vous, le Cœur de Laura ne fonctionne pas et vous rappelle la tombe.
- Avec `GRAVE` et `TIMER`, vous êtes prévenu de sa mort où que vous soyez, y compris dans une autre dimension.

### 15. Plusieurs compagnes

- Chaque compagne est liée au joueur qui l'a invoquée. Les autres joueurs ne peuvent pas lui donner d'ordres, sauf si `permissions.othersCanInteract` vaut true : l'option leur ouvre son menu et le clic droit. Les ordres du chat et les commandes `/laura` restent toujours réservés à son partenaire.
- Un joueur qui n'a pas le droit de lui donner d'ordres reçoit son refus en message privé, au plus une fois toutes les 5 secondes : personne d'autre ne le lit et aucune bulle ne s'affiche.
- Les commandes et le menu agissent sur la compagne sélectionnée. Sélectionnez-en une avec `/laura list`, `/laura select <nom>`, ou en écrivant son nom dans le chat.
- Un message du chat va à la compagne dont il contient le nom, à toutes les compagnes à portée avec un mot comme « tout le monde » (mots français : `tout le monde`, `les filles`, `vous toutes`, `vous tous`, `mesdames`, `tout le groupe`), sinon à la compagne sélectionnée ou à la plus proche.
- `/laura where` les liste toutes. La position gardée pour chaque compagne est rafraîchie chaque seconde tant qu'elle est active, aussitôt après une téléportation ou un changement de dimension, et quand son chunk se décharge.
- `/laura dismiss` en met une de côté jusqu'à la prochaine invocation. `/laura release`, puis `/laura release confirm`, en retire une pour de bon.

### 16. Progrès

L'onglet « My Girlfriend Laura » contient 58 progrès. Les identifiants sont `lauramod:laura/<id>`. Les compteurs sont conservés par joueur et survivent aux redémarrages et aux changements de compagne.

| Titre | Comment | Id |
|---|---|---|
| Plus jamais seul | Rencontrer votre compagne. | `root` |
| Un joli prénom | La renommer avec `/laura name` ou le menu. | `rename` |
| Accueil chaleureux | Lui faire un câlin. | `first_hug` |
| Premier bisou | Lui faire un bisou. | `first_kiss` |
| Simples amis | Atteindre 350 d'affection. | `relation_friend` |
| De plus en plus proches | Atteindre 550 d'affection. | `relation_close` |
| Coup de foudre | Atteindre 750 d'affection. | `relation_love` |
| Âmes sœurs | Atteindre 900 d'affection. | `relation_soulmate` |
| Dis oui ! (caché) | Dire « épouse-moi » dans le chat. | `marry` |
| Joyeux anniversaire | 7 jours de jeu ensemble. | `anniversary` |
| Un mois ensemble | 30 jours. | `days_30` |
| Cent jours | 100 jours. | `days_100` |
| Pour toujours | 365 jours. | `days_365` |
| Vœu exaucé | Réaliser un désir à temps. | `desire_fulfilled` |
| Faiseur de vœux | 10 désirs. | `desire_10` |
| Bonne fée | 50 désirs. | `desire_50` |
| Road trip | Réaliser un désir de lieu. | `desire_place` |
| Moments à deux | Réaliser un désir d'activité. | `desire_activity` |
| Encore oublié (caché) | Laisser un désir expirer. | `desire_failed` |
| Service en chambre | La nourrir à la main. | `feed` |
| Tu me connais si bien | Lui donner un aliment préféré. | `favorite_food` |
| Beurk ! (caché) | Lui donner un aliment qu'elle déteste. | `disliked_food` |
| Tu n'oublies rien ? (caché) | La laisser avoir très faim. | `starving` |
| Voleuse de goûter (caché) | La laisser prendre de la nourriture dans votre inventaire. | `steal_food` |
| Un petit quelque chose | Lui offrir un cadeau. | `gift_first` |
| Le plus beau cadeau | Lui offrir un cadeau `AMAZING`. | `gift_amazing` |
| Pourrie gâtée | 50 cadeaux. | `gift_50` |
| C'est l'intention qui compte | Recevoir un cadeau de sa part. | `gift_return` |
| Livraison spéciale | Lui faire rapporter quelque chose. | `fetch` |
| Coursière personnelle | 100 objets rapportés. | `fetch_100` |
| Employée du mois | Lui donner un métier. | `job` |
| Liste de tâches | Avoir 5 tâches en attente dans sa liste. | `queue_5` |
| Maison bien rangée | Attribuer un coffre à chacun des 8 usages. | `chests_all` |
| Attention, ça tombe ! | Lui faire couper du bois. | `task_chop_tree` |
| Reine des bûcherons | 100 bûches. | `chop_100` |
| Belle récolte | Lui faire récolter des cultures. | `task_harvest` |
| Champs dorés | 500 cultures. | `harvest_500` |
| Cheffe Laura | Lui faire cuisiner. | `task_cook` |
| Grande cheffe | 100 plats. | `cook_100` |
| Montre-moi tes pas | Demander une émote. | `emote_first` |
| Chorégraphe | Demander chacune des 30 émotes. | `emote_all` |
| Notre chanson | La laisser danser devant un jukebox. | `dance_jukebox` |
| Nouveau look | Changer son skin. | `new_look` |
| Relooking complet | Lui donner un modèle personnalisé. | `makeover` |
| Bête de somme | Lui donner un sac à dos à porter. | `backpack` |
| La bague au doigt (caché) | Lui offrir un bijou à porter. | `curio` |
| Rendez-vous brûlant | Être à côté d'elle (16 blocs) dans le Nether, quelle que soit la façon dont chacun y est arrivé : votre portail, un portail qu'elle a pris d'elle-même, `/laura come`, la touche d'appel, le Cœur de Laura ou une commande de téléportation. | `nether` |
| Rendez-vous au bout du monde | La même chose dans l'End. | `end` |
| Toute propre | L'eau la lave : 4 secondes d'affilée dans l'eau ou 20 secondes sous la pluie quand son hygiène est sous 100 %, ou un seau d'eau que vous lui versez dessus. | `bath` |
| Fais de beaux rêves | La laisser dormir dans un vrai lit. | `sleep_bed` |
| Pardon, d'accord ? | Se faire pardonner avec des excuses. | `apology` |
| La bouderie (caché) | La faire bouder. | `sulk` |
| Cœur brisé (caché) | Répondre « non » à sa question d'amour. | `love_no` |
| Jalousie maladive (caché) | La rendre jalouse d'un autre joueur. | `jealous` |
| Enfin le calme (caché) | Utiliser le bâillon de foin. | `gag` |
| L'amour au-delà de la tombe (caché) | La ramener avec une fleur sur une tombe. | `revive` |
| C'est compliqué (caché) | Avoir plus d'une compagne. | `harem` |
| Maison pleine (caché) | Avoir trois compagnes. | `harem_3` |
