# Languages and dialogues

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [ACTIONS.md](ACTIONS.md), [CONFIG.md](CONFIG.md), [KUBEJS.md](KUBEJS.md).

---

## English

### Two kinds of text

| Text | Where it lives | Side | How to customise |
|---|---|---|---|
| What she says and what she understands in chat | Dialogue files | server | `config/lauramod/dialogues/<language>.json` |
| Interface: menu, items, advancements, system messages | Language files | client | `config/lauramod/lang/<language>.json` or a resource pack |

No code and no resource pack is needed for either of them.

### Built-in languages

18 languages ship with the mod, for both kinds of text:

`cs_cz`, `de_de`, `en_us`, `es_es`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `nl_nl`, `pl_pl`, `pt_br`, `ru_ru`, `sv_se`, `tr_tr`, `uk_ua`, `zh_cn`, `zh_tw`.

The summoning phrase of each language:

| Language | Phrase | Language | Phrase |
|---|---|---|---|
| `cs_cz` | cítím se osaměl | `nl_nl` | ik voel me eenzaam |
| `de_de` | ich fühle mich einsam | `pl_pl` | czuję się samotnie |
| `en_us` | i feel lonely | `pt_br` | me sinto sozinho |
| `es_es` | me siento solo | `ru_ru` | мне одиноко |
| `fr_fr` | je me sens seul | `sv_se` | jag är ensam |
| `id_id` | aku kesepian | `tr_tr` | yalnızım |
| `it_it` | mi sento solo | `uk_ua` | мені самотньо |
| `ja_jp` | 寂しい | `zh_cn` | 我好孤单 |
| `ko_kr` | 외로워 | `zh_tw` | 我好孤單 |

### Which language she uses

She talks to each player in the language of that player's game. For each line she tries, in order:

1. The exact language code of the player (for example `fr_ca`).
2. A close regional variant (`fr_ca` then `fr_fr`, `es_ar` then `es_mx` then `es_es`, `en_au` then `en_gb` then `en_us`, `zh_hk` then `zh_tw`, `pt_pt` then `pt_br`).
3. The main variant of the language.
4. `dialogue.fallbackLanguage` (default `en_us`).
5. `en_us`.

The search is done line by line: a language file that only has a few lines falls back to the next language for the others.

| Option | Effect |
|---|---|
| `dialogue.forcedLanguage` | Forces one dialogue language for everyone, for example `fr_fr`. |
| `dialogue.matchAllLanguages` | False: she listens to the languages of the player's own chain plus English. True: to every language she knows. |
| `dialogue.customDialoguesReplace` | True: every custom dialogue file replaces the built-in file of its language. |

`/laura lang` shows your language, the search order, the available languages and the number of lines of the language she uses with you.

### Dialogue files

Folder: `config/lauramod/dialogues/` on the server (or in your game folder for singleplayer).

| Entry | Role |
|---|---|
| `<language>.json` | Your file. The name is a Minecraft language code. |
| `_builtin/` | Read-only copies of the shipped files, rewritten at every start. Copy one to start from it. |
| `README.txt` | A short reminder of the format. |

- A file for a built-in language is **merged** with the built-in one: your lines, triggers, answers and keywords are added.
- A file with `"mode": "replace"` **replaces** the built-in language entirely.
- Inside `intents`, an entry with `"replace": true` replaces only that intent.
- A file for a language the mod does not ship **adds** that language.
- Apply changes with `/laura reload`.

Format:

```json
{
  "lines":      { "<event key>": ["line 1", "line 2"] },
  "intents":    { "<intent id>": { "triggers": ["words players type"], "responses": ["her answers"] } },
  "items":      { "<item id or #tag>": ["keywords used in fetch orders"] },
  "chests":     { "<chest purpose>": ["keywords used when assigning a chest"] },
  "connectors": ["words that chain two orders"],
  "everyone":   ["words that address every companion"],
  "spelling":   { "what players type": "what it means" }
}
```

