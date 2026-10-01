# Custom models and animations

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [SKINS.md](SKINS.md), [CONFIG.md](CONFIG.md), [COMMANDS.md](COMMANDS.md).

---

## English

### Overview

By default she uses a player-like model and a skin. She can instead wear a custom Blockbench model, with its own textures and animations.

| Setting | Side | Role |
|---|---|---|
| `models.allowCustomModels` | server | Allows custom models. True by default. |
| `models.allowUploads` | server | Allows players to upload model files. False by default. |
| `models.maxModelKb` | server | Largest model (all its files), 2048 KB by default. |
| `models.maxUploadsPerPlayer` | server | Model files one player may keep on the server, 3 by default. |
| `models.defaultModel` | server | Model of a new companion. Empty by default. |
| `display.customModels` | client | If false, you always see the default model. |
| `display.animations` | client | If false, no animation is played. |
| `network.maxModelDownloadKb` | client | Largest model you accept from a server, 4096 KB by default. |

A custom model uses its own textures. The skin chosen in the Style tab only applies to the default model.

### Supported files

| Form | Content |
|---|---|
| `<name>.bbmodel` | A Blockbench project. Textures and animations are embedded in the file. |
| `<name>.geo.json` | A Bedrock geometry, with optional `<name>.animation.json` (animations) and `<name>.png` (texture) next to it. |
| `<name>/` | A folder holding one of the two forms above. |

Limits and rules:

- Only cubes are read. Mesh elements are ignored.
- 4096 cubes and 1024 bones at most, groups nested 64 deep at most. A bone cannot be its own ancestor.
- 8 textures at most, each at most 2048x2048 pixels, and together at most the pixels of two 2048x2048 textures.
- A model beyond a limit is refused: an upload with a message, a file of the folders with a line in the log.
- Box UV and per-face UV are both supported. A `.bbmodel` can use several textures.
- Groups and cubes with export turned off, and hidden cubes, are skipped.
- A `.geo.json` file uses its first geometry.
- A model without any texture is drawn with the standard built-in skin.
- Units are pixels, 16 pixels for one block, like in Blockbench. The default model is 32 pixels tall. Her hit box does not change.

### Where to put a model

**1. Server folder** (the usual way). Put the files in `config/lauramod/models/` on the server, or in your own game folder for singleplayer. Run `/laura reload`. Players choose the model in the Style tab, page Models, or with `/laura model <name>`. Clients download it from the server and keep it in `<game folder>/lauramod/cache/models`. The server sends skins and models at 640 KB per second to each player, and a client asks again after 10 seconds without an answer. A client only accepts a file it asked for, only keeps it when its content matches the hash announced by the server, and refuses a model of more than 256 files or more than 64 MB once unpacked.

Worked example of a folder layout:

```
config/lauramod/models/
    README.txt
    knight.bbmodel                  model "knight"
    fox_girl.geo.json               model "fox_girl"
    fox_girl.animation.json
    fox_girl.png
    maid/                           model "maid"
        maid.geo.json
        maid.animation.json
        maid.png
    uploads/
        <UUID>_dress.bbmodel        model "uploads/<UUID>_dress" (sent by a player)
```

The name is the file name without its extension, or the folder name. Allowed characters: letters, digits, `_`, `-`, `.`, `/` and space.

**2. Resource pack.** Put the files at `assets/<namespace>/laura_models/<name>.bbmodel` (or `.geo.json`, `.animation.json`, `.png`). When `/laura model <name>` does not find the name on the server, the model is looked up in each client's resource packs, then in the client's own `config/lauramod/models` folder. Players who do not have it see the default model.

