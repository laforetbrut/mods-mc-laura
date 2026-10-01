# Skins

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [MODELS.md](MODELS.md), [CONFIG.md](CONFIG.md), [COMMANDS.md](COMMANDS.md).

---

## English

### The four kinds of skins

| Kind | Reference | Where the image comes from | Switch |
|---|---|---|---|
| Built-in | `builtin:<name>` | Shipped with the mod. | always available |
| Server file | `server:<name>` | A PNG of the server's `config/lauramod/skins` folder. Clients download it from the server. | always available |
| URL | `url:<link>` | An internet address. Each client downloads it. | `skins.allowUrlSkins` |
| Player name | `player:<name>` | The skin of a Minecraft account, found by the server. | `skins.allowPlayerNameSkins` |

Her skin is changed from the Style tab of her menu (key K) or with `/laura skin`.

Who may change her look: her partner, operators (permission level 2), and everybody when `skins.othersCanChangeSkin` is true.

Changing her skin makes her comment on her new look, fulfils the `new_outfit` desire and counts for the "New Look" advancement.

### Built-in skins

| Name | Label |
|---|---|
| `laura` | Laura |
| `laura_summer` | Summer |
| `laura_winter` | Winter |
| `laura_night` | Pyjamas |
| `laura_sporty` | Sporty |
| `laura_gothic` | Gothic |

Command: `/laura skin builtin laura_winter`. In the menu they are the six face buttons at the top of the Skins page.

### Skin file rules

A skin file is a standard Minecraft skin:

- PNG format.
- Width is a multiple of 64, from 64 to 1024. Height equals the width, or half of it. Valid sizes: 64x64, 64x32, 128x128, 128x64 and so on.
- Old 64x32 skins are converted to the 64x64 layout.
- Size up to `skins.maxSkinKb` (256 KB).

The "slim arms" switch chooses between the slim arm model (3 pixels wide) and the classic one (4 pixels wide).

### Server skin files

For server owners, or for your own singleplayer game:

1. Drop PNG files in `config/lauramod/skins/`. Sub-folders are allowed, their path becomes part of the name.
2. Run `/laura reload` (the folder is also scanned at startup, and when an unknown name is requested).
3. Players pick the file in the "Server skins" list, or use `/laura skin file <name> [slim]`.

Rules:

- The name is the path inside the folder without `.png`. `skins/summer/beach.png` is named `summer/beach`. The command is `/laura skin file summer/beach`, without quotes.
- Allowed characters in names: letters, digits, `_`, `-`, `.`, `/` and space. 96 characters at most.
- A file that is too big or not a valid skin is ignored and reported in the server log.
- Clients download the file once and keep it in `<game folder>/lauramod/cache/skins`. A client refuses files larger than its own `network.maxSkinDownloadKb` (512 KB).
- `/laura skin list` shows the built-in skins and the files of the server.

### Player uploads

On a multiplayer server a player can send a skin file from their own computer. `skins.allowUploads` must be true (it is by default), and you must be allowed to change the look of the companion whose menu is open.

Worked example:

1. Save your skin as `config/lauramod/skins/my_dress.png` in **your** game folder. The Style tab has an "Open skins folder" button on the Models page.
2. Open her menu (K), tab Style, page Skins.
3. Click `my_dress.png` in the "Your skins" list. You can also drag and drop any `.png` file onto the menu window.
4. The file is sent to the server. A message at the bottom of the tab confirms it, or explains the refusal (uploads disabled, not allowed to change her look, upload limit reached, file too big, not a valid skin). Nothing is stored on the server when the upload is refused.
5. The server saves it as `config/lauramod/skins/uploads/<your player name>_my_dress.png` and puts it on her at once.
6. From now on every player sees `uploads/<your player name>_my_dress` in the "Server skins" list.

Notes:

- Each player may keep 10 skin files on the server (`skins.maxUploadsPerPlayer`). Sending a file with the same name replaces the old one and is always allowed.
- A successful upload counts for the "New Look" advancement.
- The stored name is cleaned: lower case letters, digits, `_` and `-` only, 32 characters at most for each part.
- In singleplayer nothing is uploaded: your folder is the server folder, and a click applies the file directly.
- The server administrator can delete files of the `uploads` folder at any time, then run `/laura reload`.

### URL skins

`/laura skin url <direct link to a PNG> [slim]`, or the URL field of the Skins page.

- `skins.allowUrlSkins` must be true.
- The address must start with `http` or `https`, be at most 1024 characters long, and its domain must be in `skins.urlDomainWhitelist`. Sub-domains are accepted. `"*"` in the list allows every domain.
- Default whitelist: `textures.minecraft.net`, `i.imgur.com`, `s.namemc.com`, `namemc.com`, `mc-heads.net`, `minotar.net`, `crafatar.com`, `raw.githubusercontent.com`, `cdn.discordapp.com`, `media.discordapp.net`.
- Links to a NameMC skin page and to an Imgur page are rewritten to the direct image address.
- The image is downloaded by each client, not by the server. A client can turn this off with `display.remoteSkins` (the default skin is then shown), limits the size with `network.maxSkinDownloadKb`, follows at most 3 redirects, checks the whitelist at every step and refuses local network addresses.
- In a command the address is typed as it is, without quotes. Add `true` or `false` after it to choose slim arms (`false` by default for URL skins).

### Player name skins

`/laura skin player <name>`, or the "Player name" field of the Skins page.

- `skins.allowPlayerNameSkins` must be true.
- The name has 1 to 16 letters, digits or underscores.
- The server looks the account up through the public Mojang API and keeps the answer for one hour.
- The result is stored as a URL skin on `textures.minecraft.net`, with the arm model of that account. Keep this domain in `skins.urlDomainWhitelist`, otherwise clients refuse to download it.

### Default skin

`skins.defaultSkin` (default `builtin:laura`) is the skin of every newly summoned companion. `/laura skin reset` and the "Default skin" button of the menu apply it again.

| Value | What happens |
|---|---|
| `builtin:<name>` | The built-in skin of that name. |
| `server:<file name>` | The file of the server folder, in its current version. If the file does not exist, the standard built-in skin is used. |
| `url:<link>` | The address must pass the URL rules above (`skins.allowUrlSkins`, whitelist, length). Otherwise the standard built-in skin is used. |
| `player:<name>` | The account is looked up in the background. She wears the standard built-in skin until the answer arrives. Needs `skins.allowPlayerNameSkins`. |

### Command summary

| Command | Example |
|---|---|
| `/laura skin list` | |
| `/laura skin builtin <name>` | `/laura skin builtin laura_gothic` |
| `/laura skin file <name> [slim]` | `/laura skin file uploads/vyrriox_my_dress true` |
| `/laura skin player <name>` | `/laura skin player vyrriox` |
| `/laura skin url <url> [slim]` | `/laura skin url <direct link to a PNG> false` |
| `/laura skin reset` | |

---

## Français

### Les quatre types de skins

| Type | Référence | D'où vient l'image | Interrupteur |
|---|---|---|---|
| Intégré | `builtin:<nom>` | Fourni avec le mod. | toujours disponible |
| Fichier du serveur | `server:<nom>` | Un PNG du dossier `config/lauramod/skins` du serveur. Les clients le téléchargent depuis le serveur. | toujours disponible |
| URL | `url:<lien>` | Une adresse internet. Chaque client la télécharge. | `skins.allowUrlSkins` |
| Pseudo de joueur | `player:<pseudo>` | Le skin d'un compte Minecraft, trouvé par le serveur. | `skins.allowPlayerNameSkins` |

Son skin se change depuis l'onglet Style de son menu (touche K) ou avec `/laura skin`.

Qui peut changer son apparence : son partenaire, les opérateurs (niveau de permission 2), et tout le monde quand `skins.othersCanChangeSkin` vaut true.