| Section | Details |
|---|---|
| `lines` | What she says for an event. One line is picked at random. The full list of keys is in `_builtin/en_us.json` (216 keys). |
| `intents` | What she understands. `triggers` are the phrases, `responses` her possible answers. |
| `items` | Words for the fetch order. The key is an item id or an item tag. |
| `chests` | Words for "this chest is for ...". Keys: `wood`, `harvest`, `seeds`, `ingredients`, `fuel`, `meals`, `pantry`, `storage`. |
| `connectors` | Words that split a message into several orders ("then"). |
| `everyone` | Words that address all companions in range ("everyone"). |
| `spelling` | Rewrites applied before triggers are searched. See below. |

### Triggers

- Case, accents and punctuation are ignored. "Désolée !" matches the trigger `desolee`.
- A trigger matches whole words that follow each other, anywhere in the message.
- A word ending with `*` matches the beginning of a word: `sorr*` matches "sorry" and "sorrry".
- When several triggers match, the longest one wins.
- A trigger must contain at least one letter.
- Languages written without spaces (Chinese, Japanese, Thai, Lao, Khmer, Burmese) are matched as pieces of text instead of words.

### The spelling section

`spelling` lists chat shortcuts and frequent mistakes with their usual form. A message matches a trigger as typed, or after these rewrites.

```json
{
  "spelling": {
    "u": "you",
    "ur": "you're",
    "thx": "thanks",
    "so": "",
    "very": ""
  }
}
```

- `"u": "you"` reads "love u" as "love you".
- An empty value drops a filler word: "you're so pretty" is read as "you're pretty".
- Rules are applied once, from left to right. At each word the longest rule wins.
- The mod ships spelling rules for `en_us` and `fr_fr`. You can add rules for any language.

### Intents

The built-in intent ids, and what they do:

| Intent id | Effect |
|---|---|
| `summon` | Summons a companion. |
| `follow`, `stay`, `come`, `home`, `wander`, `set_home`, `stop` | The matching order. |
| `fetch` | Fetch. The item comes from the `items` keywords of the same message. |
| `chop_tree`, `harvest`, `cook` | One task. |
| `job_lumberjack`, `job_farmer`, `job_cook`, `job_stop`, `back_to_work` | Jobs. |
| `assign_chest` | Assigns the container the player looks at. The purpose comes from the `chests` keywords. |
| `hug`, `kiss`, `eat`, `sleep`, `wake_up` | The matching action. |
| `apology` | Ends sulking and anger. |
| `beautiful`, `love_you`, `miss_you`, `thanks` | Compliment, then one of the `responses`. |
| `insult` | Makes her angry, then one of the `responses`. |
| `what_do_you_want`, `how_are_you`, `where` | She tells her desire, her mood, or answers. |
| `player_hungry` | She gives food to the player. |
| `marry_me` | One of the `responses`, and an advancement. |
| `yes`, `no` | Answer to her love question. |
| `emote_<name>` | Plays the emote `<name>`, for example `emote_dance`. |
| Any other id | She says one of the `responses`. This is how small talk works (`hello`, `joke`...), and how you add your own. |

### Placeholders

A line can contain placeholders between braces. A placeholder that has no value for that line is left as written.

| Placeholder | Value | Available in |
|---|---|---|
| `{laura}` | Her name. | every line |
| `{player}` | The name of the player she talks to. | every line |
| `{days}` | In-game days together. | every line |
| `{item}` | An item name, shown in each player's language. | fetch, food, gift, desire and work lines |
| `{count}` | A number of items. | fetch, gift return and delivery lines |
| `{place}`, `{activity}` | What she wishes for. | desire lines |
| `{minutes}` | Minutes left. | new desire and reminder lines |
| `{hours}` | Hours you were away. | `welcome_back.long`, `welcome_back.days` |
| `{attacker}` | Who hit her. | `hit_by_other`, `help` |
| `{victim}` | What you killed. | `owner.killed.*` |
| `{other}` | The other player. | `jealous.player` |
| `{name}` | Her new name. | `rename.done` |
| `{radius}` | The work radius. | `job.start.*` |
| `{task}` | A task name. | `task.queued` |
| `{purpose}` | A chest purpose. | `chest.assigned` |

Look at `_builtin/en_us.json` to see which placeholders a line uses.

### Worked example 1: add a language

Esperanto is a Minecraft language (`eo_uy`) that the mod does not ship. Create `config/lauramod/dialogues/eo_uy.json`:

