# Page media

The animated banners of the project page live in `media/`. They are made from footage recorded in
game with a shader pack, then framed and dressed with titles, cards and hearts drawn with the icons
and the palette of the mod.

| File | What it shows |
|---|---|
| `hero.gif` | The title banner |
| `meet-en.gif`, `meet-fr.gif` | The chat phrase that calls her, and her arrival |
| `talk-en.gif`, `talk-fr.gif` | An order typed in chat, and her answer |
| `needs.gif` | Her needs, a wish, and what food does |
| `work.gif` | The farmer job |
| `outfits.gif` | The six built-in outfits |
| `emotes.gif` | The emote wheel and four emotes |
| `menu-en.gif`, `menu-fr.gif` | The seven tabs of her menu |

Every file is 800 pixels wide, plays at 20 frames per second and weighs less than 2 MB, the limit
of CurseForge for an uploaded image.

## Building the banners

Requirements: Python 3 with Pillow and numpy, and ffmpeg on the PATH.

```
python tools/media/make_media.py              # every banner
python tools/media/make_media.py hero work    # only these two
```

The script reads the footage from `neoforge-1.21.1/run-capture/screenshots` (`--src` to read it
elsewhere) and writes to `media/` (`--out`). The footage is not in the repository: it is about
2,000 PNG frames and 6 GB.

- `make_media.py`: one function per banner.
- `kit.py`: easing, sprites, cards, looping particles, footage helpers and the GIF encoding (one
  palette for the whole clip, ordered dithering).
- `pixelfont.py`: the pixel font of the titles, with the French accents.

How a banner stays small: the background is one still frame, and only the area where she moves
comes from the footage, with a soft edge. Pixels that barely change from one frame to the next are
kept identical, so the file only stores what really moves.

## Recording the footage

The footage is recorded by `capture/CaptureStudio.java`, a scripted camera. It is not part of the
mod and must never be committed into its sources.

1. From the repository root, add the tool to the 1.21.1 sources:

   ```
   git apply tools/media/capture/hooks.patch
   cp tools/media/capture/CaptureStudio.java common/1.21.1/src/main/java/com/vyrriox/lauramod/client/
   ```

   The patch adds one call in `LauraClient` and a `runCapture` run: a 1600 x 900 window, with
   `neoforge-1.21.1/run-capture` as game folder.
2. In `neoforge-1.21.1/run-capture/`, put Iris and Sodium for NeoForge 1.21.1 in `mods/`, a shader
   pack in `shaderpacks/`, and name the pack in `config/iris.properties`. The banners were recorded
   with Iris 1.8.12, Sodium 0.6.13 and Complementary Unbound r5.9.3. None of these files is
   distributed here.
3. Copy the scripts of `tools/media/capture/` into `neoforge-1.21.1/run-capture/`, then run them in
   order from `neoforge-1.21.1`:

   ```
   ./gradlew runCapture -PcaptureScript=1-scenes.txt
   ```

   Each run opens the world, plays the script and closes the game. The frames are written to
   `run-capture/screenshots`.
4. Before `5-farm.txt`, copy `saves/laura_capture` to `saves/laura_farm` (the script plants a wheat
   field there), and create `shaderpacks/<file name of the pack>.txt` with the line
   `WAVING_SPEED=0.00`, so that the plants stand still. Delete that file afterwards.
5. Undo the hooks:

   ```
   git checkout -- common/1.21.1/src/main/java/com/vyrriox/lauramod/client/LauraClient.java neoforge-1.21.1/build.gradle
   rm common/1.21.1/src/main/java/com/vyrriox/lauramod/client/CaptureStudio.java
   ```

Notes on the scripts:

- The scenes are set in a cherry grove of the world that the first script creates (seed 20261001),
  around `877 112 -766`. The grass around that spot was removed beforehand with
  `/fill 866 109 -782 888 117 -756 air replace minecraft:short_grass`, then the same command with
  `minecraft:tall_grass`.
- A script holds one command per line. The commands are listed in the `run` and `laura` methods of
  `CaptureStudio.java`.
- A later script replaces the shots of an earlier one that have the same name: `3-home-tab.txt` and
  `4-settings-tab.txt` take two tabs of the menu again.
- `5-farm.txt` logs `[CAPTURE] crops farm <frame> <count>` each time a crop is cut. These frame
  numbers are the `HARVESTS` list of `make_media.py`.

Author: vyrriox

---

# Médias de la page (Version Française)

Les bannières animées de la page du projet se trouvent dans `media/`. Elles sont faites à partir
d'images enregistrées en jeu avec un pack de shaders, puis cadrées et habillées de titres, de
cartes et de cœurs dessinés avec les icônes et la palette du mod.

