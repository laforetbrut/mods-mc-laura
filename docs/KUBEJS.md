# KubeJS scripting

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [CONFIG.md](CONFIG.md), [LANGUAGES.md](LANGUAGES.md), [COMPATIBILITY.md](COMPATIBILITY.md).

---

## English

### What you get

When [KubeJS](https://kubejs.com/) is installed, server scripts receive:

| Name | Kind | Role |
|---|---|---|
| `Laura` | global binding | Functions to add content, read a companion's state and make her act. |
| `LauraEvents` | event group | Server events fired when something happens to a companion. |

The integration ships in the NeoForge 1.21.1 build (built against KubeJS 2101.7.2) and in the NeoForge, Forge and Fabric 1.20.1 builds (built against KubeJS 2001.6.5). See [COMPATIBILITY.md](COMPATIBILITY.md) for the targets without it.

Scripts go in `kubejs/server_scripts/`. Everything runs on the server.

### Script life cycle

- Before server scripts are loaded, everything scripts added earlier (content and listeners) is forgotten.
- After they are loaded, the mod reloads its tables on the next server tick, with the script content merged in.
- Script content is merged again at every `/laura reload`, so it is never lost.
- Script entries are added **after** the entries of the config files. For gifts the first matching entry wins: a script cannot override an item already listed in `gifts.json`.

### Content functions

Entries use the same format as the files of `config/lauramod` (see [CONFIG.md](CONFIG.md) and [LANGUAGES.md](LANGUAGES.md)). An entry can be a JavaScript object or a JSON text.

| Function | Entry |
|---|---|
| `Laura.addGift(entry)` | `{ match, affection, fun, tier, returnChance, food }` |
| `Laura.addFavoriteFood(item)` | Item id or `#tag`. |
| `Laura.addDislikedFood(item)` | Item id or `#tag`. |
| `Laura.addReturnGift(entry)` | `{ item, count, weight }` |
| `Laura.addDesiredItem(entry)` | `{ item, eat, weight }` |
| `Laura.addDesiredPlace(entry)` | `{ id, biomes, icon, weight }` |
| `Laura.addMeal(entry)` | `{ id, result, count, ingredients, returns }` |
| `Laura.addDialogue(locale, json)` | A whole dialogue file: `{ lines, intents, items, chests, connectors, everyone, spelling }`. |
| `Laura.addLine(locale, key, text)` | One more line for a dialogue key (`ambient`, `hug`, `summon`...). |
| `Laura.addChat(locale, intent, trigger, response)` | One more chat phrase for an intent, with an optional answer (empty text for none). New intent ids work too. |
| `Laura.addSpelling(locale, from, to)` | One more spelling rule. An empty `to` drops a filler word. |
| `Laura.reload()` | Reloads the config, dialogues and tables on the next server tick. |

Wherever an entry takes an item (`match`, the two food functions, `item` of a desired item, meal `ingredients`, `Laura.wish`), the three forms of the config files are accepted: an item id, an `#item_tag` or the built-in group `@music_disc` (any music disc, with or without mods).

### Query functions

| Function | Returns |
|---|---|
| `Laura.companions(player)` | The loaded companions of a player, nearest first. |
| `Laura.companion(player)` | The selected companion, or the nearest one. `null` when none is loaded. |
| `Laura.name(laura)` | Her name. |
| `Laura.mood(laura)` | `happy`, `in_love`, `neutral`, `bored`, `hungry`, `tired`, `sad`, `angry`, `jealous` or `sulking`. |
| `Laura.affection(laura)` | 0 to 1000. |
| `Laura.need(laura, need)` | 0 to 100. `need` is `hunger`, `energy`, `fun`, `attention` or `hygiene`. |

### Action functions

| Function | Effect |
|---|---|
| `Laura.summon(player)` | Same as `/laura summon`. |
| `Laura.say(laura, player, text)` | She says the text, with her name, in proximity chat and in her speech bubble. |
| `Laura.sayLine(laura, player, key)` | She says a dialogue line by key, in the player's language. Returns `false` when the key does not exist. |
| `Laura.addAffection(laura, amount)` | Adds (or removes, with a negative number) affection. |
| `Laura.setNeed(laura, need, value)` | Sets a need between 0 and 100. |
| `Laura.emote(laura, emote)` | Plays an emote by name. |
| `Laura.order(player, laura, action, argument)` | Gives an order as the player would from the menu. |
| `Laura.wish(laura, item, minutes)` | Gives her a wish for an item (id or `#tag`), to fulfil within the given minutes by giving her the item. It counts even if she eats it on the spot. |

Actions accepted by `Laura.order`:

| Action | Argument |
|---|---|
| `FOLLOW`, `STAY`, `WANDER`, `COME`, `STOP`, `HOME`, `SET_HOME`, `CLEAR_HOME` | none |
| `HUG`, `KISS`, `COMPLIMENT`, `EAT`, `SLEEP`, `WAKE_UP`, `UNGAG`, `INFO`, `INVENTORY`, `WORK` | none |
| `FETCH` | `item`, `item\|count` or `item\|count\|queue`, for example `minecraft:bread\|16` |
| `EMOTE` | An emote name. |
| `COMBAT` | `PASSIVE`, `DEFENSIVE` or `AGGRESSIVE` |
| `PICKUP` | `on` or `off` |
| `ANSWER` | `yes` or `no` |
| `RENAME` | The new name. |
| `JOB` | `LUMBERJACK:on:12`, `FARMER:off`, `ALL:off` |
| `TASK` | `CHOP_TREE:now`, `HARVEST:queue`, `COOK:now:10` (the last number is the radius) |
| `QUEUE` | `clear` or `remove:2` |
| `CHEST` | A chest purpose, or `remove`. Applies to the container the player looks at. |

The same checks as for a player apply: the player must be allowed to command her, and she may refuse when she is unhappy. As for a player, she never refuses an order when that player is out of earshot, and `HUG` and `KISS` follow their own rules (see [ACTIONS.md](ACTIONS.md)).

### Events

All events are server events. The event object has three properties:

| Property | Content |
|---|---|
| `event.laura` | The companion. |
| `event.player` | The player involved. Can be `null` (for example when she dies while her partner is offline). |
| `event.detail` | A text that depends on the event. |

| Event | When | `event.detail` |
|---|---|---|
| `LauraEvents.summon` | A new companion is summoned. | empty |
| `LauraEvents.death` | She dies. | empty |
| `LauraEvents.revive` | She is revived at a gravestone. | empty |
| `LauraEvents.desireFulfilled` | A desire is fulfilled. | `kind:target`, for example `item:minecraft:cake`, `place:beach`, `activity:hug` |
| `LauraEvents.desireFailed` | A desire runs out of time. | same as above |
| `LauraEvents.gift` | She receives a gift listed in the gift table. | The item id. |
| `LauraEvents.emote` | A player asks her for an emote. | The emote name. |
| `LauraEvents.chat` | She understands a chat message. | The intent id (`follow`, `hug`, `hello`...). |

`event.cancel()` in a `chat` listener stops her default reaction to that message. Cancelling the other events has no effect.

### Worked example

`kubejs/server_scripts/laura.js`:

```js
// Content, same format as the files of config/lauramod
Laura.addGift({ match: 'minecraft:emerald_block', affection: 60, fun: 30, tier: 'AMAZING', returnChance: 0.5 })
Laura.addFavoriteFood('minecraft:baked_potato')
Laura.addDesiredItem({ item: 'minecraft:melon_slice', eat: true, weight: 8 })
Laura.addMeal({ id: 'script_bread', result: 'minecraft:bread', count: 2, ingredients: { 'minecraft:wheat': 2 } })

// Dialogues
Laura.addLine('en_us', 'hug', 'A hug from a script!')
Laura.addChat('en_us', 'secret_word', 'open sesame', 'You know the secret word!')
Laura.addSpelling('en_us', 'wuv', 'love')

// A warmer welcome
LauraEvents.summon(event => {
  Laura.addAffection(event.laura, 50)
  Laura.say(event.laura, event.player, 'I was waiting for you!')
})

// React to a custom intent, and ignore insults
LauraEvents.chat(event => {
  if (event.detail == 'secret_word') {
    Laura.emote(event.laura, 'celebrate')
  }
  if (event.detail == 'insult') {
    Laura.say(event.laura, event.player, 'I did not hear that.')
    event.cancel()
  }
})

// After a trip, she wants a flower within 10 minutes
LauraEvents.desireFulfilled(event => {
  if (event.detail == 'place:beach') {
    Laura.wish(event.laura, 'minecraft:poppy', 10)
  }
})
```

### For Java mods

The same functions are static methods of `com.vyrriox.lauramod.api.LauraAPI`. Listeners are registered with `LauraAPI.listen(event, listener)`. Event names: `summon`, `death`, `revive`, `desire_fulfilled`, `desire_failed`, `gift`, `emote`, `chat`. Every method must be called on the server thread.

---

## Français

### Ce que vous obtenez

Quand [KubeJS](https://kubejs.com/) est installé, les scripts serveur reçoivent :

| Nom | Nature | Rôle |
|---|---|---|
| `Laura` | liaison globale | Fonctions pour ajouter du contenu, lire l'état d'une compagne et la faire agir. |
| `LauraEvents` | groupe d'événements | Événements serveur déclenchés quand il arrive quelque chose à une compagne. |

L'intégration est fournie dans la version NeoForge 1.21.1 (compilée avec KubeJS 2101.7.2) et dans les versions NeoForge, Forge et Fabric 1.20.1 (compilées avec KubeJS 2001.6.5). Voir [COMPATIBILITY.md](COMPATIBILITY.md) pour les cibles qui ne l'ont pas.

Les scripts vont dans `kubejs/server_scripts/`. Tout s'exécute sur le serveur.

### Cycle de vie des scripts

- Avant le chargement des scripts serveur, tout ce que les scripts avaient ajouté (contenu et écouteurs) est oublié.
- Après leur chargement, le mod recharge ses tables au tick serveur suivant, avec le contenu des scripts fusionné.
- Le contenu des scripts est fusionné de nouveau à chaque `/laura reload` : il n'est jamais perdu.
- Les entrées des scripts sont ajoutées **après** celles des fichiers de configuration. Pour les cadeaux, la première entrée qui correspond l'emporte : un script ne peut pas remplacer un objet déjà listé dans `gifts.json`.

### Fonctions de contenu

Les entrées utilisent le même format que les fichiers de `config/lauramod` (voir [CONFIG.md](CONFIG.md) et [LANGUAGES.md](LANGUAGES.md)). Une entrée peut être un objet JavaScript ou un texte JSON.

| Fonction | Entrée |
|---|---|
| `Laura.addGift(entry)` | `{ match, affection, fun, tier, returnChance, food }` |
| `Laura.addFavoriteFood(item)` | Identifiant d'objet ou `#tag`. |
| `Laura.addDislikedFood(item)` | Identifiant d'objet ou `#tag`. |
| `Laura.addReturnGift(entry)` | `{ item, count, weight }` |
| `Laura.addDesiredItem(entry)` | `{ item, eat, weight }` |
| `Laura.addDesiredPlace(entry)` | `{ id, biomes, icon, weight }` |
| `Laura.addMeal(entry)` | `{ id, result, count, ingredients, returns }` |
| `Laura.addDialogue(locale, json)` | Un fichier de dialogues entier : `{ lines, intents, items, chests, connectors, everyone, spelling }`. |
| `Laura.addLine(locale, key, text)` | Une réplique de plus pour une clé de dialogue (`ambient`, `hug`, `summon`...). |
| `Laura.addChat(locale, intent, trigger, response)` | Une phrase de chat de plus pour une intention, avec une réponse facultative (texte vide pour aucune). Les nouveaux identifiants d'intention fonctionnent aussi. |
| `Laura.addSpelling(locale, from, to)` | Une règle d'orthographe de plus. Un `to` vide supprime un mot de remplissage. |
| `Laura.reload()` | Recharge la configuration, les dialogues et les tables au tick serveur suivant. |

Partout où une entrée attend un objet (`match`, les deux fonctions d'aliments, `item` d'un objet désiré, les `ingredients` d'un plat, `Laura.wish`), les trois formes des fichiers de configuration sont acceptées : un identifiant d'objet, un `#tag_d_objets` ou le groupe intégré `@music_disc` (n'importe quel disque de musique, avec ou sans mods).

### Fonctions de lecture

| Fonction | Résultat |
|---|---|
| `Laura.companions(player)` | Les compagnes chargées d'un joueur, la plus proche en premier. |
| `Laura.companion(player)` | La compagne sélectionnée, ou la plus proche. `null` quand aucune n'est chargée. |
| `Laura.name(laura)` | Son nom. |
| `Laura.mood(laura)` | `happy`, `in_love`, `neutral`, `bored`, `hungry`, `tired`, `sad`, `angry`, `jealous` ou `sulking`. |
| `Laura.affection(laura)` | 0 à 1000. |
| `Laura.need(laura, need)` | 0 à 100. `need` vaut `hunger`, `energy`, `fun`, `attention` ou `hygiene`. |

### Fonctions d'action

| Fonction | Effet |
|---|---|
| `Laura.summon(player)` | Comme `/laura summon`. |
| `Laura.say(laura, player, text)` | Elle dit le texte, avec son nom, dans le chat de proximité et dans sa bulle. |
| `Laura.sayLine(laura, player, key)` | Elle dit une réplique par sa clé, dans la langue du joueur. Renvoie `false` quand la clé n'existe pas. |
| `Laura.addAffection(laura, amount)` | Ajoute (ou retire, avec un nombre négatif) de l'affection. |
| `Laura.setNeed(laura, need, value)` | Règle un besoin entre 0 et 100. |
| `Laura.emote(laura, emote)` | Joue une émote par son nom. |
| `Laura.order(player, laura, action, argument)` | Donne un ordre comme le joueur le ferait depuis le menu. |
| `Laura.wish(laura, item, minutes)` | Lui donne un désir pour un objet (identifiant ou `#tag`), à réaliser dans le délai indiqué en minutes en lui donnant l'objet. Cela compte même si elle le mange aussitôt. |

Actions acceptées par `Laura.order` :

| Action | Argument |
|---|---|
| `FOLLOW`, `STAY`, `WANDER`, `COME`, `STOP`, `HOME`, `SET_HOME`, `CLEAR_HOME` | aucun |
| `HUG`, `KISS`, `COMPLIMENT`, `EAT`, `SLEEP`, `WAKE_UP`, `UNGAG`, `INFO`, `INVENTORY`, `WORK` | aucun |
| `FETCH` | `objet`, `objet\|quantité` ou `objet\|quantité\|queue`, par exemple `minecraft:bread\|16` |
| `EMOTE` | Un nom d'émote. |
| `COMBAT` | `PASSIVE`, `DEFENSIVE` ou `AGGRESSIVE` |
| `PICKUP` | `on` ou `off` |
| `ANSWER` | `yes` ou `no` |
| `RENAME` | Le nouveau nom. |
| `JOB` | `LUMBERJACK:on:12`, `FARMER:off`, `ALL:off` |
| `TASK` | `CHOP_TREE:now`, `HARVEST:queue`, `COOK:now:10` (le dernier nombre est le rayon) |
| `QUEUE` | `clear` ou `remove:2` |
| `CHEST` | Un usage de coffre, ou `remove`. S'applique au conteneur que le joueur regarde. |

Les mêmes vérifications que pour un joueur s'appliquent : le joueur doit avoir le droit de la commander, et elle peut refuser quand elle est mécontente. Comme pour un joueur, elle ne refuse jamais un ordre quand ce joueur est hors de portée de voix, et `HUG` et `KISS` suivent leurs propres règles (voir [ACTIONS.md](ACTIONS.md)).

### Événements

Tous les événements sont des événements serveur. L'objet événement a trois propriétés :

| Propriété | Contenu |
|---|---|
| `event.laura` | La compagne. |
| `event.player` | Le joueur concerné. Peut valoir `null` (par exemple quand elle meurt alors que son partenaire est hors ligne). |
| `event.detail` | Un texte qui dépend de l'événement. |

| Événement | Quand | `event.detail` |
|---|---|---|
| `LauraEvents.summon` | Une nouvelle compagne est invoquée. | vide |
| `LauraEvents.death` | Elle meurt. | vide |
| `LauraEvents.revive` | Elle revient à la vie sur une tombe. | vide |
| `LauraEvents.desireFulfilled` | Un désir est réalisé. | `type:cible`, par exemple `item:minecraft:cake`, `place:beach`, `activity:hug` |
| `LauraEvents.desireFailed` | Un désir expire. | comme ci-dessus |
| `LauraEvents.gift` | Elle reçoit un cadeau listé dans la table des cadeaux. | L'identifiant de l'objet. |
| `LauraEvents.emote` | Un joueur lui demande une émote. | Le nom de l'émote. |
| `LauraEvents.chat` | Elle comprend un message du chat. | L'identifiant d'intention (`follow`, `hug`, `hello`...). |

`event.cancel()` dans un écouteur `chat` empêche sa réaction par défaut à ce message. Annuler les autres événements n'a aucun effet.

### Exemple complet

`kubejs/server_scripts/laura.js` :

```js
// Contenu, même format que les fichiers de config/lauramod
Laura.addGift({ match: 'minecraft:emerald_block', affection: 60, fun: 30, tier: 'AMAZING', returnChance: 0.5 })
Laura.addFavoriteFood('minecraft:baked_potato')
Laura.addDesiredItem({ item: 'minecraft:melon_slice', eat: true, weight: 8 })
Laura.addMeal({ id: 'script_bread', result: 'minecraft:bread', count: 2, ingredients: { 'minecraft:wheat': 2 } })

// Dialogues
Laura.addLine('fr_fr', 'hug', 'Un câlin venu d\'un script !')
Laura.addChat('fr_fr', 'secret_word', 'sésame ouvre toi', 'Tu connais le mot secret !')
Laura.addSpelling('fr_fr', 'jtdr', "je t'adore")

// Un accueil plus chaleureux
LauraEvents.summon(event => {
  Laura.addAffection(event.laura, 50)
  Laura.say(event.laura, event.player, 'Je t\'attendais !')
})

// Réagir à une intention personnalisée, et ignorer les insultes
LauraEvents.chat(event => {
  if (event.detail == 'secret_word') {
    Laura.emote(event.laura, 'celebrate')
  }
  if (event.detail == 'insult') {
    Laura.say(event.laura, event.player, 'Je n\'ai rien entendu.')
    event.cancel()
  }
})

// Après une sortie, elle veut une fleur dans les 10 minutes
LauraEvents.desireFulfilled(event => {
  if (event.detail == 'place:beach') {
    Laura.wish(event.laura, 'minecraft:poppy', 10)
  }
})
```

### Pour les mods Java

Les mêmes fonctions sont des méthodes statiques de `com.vyrriox.lauramod.api.LauraAPI`. Les écouteurs s'enregistrent avec `LauraAPI.listen(event, listener)`. Noms des événements : `summon`, `death`, `revive`, `desire_fulfilled`, `desire_failed`, `gift`, `emote`, `chat`. Chaque méthode doit être appelée sur le thread du serveur.