**3. Player upload.** With `models.allowUploads` set to true, on a multiplayer server, the Models page lists the `.bbmodel` files of your own `config/lauramod/models` folder with an "Upload" action. You can also drag and drop a `.bbmodel` file onto the menu window. You must be allowed to change her look. The server checks the file, saves it as `models/uploads/<UUID>_<name>.bbmodel` (the UUID of the player's account, without dashes) and applies it. A player can send one file every 3 seconds. Each player may keep 3 model files on the server (`models.maxUploadsPerPlayer`); sending a file with the same name replaces the old one and is always allowed. Only `.bbmodel` files can be uploaded, because they hold their textures. A `.geo.json` file is refused with a message: such models must be placed in the server folder by its administrator.

`models.defaultModel` is the model of every newly summoned companion. `/laura model reset` and the "Default model" entry of the menu apply it again. The name designates a file of the server folder when one exists, otherwise a resource pack model. Write `pack:<name>` to force the resource pack. The option is ignored when `models.allowCustomModels` is false. It cannot designate a file that only exists in a player's own folder.

### Bone names

Some features need to know which bones are the head, the body, the arms and the legs: the default animations, the walking swing, the head that looks around, held items, the hay gag and the backpack.

Bones are recognised by name. Case, accents, spaces, dashes and underscores are ignored, so `Right_Arm`, `rightArm`, `RIGHT ARM` and `right-arm` are the same name. When several bones match, the one closest to the top of the hierarchy wins.

Names tell arms from legs. Which arm (or leg) is the right one is then read from the positions: the model faces north, and of the two bones the one whose pivot has the larger X in Blockbench is used as the right one. A model whose sides are labelled the other way round still waves and holds items with the correct hand.

| Part | Accepted names (normalised) |
|---|---|
| head | `head`, `bipedhead`, `tete`, `kopf`, `cabeza`, `testa`, `cabeca`, `hoofd`, `glowa`, `huvud`, `kafa`, `kepala` |
| body | `body`, `bipedbody`, `torso`, `chest`, `corps`, `torse`, `korper`, `rumpf`, `cuerpo`, `corpo`, `tronco`, `lichaam`, `romp`, `cialo`, `tulwa`, `kropp`, `govde`, `badan` |
| right arm | `rightarm`, `armright`, `bipedrightarm`, `armr`, `rarm`, `rightupperarm`, `brasd`, `brasdroit`, `rechterarm`, `armrechts`, `brazod`, `brazoderecho`, `brazoder`, `bracciod`, `bracciodestro`, `bracod`, `bracodireito`, `prawareka`, `rekaprawa`, `hogerarm`, `sagkol`, `lengankanan` |
| left arm | `leftarm`, `armleft`, `bipedleftarm`, `arml`, `larm`, `leftupperarm`, `brasg`, `brasgauche`, `linkerarm`, `armlinks`, `brazoi`, `brazoizquierdo`, `brazoizq`, `braccios`, `bracciosinistro`, `bracoe`, `bracoesquerdo`, `lewareka`, `rekalewa`, `vansterarm`, `solkol`, `lengankiri` |
| right leg | `rightleg`, `legright`, `bipedrightleg`, `legr`, `rleg`, `rightupperleg`, `jambed`, `jambedroite`, `rechterbeen`, `beinrechts`, `rechtesbein`, `piernad`, `piernaderecha`, `piernader`, `gambad`, `gambadestra`, `pernad`, `pernadireita`, `prawanoga`, `nogaprawa`, `hogerben`, `sagbacak`, `kakikanan` |
| left leg | `leftleg`, `legleft`, `bipedleftleg`, `legl`, `lleg`, `leftupperleg`, `jambeg`, `jambegauche`, `linkerbeen`, `beinlinks`, `linkesbein`, `piernai`, `piernaizquierda`, `piernaizq`, `gambas`, `gambasinistra`, `pernae`, `pernaesquerda`, `lewanoga`, `nogalewa`, `vansterben`, `solbacak`, `kakikiri` |
| root | `root`, `racine`, `raiz`, `radice`, `wurzel`, `wortel`, `korzen` |

Attachment points:

| Feature | Bone searched, in order |
|---|---|
| Head looking around | `head`, `bipedhead`, `neck`, then the head part. |
| Hay gag | `head`, `bipedhead`, then the head part. |
| Item in the right hand | `right_hand`, `rightitem`, `right_item`, `hand_right`, then the right arm part. |
| Item in the left hand | `left_hand`, `leftitem`, `left_item`, `hand_left`, then the left arm part. |
| Backpack | `body`, `bipedbody`, `torso`, `chest`, then the body part. Without such a bone the backpack is not drawn. |

Recommended hierarchy:

```
root
    body
        head
        right_arm
            right_hand
        left_arm
            left_hand
    right_leg
    left_leg
```

### Animations she plays

An animation is found by the last part of its name, in lower case: `animation.laura.walk` is `walk`. Missing animations are simply skipped.

**State animations** (one at a time, looping). The first matching line wins:

| Situation | Animation | Used instead when missing |
|---|---|---|
| Asleep | `sleep` | `idle` |
| Sitting (stay order) or riding | `sit` | `idle` |
| Swimming | `swim` | `idle` |
| Fetching or carrying an item, moving | `carry_walk` | `carry`, then `walk` |
| Fetching or carrying an item, standing | `carry` | `idle` |
| Gagged | `gagged` (or `walk` while moving, if the model has one) | `idle` |
| Moving | `walk` | none |
| Sad or sulking | `sad` | `idle` |
| Angry or jealous | `angry` | `idle` |
| Tired | `tired` | `idle` |
| Hungry | `hungry` | `idle` |
| Happy or in love | `happy` | `idle` |
| Otherwise | `idle` | none |

**Emote animations** (played once over the state animation): `wave`, `hug`, `kiss`, `dance`, `clap`, `laugh`, `cry`, `blush`, `facepalm`, `jump`, `bow`, `think`, `shrug`, `stomp`, `yawn`, `eat`, `poke`, `slap`, `celebrate`, `snap`, `twirl`, `hum`, `stretch`, `tap_foot`, `blow_kiss`, `sneeze`, `hair_flip`, `check_nails`, `air_guitar`, `hiccup`, `pout`, `shiver`, `fan`.

Rules:

- Loop modes: loop, hold on last frame, play once.
- Changes of state cross-fade over 0.25 second.
- A model **without any animation** borrows the default animations. They are applied to the bones recognised as head, body, arms, legs and root.
- A model **without a `walk` animation** gets the player-like swing of arms and legs on the recognised bones.
- Keyframe interpolation: linear, step, and smooth (catmullrom and bezier are both played as a smooth curve).

**Molang.** Keyframe values may be expressions:

- Numbers, `+ - * / %`, comparisons, `&&`, `||`, `!`, the ternary `? :`, and `??`.
- `math.` functions: `sin`, `cos`, `asin`, `acos`, `atan`, `atan2` (degrees), `abs`, `sqrt`, `floor`, `ceil`, `round`, `trunc`, `exp`, `ln`, `pow`, `mod`, `min`, `max`, `clamp`, `lerp`, `lerprotate`, `hermite_blend`, `sign`, `random`, `random_integer`, `die_roll`, and the constant `math.pi`.
- `query.` (or `q.`) values: `anim_time`, `life_time`, `ground_speed`, `modified_move_speed`, `distance_moved`, `modified_distance_moved`, `health`, `max_health`, `head_y_rotation`, `head_x_rotation`, `is_on_ground`, `is_in_water`, `is_sitting`, `is_sleeping`.
- `variable.`, `temp.` and `context.` values are read as 0. An expression that cannot be parsed is read as 0, and so is one longer than 1024 characters, nested more than 48 deep, or holding more than 512 operators and function calls.
- `math.die_roll` throws 64 dice at most.

### The default animation file

The animations of the default model are in the mod jar: `assets/lauramod/animations/laura_humanoid.json`. The file uses the Bedrock animation format, with the bones `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg` and `root`. Rotations are degrees added to the part, positions are pixels added to the part, with Y pointing down.

To change them, create `config/lauramod/animations/laura_humanoid.json` on the client. An animation of that file replaces the built-in animation of the same name. Animations you do not list stay as they are. The file is read when the game starts and at each resource reload (F3 + T).

Worked example, a calmer `idle`:

```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.laura.idle": {
      "loop": true,
      "animation_length": 4.0,
      "bones": {
        "head": { "rotation": { "0.0": [0, 0, 0], "2.0": [2, 0, 0], "4.0": [0, 0, 0] } },
        "right_arm": { "rotation": [0, 0, "math.sin(query.anim_time * 90) * 2"] },
        "left_arm": { "rotation": [0, 0, "-math.sin(query.anim_time * 90) * 2"] }
      }
    }
  }
}
```

### Commands

| Command | Effect |
|---|---|
| `/laura model list` | Models of the server. |
| `/laura model <name>` | Applies a model. The name is typed as it is, for example `/laura model maid`. |
| `/laura model reset` | Applies `models.defaultModel` (the default player-like model when it is empty). |

The first custom model you give her, chosen or uploaded, grants the "Total Makeover" advancement. A change of model also fulfils the `new_outfit` desire.

---

## Français

### Vue d'ensemble

Par défaut elle utilise un modèle de type joueur et un skin. Elle peut à la place porter un modèle Blockbench personnalisé, avec ses propres textures et animations.

| Réglage | Côté | Rôle |
|---|---|---|
| `models.allowCustomModels` | serveur | Autorise les modèles personnalisés. True par défaut. |
| `models.allowUploads` | serveur | Autorise les joueurs à envoyer des fichiers de modèle. False par défaut. |
| `models.maxModelKb` | serveur | Plus gros modèle (tous ses fichiers), 2048 Ko par défaut. |
| `models.maxUploadsPerPlayer` | serveur | Nombre de fichiers de modèle qu'un joueur peut garder sur le serveur, 3 par défaut. |
| `models.defaultModel` | serveur | Modèle d'une nouvelle compagne. Vide par défaut. |
| `display.customModels` | client | Si false, vous voyez toujours le modèle par défaut. |
| `display.animations` | client | Si false, aucune animation n'est jouée. |
| `network.maxModelDownloadKb` | client | Plus gros modèle que vous acceptez d'un serveur, 4096 Ko par défaut. |

Un modèle personnalisé utilise ses propres textures. Le skin choisi dans l'onglet Style ne s'applique qu'au modèle par défaut.

### Fichiers gérés

| Forme | Contenu |
|---|---|
| `<nom>.bbmodel` | Un projet Blockbench. Les textures et les animations sont incluses dans le fichier. |
| `<nom>.geo.json` | Une géométrie Bedrock, avec en option `<nom>.animation.json` (animations) et `<nom>.png` (texture) à côté. |
| `<nom>/` | Un dossier contenant l'une des deux formes ci-dessus. |

Limites et règles :

- Seuls les cubes sont lus. Les éléments de type mesh sont ignorés.
- 4096 cubes et 1024 os au plus, des groupes imbriqués sur 64 niveaux au plus. Un os ne peut pas être son propre ancêtre.
- 8 textures au plus, chacune d'au plus 2048x2048 pixels, et ensemble au plus les pixels de deux textures de 2048x2048.
- Un modèle qui dépasse une limite est refusé : un envoi avec un message, un fichier des dossiers avec une ligne dans le journal.
- L'UV en boîte et l'UV par face sont tous deux gérés. Un `.bbmodel` peut utiliser plusieurs textures.
- Les groupes et cubes dont l'export est désactivé, et les cubes masqués, sont ignorés.
- Un fichier `.geo.json` utilise sa première géométrie.
- Un modèle sans aucune texture est dessiné avec le skin intégré standard.
- Les unités sont des pixels, 16 pixels pour un bloc, comme dans Blockbench. Le modèle par défaut mesure 32 pixels de haut. Sa boîte de collision ne change pas.

### Où placer un modèle

**1. Dossier du serveur** (la méthode habituelle). Placez les fichiers dans `config/lauramod/models/` sur le serveur, ou dans votre propre dossier de jeu en solo. Lancez `/laura reload`. Les joueurs choisissent le modèle dans l'onglet Style, page Modèles, ou avec `/laura model <nom>`. Les clients le téléchargent depuis le serveur et le gardent dans `<dossier du jeu>/lauramod/cache/models`. Le serveur envoie les skins et les modèles à 640 Ko par seconde à chaque joueur, et un client redemande après 10 secondes sans réponse. Un client n'accepte qu'un fichier qu'il a demandé, ne le garde que si son contenu correspond à l'empreinte annoncée par le serveur, et refuse un modèle de plus de 256 fichiers ou de plus de 64 Mo une fois décompressé.

Exemple complet d'organisation du dossier :

```
config/lauramod/models/
    README.txt
    knight.bbmodel                  modèle "knight"
    fox_girl.geo.json               modèle "fox_girl"
    fox_girl.animation.json
    fox_girl.png
    maid/                           modèle "maid"
        maid.geo.json
        maid.animation.json
        maid.png
    uploads/
        <UUID>_dress.bbmodel        modèle "uploads/<UUID>_dress" (envoyé par un joueur)
```

Le nom est le nom du fichier sans son extension, ou le nom du dossier. Caractères autorisés : lettres, chiffres, `_`, `-`, `.`, `/` et espace.

**2. Pack de ressources.** Placez les fichiers dans `assets/<namespace>/laura_models/<nom>.bbmodel` (ou `.geo.json`, `.animation.json`, `.png`). Quand `/laura model <nom>` ne trouve pas le nom sur le serveur, le modèle est cherché dans les packs de ressources de chaque client, puis dans le dossier `config/lauramod/models` du client. Les joueurs qui ne l'ont pas voient le modèle par défaut.

**3. Envoi par un joueur.** Avec `models.allowUploads` à true, sur un serveur multijoueur, la page Modèles liste les fichiers `.bbmodel` de votre propre dossier `config/lauramod/models` avec une action « Envoyer ». Vous pouvez aussi glisser-déposer un fichier `.bbmodel` sur la fenêtre du menu. Vous devez avoir le droit de changer son apparence. Le serveur vérifie le fichier, l'enregistre sous `models/uploads/<UUID>_<nom>.bbmodel` (l'UUID du compte du joueur, sans tirets) et l'applique. Un joueur peut envoyer un fichier toutes les 3 secondes. Chaque joueur peut garder 3 fichiers de modèle sur le serveur (`models.maxUploadsPerPlayer`) ; envoyer un fichier du même nom remplace l'ancien et reste toujours possible. Seuls les fichiers `.bbmodel` peuvent être envoyés, car ils contiennent leurs textures. Un fichier `.geo.json` est refusé avec un message : ces modèles doivent être placés dans le dossier du serveur par son administrateur.