```json
{
  "lines": {
    "summon": ["Saluton, {player}! Mi estas {laura}."],
    "order.follow": ["Mi venas!", "Mi sekvas vin, {player}."],
    "order.stay": ["Bone, mi atendas ĉi tie."],
    "fetch.start": ["Mi serĉas {item} por vi."],
    "confused": ["Mi ne komprenas..."]
  },
  "intents": {
    "summon": { "triggers": ["mi sentas min sola"] },
    "follow": { "triggers": ["sekvu min", "venu kun mi"] },
    "stay": { "triggers": ["restu ĉi tie", "atendu"] },
    "come": { "triggers": ["venu ĉi tien"] },
    "fetch": { "triggers": ["alportu al mi", "alportu"] },
    "hug": { "triggers": ["brakumo", "brakumu min"] },
    "apology": { "triggers": ["pardonu", "pardon*"] },
    "hello": { "triggers": ["saluton"], "responses": ["Saluton, {player}!"] }
  },
  "items": {
    "minecraft:bread": ["pano*"],
    "#minecraft:logs": ["ligno*", "trunko*"]
  },
  "chests": {
    "wood": ["ligno"],
    "pantry": ["mangaĵo*"]
  },
  "connectors": ["poste", "kaj poste"],
  "everyone": ["ĉiuj", "vi ĉiuj"]
}
```

Run `/laura reload`. A player whose game is in Esperanto now gets these lines, and English for everything the file does not contain. She understands the Esperanto triggers and the English ones.

### Worked example 2: tune a built-in language

`config/lauramod/dialogues/en_us.json`, merged with the built-in English:

```json
{
  "spelling": {
    "bby": "baby",
    "cmon": "come on",
    "kinda": ""
  },
  "lines": {
    "ambient": ["I counted {days} days with you, {player}."]
  },
  "intents": {
    "follow": { "triggers": ["move out", "let's roll"] },
    "weather": { "triggers": ["nice weather"], "responses": ["I like it when it is sunny."] }
  }
}
```

- Three spelling rules are added.
- One more random line is added to `ambient`.
- "move out" and "let's roll" now also mean "follow me".
- `weather` is a new intent: she answers when somebody writes "nice weather".

### Interface translations

Folder: `config/lauramod/lang/` in the **client** game folder.

- Put a `<language>.json` file in the usual Minecraft language format.
- `en_us.json` is loaded first, then the file of the language selected in the game options.
- It works for the keys of the mod and for any other key of the game or of other mods.
- The full list of the mod's keys is in the jar: `assets/lauramod/lang/en_us.json`.
- Changes are applied when resources are reloaded (F3 + T) or when you change language.

Example, `config/lauramod/lang/en_us.json`:

```json
{
  "item.lauramod.laura_heart": "Heart of my companion",
  "lauramod.menu.tab.home": "Overview",
  "lauramod.place.volcano": "the volcano"
}
```

The last key names a custom place with the id `volcano` added to `desires.json`.

---

## Français

### Deux sortes de textes

| Texte | Où il se trouve | Côté | Comment le personnaliser |
|---|---|---|---|
| Ce qu'elle dit et ce qu'elle comprend dans le chat | Fichiers de dialogues | serveur | `config/lauramod/dialogues/<langue>.json` |
| Interface : menu, objets, progrès, messages système | Fichiers de langue | client | `config/lauramod/lang/<langue>.json` ou un pack de ressources |

Aucun code ni pack de ressources n'est nécessaire pour l'un comme pour l'autre.

### Langues intégrées

18 langues sont fournies avec le mod, pour les deux sortes de textes :

`cs_cz`, `de_de`, `en_us`, `es_es`, `fr_fr`, `id_id`, `it_it`, `ja_jp`, `ko_kr`, `nl_nl`, `pl_pl`, `pt_br`, `ru_ru`, `sv_se`, `tr_tr`, `uk_ua`, `zh_cn`, `zh_tw`.

La phrase d'invocation de chaque langue :