Changer son skin lui fait commenter son nouveau look, réalise le désir `new_outfit` et compte pour le progrès « Nouveau look ».

### Skins intégrés

| Nom | Libellé |
|---|---|
| `laura` | Laura |
| `laura_summer` | Été |
| `laura_winter` | Hiver |
| `laura_night` | Pyjama |
| `laura_sporty` | Sportive |
| `laura_gothic` | Gothique |

Commande : `/laura skin builtin laura_winter`. Dans le menu, ce sont les six boutons à visage en haut de la page Skins.

### Règles d'un fichier de skin

Un fichier de skin est un skin Minecraft standard :

- Format PNG.
- La largeur est un multiple de 64, de 64 à 1024. La hauteur est égale à la largeur, ou à sa moitié. Tailles valides : 64x64, 64x32, 128x128, 128x64 et ainsi de suite.
- Les anciens skins 64x32 sont convertis à la disposition 64x64.
- Poids jusqu'à `skins.maxSkinKb` (256 Ko).

L'interrupteur « bras fins » choisit entre le modèle à bras fins (3 pixels de large) et le modèle classique (4 pixels de large).

### Fichiers de skin du serveur

Pour les propriétaires de serveur, ou pour votre propre partie solo :

1. Déposez des fichiers PNG dans `config/lauramod/skins/`. Les sous-dossiers sont autorisés, leur chemin fait partie du nom.
2. Lancez `/laura reload` (le dossier est aussi relu au démarrage, et quand un nom inconnu est demandé).
3. Les joueurs choisissent le fichier dans la liste « Skins du serveur », ou utilisent `/laura skin file <nom> [slim]`.

Règles :

- Le nom est le chemin dans le dossier, sans `.png`. `skins/summer/beach.png` s'appelle `summer/beach`. La commande est `/laura skin file summer/beach`, sans guillemets.
- Caractères autorisés dans les noms : lettres, chiffres, `_`, `-`, `.`, `/` et espace. 96 caractères au plus.
- Un fichier trop lourd ou qui n'est pas un skin valide est ignoré et signalé dans le journal du serveur.
- Les clients téléchargent le fichier une fois et le gardent dans `<dossier du jeu>/lauramod/cache/skins`. Un client refuse les fichiers plus lourds que son propre `network.maxSkinDownloadKb` (512 Ko).
- `/laura skin list` affiche les skins intégrés et les fichiers du serveur.

### Envoi par les joueurs

Sur un serveur multijoueur, un joueur peut envoyer un fichier de skin depuis son ordinateur. `skins.allowUploads` doit valoir true (c'est le cas par défaut), et vous devez avoir le droit de changer l'apparence de la compagne dont le menu est ouvert.

Exemple complet :

1. Enregistrez votre skin sous `config/lauramod/skins/my_dress.png` dans **votre** dossier de jeu. L'onglet Style propose un bouton « Ouvrir le dossier des skins » sur la page Modèles.
2. Ouvrez son menu (K), onglet Style, page Skins.
3. Cliquez sur `my_dress.png` dans la liste « Tes skins ». Vous pouvez aussi glisser-déposer n'importe quel fichier `.png` sur la fenêtre du menu.
4. Le fichier est envoyé au serveur. Un message en bas de l'onglet le confirme, ou explique le refus (envois désactivés, pas le droit de changer son apparence, limite d'envois atteinte, fichier trop lourd, skin invalide). Rien n'est enregistré sur le serveur quand l'envoi est refusé.
5. Le serveur l'enregistre sous `config/lauramod/skins/uploads/<votre pseudo>_my_dress.png` et le lui met aussitôt.
6. Désormais tous les joueurs voient `uploads/<votre pseudo>_my_dress` dans la liste « Skins du serveur ».

Remarques :