`models.defaultModel` est le modèle de toute compagne nouvellement invoquée. `/laura model reset` et l'entrée « Modèle par défaut » du menu l'appliquent de nouveau. Le nom désigne un fichier du dossier du serveur quand il en existe un, sinon un modèle de pack de ressources. Écrivez `pack:<nom>` pour imposer le pack de ressources. L'option est ignorée quand `models.allowCustomModels` vaut false. Elle ne peut pas désigner un fichier qui n'existe que dans le dossier d'un joueur.

### Noms des os

Certaines fonctions ont besoin de savoir quels os sont la tête, le corps, les bras et les jambes : les animations par défaut, le balancement de la marche, la tête qui regarde autour d'elle, les objets tenus, le bâillon de foin et le sac à dos.

Les os sont reconnus par leur nom. La casse, les accents, les espaces, les tirets et les tirets bas sont ignorés : `Bras_D`, `brasD`, `BRAS D` et `bras-d` sont le même nom. Quand plusieurs os correspondent, celui qui est le plus haut dans la hiérarchie l'emporte.

Les noms distinguent les bras des jambes. Le bras (ou la jambe) droit est ensuite déterminé par les positions : le modèle regarde vers le nord, et des deux os celui dont le pivot a le X le plus grand dans Blockbench est utilisé comme côté droit. Un modèle dont les côtés sont nommés à l'envers salue et tient donc quand même les objets avec la bonne main.