| Langue | Phrase | Langue | Phrase |
|---|---|---|---|
| `cs_cz` | cítím se osaměl | `nl_nl` | ik voel me eenzaam |
| `de_de` | ich fühle mich einsam | `pl_pl` | czuję się samotnie |
| `en_us` | i feel lonely | `pt_br` | me sinto sozinho |
| `es_es` | me siento solo | `ru_ru` | мне одиноко |
| `fr_fr` | je me sens seul | `sv_se` | jag är ensam |
| `id_id` | aku kesepian | `tr_tr` | yalnızım |
| `it_it` | mi sento solo | `uk_ua` | мені самотньо |
| `ja_jp` | 寂しい | `zh_cn` | 我好孤单 |
| `ko_kr` | 외로워 | `zh_tw` | 我好孤單 |

### Quelle langue elle utilise

Elle parle à chaque joueur dans la langue de son jeu. Pour chaque réplique elle essaie, dans l'ordre :

1. Le code de langue exact du joueur (par exemple `fr_ca`).
2. Une variante régionale proche (`fr_ca` puis `fr_fr`, `es_ar` puis `es_mx` puis `es_es`, `en_au` puis `en_gb` puis `en_us`, `zh_hk` puis `zh_tw`, `pt_pt` puis `pt_br`).
3. La variante principale de la langue.
4. `dialogue.fallbackLanguage` (par défaut `en_us`).
5. `en_us`.

La recherche se fait réplique par réplique : un fichier de langue qui ne contient que quelques répliques passe à la langue suivante pour les autres.

| Option | Effet |
|---|---|
| `dialogue.forcedLanguage` | Impose une langue de dialogue à tout le monde, par exemple `fr_fr`. |
| `dialogue.matchAllLanguages` | False : elle écoute les langues de la chaîne du joueur plus l'anglais. True : toutes les langues qu'elle connaît. |
| `dialogue.customDialoguesReplace` | True : chaque fichier de dialogues personnalisé remplace le fichier intégré de sa langue. |

`/laura lang` affiche votre langue, l'ordre de recherche, les langues disponibles et le nombre de répliques de la langue qu'elle utilise avec vous.

### Fichiers de dialogues

Dossier : `config/lauramod/dialogues/` sur le serveur (ou dans votre dossier de jeu en solo).

| Élément | Rôle |
|---|---|
| `<langue>.json` | Votre fichier. Le nom est un code de langue Minecraft. |
| `_builtin/` | Copies en lecture seule des fichiers fournis, réécrites à chaque démarrage. Copiez-en une pour partir de là. |
| `README.txt` | Un court rappel du format. |

- Un fichier pour une langue intégrée est **fusionné** avec le fichier intégré : vos répliques, déclencheurs, réponses et mots-clés s'ajoutent.
- Un fichier contenant `"mode": "replace"` **remplace** entièrement la langue intégrée.
- Dans `intents`, une entrée contenant `"replace": true` remplace seulement cette intention.
- Un fichier pour une langue que le mod ne fournit pas **ajoute** cette langue.
- Appliquez les changements avec `/laura reload`.

Format :

```json
{
  "lines":      { "<clé d'événement>": ["réplique 1", "réplique 2"] },
  "intents":    { "<id d'intention>": { "triggers": ["mots tapés par les joueurs"], "responses": ["ses réponses"] } },
  "items":      { "<id d'objet ou #tag>": ["mots-clés des ordres rapporter"] },
  "chests":     { "<usage de coffre>": ["mots-clés pour attribuer un coffre"] },
  "connectors": ["mots qui enchaînent deux ordres"],
  "everyone":   ["mots qui s'adressent à toutes les compagnes"],
  "spelling":   { "ce que les joueurs tapent": "ce que cela veut dire" }
}
```

| Section | Détails |
|---|---|
| `lines` | Ce qu'elle dit pour un événement. Une réplique est tirée au hasard. La liste complète des clés est dans `_builtin/en_us.json` (216 clés). |
| `intents` | Ce qu'elle comprend. `triggers` contient les phrases, `responses` ses réponses possibles. |
| `items` | Mots pour l'ordre rapporter. La clé est un identifiant d'objet ou un tag d'objets. |
| `chests` | Mots pour « ce coffre est pour ... ». Clés : `wood`, `harvest`, `seeds`, `ingredients`, `fuel`, `meals`, `pantry`, `storage`. |
| `connectors` | Mots qui découpent un message en plusieurs ordres (« puis »). |
| `everyone` | Mots qui s'adressent à toutes les compagnes à portée (« tout le monde »). |
| `spelling` | Réécritures appliquées avant la recherche des déclencheurs. Voir plus bas. |