- Chaque joueur peut garder 10 fichiers de skin sur le serveur (`skins.maxUploadsPerPlayer`). Envoyer un fichier du même nom remplace l'ancien et reste toujours possible.
- Un envoi réussi compte pour le progrès « Nouveau look ».
- Le nom enregistré est nettoyé : lettres minuscules, chiffres, `_` et `-` seulement, 32 caractères au plus pour chaque partie.
- En solo rien n'est envoyé : votre dossier est le dossier du serveur, et un clic applique directement le fichier.
- L'administrateur du serveur peut supprimer des fichiers du dossier `uploads` à tout moment, puis lancer `/laura reload`.

### Skins par URL

`/laura skin url <lien direct vers un PNG> [slim]`, ou le champ URL de la page Skins.

- `skins.allowUrlSkins` doit valoir true.
- L'adresse doit commencer par `http` ou `https`, faire au plus 1024 caractères, et son domaine doit figurer dans `skins.urlDomainWhitelist`. Les sous-domaines sont acceptés. `"*"` dans la liste autorise tous les domaines.
- Liste blanche par défaut : `textures.minecraft.net`, `i.imgur.com`, `s.namemc.com`, `namemc.com`, `mc-heads.net`, `minotar.net`, `crafatar.com`, `raw.githubusercontent.com`, `cdn.discordapp.com`, `media.discordapp.net`.
- Les liens vers une page de skin NameMC et vers une page Imgur sont réécrits en adresse directe de l'image.
- L'image est téléchargée par chaque client, pas par le serveur. Un client peut désactiver cela avec `display.remoteSkins` (le skin par défaut est alors affiché), limite le poids avec `network.maxSkinDownloadKb`, suit au plus 3 redirections, vérifie la liste blanche à chaque étape et refuse les adresses du réseau local.
- Dans une commande, l'adresse s'écrit telle quelle, sans guillemets. Ajoutez `true` ou `false` après elle pour choisir les bras fins (`false` par défaut pour les skins par URL).

### Skins par pseudo de joueur

`/laura skin player <pseudo>`, ou le champ « Pseudo d'un joueur » de la page Skins.

- `skins.allowPlayerNameSkins` doit valoir true.
- Le pseudo compte de 1 à 16 lettres, chiffres ou tirets bas.
- Le serveur recherche le compte par l'API publique de Mojang et garde la réponse pendant une heure.
- Le résultat est enregistré comme un skin par URL sur `textures.minecraft.net`, avec le modèle de bras de ce compte. Gardez ce domaine dans `skins.urlDomainWhitelist`, sinon les clients refusent de le télécharger.

### Skin par défaut

`skins.defaultSkin` (par défaut `builtin:laura`) est le skin de toute compagne nouvellement invoquée. `/laura skin reset` et le bouton « Skin par défaut » du menu l'appliquent de nouveau.

| Valeur | Ce qui se passe |
|---|---|
| `builtin:<nom>` | Le skin intégré de ce nom. |
| `server:<nom de fichier>` | Le fichier du dossier du serveur, dans sa version actuelle. Si le fichier n'existe pas, le skin intégré standard est utilisé. |
| `url:<lien>` | L'adresse doit respecter les règles des skins par URL ci-dessus (`skins.allowUrlSkins`, liste blanche, longueur). Sinon le skin intégré standard est utilisé. |
| `player:<pseudo>` | Le compte est recherché en arrière-plan. Elle porte le skin intégré standard jusqu'à l'arrivée de la réponse. Demande `skins.allowPlayerNameSkins`. |

### Résumé des commandes

| Commande | Exemple |
|---|---|
| `/laura skin list` | |
| `/laura skin builtin <nom>` | `/laura skin builtin laura_gothic` |
| `/laura skin file <nom> [slim]` | `/laura skin file uploads/vyrriox_my_dress true` |
| `/laura skin player <pseudo>` | `/laura skin player vyrriox` |
| `/laura skin url <url> [slim]` | `/laura skin url <lien direct vers un PNG> false` |
| `/laura skin reset` | |