| Partie | Noms acceptés (normalisés) |
|---|---|
| tête | `head`, `bipedhead`, `tete`, `kopf`, `cabeza`, `testa`, `cabeca`, `hoofd`, `glowa`, `huvud`, `kafa`, `kepala` |
| corps | `body`, `bipedbody`, `torso`, `chest`, `corps`, `torse`, `korper`, `rumpf`, `cuerpo`, `corpo`, `tronco`, `lichaam`, `romp`, `cialo`, `tulwa`, `kropp`, `govde`, `badan` |
| bras droit | `rightarm`, `armright`, `bipedrightarm`, `armr`, `rarm`, `rightupperarm`, `brasd`, `brasdroit`, `rechterarm`, `armrechts`, `brazod`, `brazoderecho`, `brazoder`, `bracciod`, `bracciodestro`, `bracod`, `bracodireito`, `prawareka`, `rekaprawa`, `hogerarm`, `sagkol`, `lengankanan` |
| bras gauche | `leftarm`, `armleft`, `bipedleftarm`, `arml`, `larm`, `leftupperarm`, `brasg`, `brasgauche`, `linkerarm`, `armlinks`, `brazoi`, `brazoizquierdo`, `brazoizq`, `braccios`, `bracciosinistro`, `bracoe`, `bracoesquerdo`, `lewareka`, `rekalewa`, `vansterarm`, `solkol`, `lengankiri` |
| jambe droite | `rightleg`, `legright`, `bipedrightleg`, `legr`, `rleg`, `rightupperleg`, `jambed`, `jambedroite`, `rechterbeen`, `beinrechts`, `rechtesbein`, `piernad`, `piernaderecha`, `piernader`, `gambad`, `gambadestra`, `pernad`, `pernadireita`, `prawanoga`, `nogaprawa`, `hogerben`, `sagbacak`, `kakikanan` |
| jambe gauche | `leftleg`, `legleft`, `bipedleftleg`, `legl`, `lleg`, `leftupperleg`, `jambeg`, `jambegauche`, `linkerbeen`, `beinlinks`, `linkesbein`, `piernai`, `piernaizquierda`, `piernaizq`, `gambas`, `gambasinistra`, `pernae`, `pernaesquerda`, `lewanoga`, `nogalewa`, `vansterben`, `solbacak`, `kakikiri` |
| racine | `root`, `racine`, `raiz`, `radice`, `wurzel`, `wortel`, `korzen` |