### Déclencheurs

- La casse, les accents et la ponctuation sont ignorés. « Désolée ! » correspond au déclencheur `desolee`.
- Un déclencheur correspond à des mots entiers qui se suivent, n'importe où dans le message.
- Un mot terminé par `*` correspond au début d'un mot : `pardon*` correspond à « pardon » et « pardonne ».
- Quand plusieurs déclencheurs correspondent, le plus long l'emporte.
- Un déclencheur doit contenir au moins une lettre.
- Les langues écrites sans espaces (chinois, japonais, thaï, lao, khmer, birman) sont comparées par morceaux de texte plutôt que par mots.

### La section spelling

`spelling` liste les abréviations du chat et les fautes fréquentes avec leur forme habituelle. Un message correspond à un déclencheur tel qu'il est tapé, ou après ces réécritures.

```json
{
  "spelling": {
    "jtm": "je t'aime",
    "tu est": "tu es",
    "stp": "s'il te plait",
    "trop": "",
    "vraiment": ""
  }
}
```

- `"jtm": "je t'aime"` lit « jtm » comme « je t'aime ».
- `"tu est": "tu es"` corrige une faute courante : « tu est jolie » est lu comme « tu es jolie ».
- Une valeur vide supprime un mot de remplissage : « tu es trop jolie » est lu comme « tu es jolie ».
- Les règles sont appliquées une fois, de gauche à droite. À chaque mot, la règle la plus longue l'emporte.
- Le mod fournit des règles pour `en_us` et `fr_fr`. Vous pouvez en ajouter pour n'importe quelle langue.

### Intentions

Les identifiants d'intention intégrés, et ce qu'ils font :

| Id d'intention | Effet |
|---|---|
| `summon` | Invoque une compagne. |
| `follow`, `stay`, `come`, `home`, `wander`, `set_home`, `stop` | L'ordre correspondant. |
| `fetch` | Rapporter. L'objet vient des mots-clés `items` du même message. |
| `chop_tree`, `harvest`, `cook` | Une tâche. |
| `job_lumberjack`, `job_farmer`, `job_cook`, `job_stop`, `back_to_work` | Métiers. |
| `assign_chest` | Attribue le conteneur que le joueur regarde. L'usage vient des mots-clés `chests`. |
| `hug`, `kiss`, `eat`, `sleep`, `wake_up` | L'action correspondante. |
| `apology` | Met fin à la bouderie et à la colère. |
| `beautiful`, `love_you`, `miss_you`, `thanks` | Compliment, puis l'une des `responses`. |
| `insult` | La met en colère, puis l'une des `responses`. |
| `what_do_you_want`, `how_are_you`, `where` | Elle dit son désir, son humeur, ou répond. |
| `player_hungry` | Elle donne de la nourriture au joueur. |
| `marry_me` | L'une des `responses`, et un progrès. |
| `yes`, `no` | Réponse à sa question d'amour. |
| `emote_<nom>` | Joue l'émote `<nom>`, par exemple `emote_dance`. |
| Tout autre identifiant | Elle dit l'une des `responses`. C'est ainsi que fonctionne le bavardage (`hello`, `joke`...), et c'est ainsi que vous ajoutez le vôtre. |

### Variables

Une réplique peut contenir des variables entre accolades. Une variable qui n'a pas de valeur pour cette réplique reste écrite telle quelle.

| Variable | Valeur | Disponible dans |
|---|---|---|
| `{laura}` | Son nom. | toutes les répliques |
| `{player}` | Le nom du joueur à qui elle parle. | toutes les répliques |
| `{days}` | Jours de jeu passés ensemble. | toutes les répliques |
| `{item}` | Un nom d'objet, affiché dans la langue de chaque joueur. | répliques de recherche, de nourriture, de cadeau, de désir et de travail |
| `{count}` | Un nombre d'objets. | répliques de recherche, de cadeau en retour et de livraison |
| `{place}`, `{activity}` | Ce qu'elle désire. | répliques de désir |
| `{minutes}` | Minutes restantes. | nouveau désir et rappels |
| `{hours}` | Heures d'absence. | `welcome_back.long`, `welcome_back.days` |
| `{attacker}` | Qui l'a frappée. | `hit_by_other`, `help` |
| `{victim}` | Ce que vous avez tué. | `owner.killed.*` |
| `{other}` | L'autre joueur. | `jealous.player` |
| `{name}` | Son nouveau nom. | `rename.done` |
| `{radius}` | Le rayon de travail. | `job.start.*` |
| `{task}` | Un nom de tâche. | `task.queued` |
| `{purpose}` | Un usage de coffre. | `chest.assigned` |