| Fichier | Ce qu'il montre |
|---|---|
| `hero.gif` | La bannière de titre |
| `meet-en.gif`, `meet-fr.gif` | La phrase du chat qui l'appelle, et son arrivée |
| `talk-en.gif`, `talk-fr.gif` | Un ordre tapé dans le chat, et sa réponse |
| `needs.gif` | Ses besoins, une envie, et l'effet de la nourriture |
| `work.gif` | Le métier de fermière |
| `outfits.gif` | Les six tenues intégrées |
| `emotes.gif` | La roue des émotes et quatre émotes |
| `menu-en.gif`, `menu-fr.gif` | Les sept onglets de son menu |

Chaque fichier fait 800 pixels de large, tourne à 20 images par seconde et pèse moins de 2 Mo, la
limite de CurseForge pour une image envoyée.

## Générer les bannières

Prérequis : Python 3 avec Pillow et numpy, et ffmpeg dans le PATH.

```
python tools/media/make_media.py              # toutes les bannières
python tools/media/make_media.py hero work    # seulement ces deux-là
```

Le script lit les images dans `neoforge-1.21.1/run-capture/screenshots` (`--src` pour les lire
ailleurs) et écrit dans `media/` (`--out`). Les images sources ne sont pas dans le dépôt : il y en
a environ 2 000, au format PNG, pour 6 Go.

- `make_media.py` : une fonction par bannière.
- `kit.py` : courbes d'animation, sprites, cartes, particules en boucle, outils pour les images
  sources et encodage GIF (une palette pour tout le clip, tramage ordonné).
- `pixelfont.py` : la police pixel des titres, avec les accents français.

Pourquoi une bannière reste légère : le fond est une seule image fixe, et seule la zone où elle
bouge vient de l'enregistrement, avec un bord adouci. Les pixels qui changent à peine d'une image
à l'autre sont gardés identiques, et le fichier ne stocke que ce qui bouge vraiment.

## Enregistrer les images

Les images sont enregistrées par `capture/CaptureStudio.java`, une caméra pilotée par script. Elle
ne fait pas partie du mod et ne doit jamais être commitée dans ses sources.

1. Depuis la racine du dépôt, ajoutez l'outil aux sources 1.21.1 :

   ```
   git apply tools/media/capture/hooks.patch
   cp tools/media/capture/CaptureStudio.java common/1.21.1/src/main/java/com/vyrriox/lauramod/client/
   ```

   Le patch ajoute un appel dans `LauraClient` et un lancement `runCapture` : une fenêtre de
   1600 x 900, avec `neoforge-1.21.1/run-capture` comme dossier de jeu.
2. Dans `neoforge-1.21.1/run-capture/`, placez Iris et Sodium pour NeoForge 1.21.1 dans `mods/`, un
   pack de shaders dans `shaderpacks/`, et indiquez le pack dans `config/iris.properties`. Les
   bannières ont été enregistrées avec Iris 1.8.12, Sodium 0.6.13 et Complementary Unbound r5.9.3.
   Aucun de ces fichiers n'est distribué ici.
3. Copiez les scripts de `tools/media/capture/` dans `neoforge-1.21.1/run-capture/`, puis lancez-les
   dans l'ordre depuis `neoforge-1.21.1` :

   ```
   ./gradlew runCapture -PcaptureScript=1-scenes.txt
   ```

   Chaque lancement ouvre le monde, joue le script et ferme le jeu. Les images sont écrites dans
   `run-capture/screenshots`.
4. Avant `5-farm.txt`, copiez `saves/laura_capture` vers `saves/laura_farm` (le script y plante un
   champ de blé), et créez `shaderpacks/<nom du fichier du pack>.txt` avec la ligne
   `WAVING_SPEED=0.00`, pour que les plantes restent immobiles. Supprimez ce fichier ensuite.
5. Retirez les ajouts :

   ```
   git checkout -- common/1.21.1/src/main/java/com/vyrriox/lauramod/client/LauraClient.java neoforge-1.21.1/build.gradle
   rm common/1.21.1/src/main/java/com/vyrriox/lauramod/client/CaptureStudio.java
   ```

Notes sur les scripts :

- Les scènes se passent dans une cerisaie du monde que crée le premier script (graine 20261001),
  autour de `877 112 -766`. L'herbe autour de cet endroit a été retirée au préalable avec
  `/fill 866 109 -782 888 117 -756 air replace minecraft:short_grass`, puis la même commande avec
  `minecraft:tall_grass`.
- Un script contient une commande par ligne. Les commandes sont listées dans les méthodes `run` et
  `laura` de `CaptureStudio.java`.
- Un script remplace les prises d'un script précédent qui portent le même nom : `3-home-tab.txt` et
  `4-settings-tab.txt` reprennent deux onglets du menu.
- `5-farm.txt` écrit `[CAPTURE] crops farm <image> <nombre>` dans le journal à chaque plant coupé.
  Ces numéros d'image forment la liste `HARVESTS` de `make_media.py`.

Author: vyrriox