Points d'attache :

| Fonction | Os recherché, dans l'ordre |
|---|---|
| Tête qui regarde autour d'elle | `head`, `bipedhead`, `neck`, puis la partie tête. |
| Bâillon de foin | `head`, `bipedhead`, puis la partie tête. |
| Objet dans la main droite | `right_hand`, `rightitem`, `right_item`, `hand_right`, puis la partie bras droit. |
| Objet dans la main gauche | `left_hand`, `leftitem`, `left_item`, `hand_left`, puis la partie bras gauche. |
| Sac à dos | `body`, `bipedbody`, `torso`, `chest`, puis la partie corps. Sans un tel os le sac à dos n'est pas dessiné. |

Hiérarchie conseillée :

```
racine
    corps
        tete
        bras_droit
            right_hand
        bras_gauche
            left_hand
    jambe_droite
    jambe_gauche
```

### Animations qu'elle joue

Une animation est trouvée par la dernière partie de son nom, en minuscules : `animation.laura.walk` correspond à `walk`. Les animations absentes sont simplement ignorées.

**Animations d'état** (une à la fois, en boucle). La première ligne qui correspond l'emporte :

| Situation | Animation | Utilisée à la place si elle manque |
|---|---|---|
| Endormie | `sleep` | `idle` |
| Assise (ordre rester) ou sur une monture | `sit` | `idle` |
| En train de nager | `swim` | `idle` |
| Rapporte ou porte un objet, en mouvement | `carry_walk` | `carry`, puis `walk` |
| Rapporte ou porte un objet, à l'arrêt | `carry` | `idle` |
| Bâillonnée | `gagged` (ou `walk` en mouvement, si le modèle en a une) | `idle` |
| En mouvement | `walk` | aucune |
| Triste ou boudeuse | `sad` | `idle` |
| En colère ou jalouse | `angry` | `idle` |
| Fatiguée | `tired` | `idle` |
| Affamée | `hungry` | `idle` |
| Heureuse ou amoureuse | `happy` | `idle` |
| Sinon | `idle` | aucune |