Consultez `_builtin/en_us.json` pour voir quelles variables une réplique utilise.

### Exemple complet 1 : ajouter une langue

L'espéranto est une langue de Minecraft (`eo_uy`) que le mod ne fournit pas. Créez `config/lauramod/dialogues/eo_uy.json` :

```json
{
  "lines": {
    "summon": ["Saluton, {player}! Mi estas {laura}."],
    "order.follow": ["Mi venas!", "Mi sekvas vin, {player}."],
    "order.stay": ["Bone, mi atendas ĉi tie."],
    "fetch.start": ["Mi serĉas {item} por vi."],
    "confused": ["Mi ne komprenas..."]
  },
  "intents": {
    "summon": { "triggers": ["mi sentas min sola"] },
    "follow": { "triggers": ["sekvu min", "venu kun mi"] },
    "stay": { "triggers": ["restu ĉi tie", "atendu"] },
    "come": { "triggers": ["venu ĉi tien"] },
    "fetch": { "triggers": ["alportu al mi", "alportu"] },
    "hug": { "triggers": ["brakumo", "brakumu min"] },
    "apology": { "triggers": ["pardonu", "pardon*"] },
    "hello": { "triggers": ["saluton"], "responses": ["Saluton, {player}!"] }
  },
  "items": {
    "minecraft:bread": ["pano*"],
    "#minecraft:logs": ["ligno*", "trunko*"]
  },
  "chests": {
    "wood": ["ligno"],
    "pantry": ["mangaĵo*"]
  },
  "connectors": ["poste", "kaj poste"],
  "everyone": ["ĉiuj", "vi ĉiuj"]
}
```

Lancez `/laura reload`. Un joueur dont le jeu est en espéranto reçoit maintenant ces répliques, et l'anglais pour tout ce que le fichier ne contient pas. Elle comprend les déclencheurs en espéranto et ceux en anglais.

### Exemple complet 2 : ajuster une langue intégrée

`config/lauramod/dialogues/fr_fr.json`, fusionné avec le français intégré :

```json
{
  "spelling": {
    "steuplait": "s'il te plait",
    "vien": "viens",
    "grave": ""
  },
  "lines": {
    "ambient": ["Ça fait {days} jours qu'on est ensemble, {player}."]
  },
  "intents": {
    "follow": { "triggers": ["on bouge", "en route"] },
    "weather": { "triggers": ["il fait beau"], "responses": ["J'aime bien quand il y a du soleil."] }
  }
}
```

- Trois règles d'orthographe sont ajoutées.
- Une réplique aléatoire de plus est ajoutée à `ambient`.
- « on bouge » et « en route » veulent maintenant aussi dire « suis-moi ».
- `weather` est une nouvelle intention : elle répond quand quelqu'un écrit « il fait beau ».

### Traductions de l'interface

Dossier : `config/lauramod/lang/` dans le dossier de jeu du **client**.

- Placez-y un fichier `<langue>.json` au format habituel des fichiers de langue de Minecraft.
- `en_us.json` est chargé en premier, puis le fichier de la langue sélectionnée dans les options du jeu.
- Cela fonctionne pour les clés du mod et pour n'importe quelle autre clé du jeu ou d'autres mods.
- La liste complète des clés du mod est dans le jar : `assets/lauramod/lang/en_us.json`.
- Les changements sont appliqués au rechargement des ressources (F3 + T) ou au changement de langue.

Exemple, `config/lauramod/lang/fr_fr.json` :

```json
{
  "item.lauramod.laura_heart": "Cœur de ma compagne",
  "lauramod.menu.tab.home": "Résumé",
  "lauramod.place.volcano": "le volcan"
}
```

La dernière clé nomme un lieu personnalisé d'identifiant `volcano` ajouté à `desires.json`.