**Animations d'émote** (jouées une fois par-dessus l'animation d'état) : `wave`, `hug`, `kiss`, `dance`, `clap`, `laugh`, `cry`, `blush`, `facepalm`, `jump`, `bow`, `think`, `shrug`, `stomp`, `yawn`, `eat`, `poke`, `slap`, `celebrate`, `snap`, `twirl`, `hum`, `stretch`, `tap_foot`, `blow_kiss`, `sneeze`, `hair_flip`, `check_nails`, `air_guitar`, `hiccup`, `pout`, `shiver`, `fan`.

Règles :

- Modes de boucle : en boucle, maintien de la dernière image, lecture unique.
- Les changements d'état se font en fondu sur 0,25 seconde.
- Un modèle **sans aucune animation** emprunte les animations par défaut. Elles sont appliquées aux os reconnus comme tête, corps, bras, jambes et racine.
- Un modèle **sans animation `walk`** reçoit le balancement des bras et des jambes d'un joueur sur les os reconnus.
- Interpolation des images clés : linéaire, par paliers, et lissée (catmullrom et bezier sont jouées toutes deux comme une courbe lissée).

**Molang.** Les valeurs des images clés peuvent être des expressions :

- Nombres, `+ - * / %`, comparaisons, `&&`, `||`, `!`, le ternaire `? :`, et `??`.
- Fonctions `math.` : `sin`, `cos`, `asin`, `acos`, `atan`, `atan2` (en degrés), `abs`, `sqrt`, `floor`, `ceil`, `round`, `trunc`, `exp`, `ln`, `pow`, `mod`, `min`, `max`, `clamp`, `lerp`, `lerprotate`, `hermite_blend`, `sign`, `random`, `random_integer`, `die_roll`, et la constante `math.pi`.
- Valeurs `query.` (ou `q.`) : `anim_time`, `life_time`, `ground_speed`, `modified_move_speed`, `distance_moved`, `modified_distance_moved`, `health`, `max_health`, `head_y_rotation`, `head_x_rotation`, `is_on_ground`, `is_in_water`, `is_sitting`, `is_sleeping`.
- Les valeurs `variable.`, `temp.` et `context.` valent 0. Une expression illisible vaut 0, de même qu'une expression de plus de 1024 caractères, imbriquée sur plus de 48 niveaux, ou contenant plus de 512 opérateurs et appels de fonction.
- `math.die_roll` lance au plus 64 dés.

### Le fichier d'animations par défaut

Les animations du modèle par défaut sont dans le jar du mod : `assets/lauramod/animations/laura_humanoid.json`. Le fichier utilise le format d'animation Bedrock, avec les os `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg` et `root`. Les rotations sont des degrés ajoutés à la partie, les positions sont des pixels ajoutés à la partie, avec Y vers le bas.

Pour les modifier, créez `config/lauramod/animations/laura_humanoid.json` sur le client. Une animation de ce fichier remplace l'animation intégrée du même nom. Les animations que vous ne listez pas restent inchangées. Le fichier est lu au lancement du jeu et à chaque rechargement des ressources (F3 + T).

Exemple complet, un `idle` plus calme :

```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.laura.idle": {
      "loop": true,
      "animation_length": 4.0,
      "bones": {
        "head": { "rotation": { "0.0": [0, 0, 0], "2.0": [2, 0, 0], "4.0": [0, 0, 0] } },
        "right_arm": { "rotation": [0, 0, "math.sin(query.anim_time * 90) * 2"] },
        "left_arm": { "rotation": [0, 0, "-math.sin(query.anim_time * 90) * 2"] }
      }
    }
  }
}
```

### Commandes

| Commande | Effet |
|---|---|
| `/laura model list` | Modèles du serveur. |
| `/laura model <nom>` | Applique un modèle. Le nom s'écrit tel quel, par exemple `/laura model maid`. |
| `/laura model reset` | Applique `models.defaultModel` (le modèle par défaut de type joueur quand il est vide). |

Le premier modèle personnalisé que vous lui donnez, choisi ou envoyé, accorde le progrès « Relooking complet ». Un changement de modèle réalise aussi le désir `new_outfit`.
