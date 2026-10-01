# Compatibility

My Girlfriend Laura 2.0.0 (mod id `lauramod`), author / auteur : vyrriox.

- [English](#english)
- [Français](#français)

Related guides / Guides liés : [ACTIONS.md](ACTIONS.md), [KUBEJS.md](KUBEJS.md), [BUILDING.md](BUILDING.md).

---

## English

### Targets

Version 2.0.0 exists for Minecraft 1.21.1 on NeoForge, Forge and Fabric. Other targets are listed in the README of the repository: <https://github.com/laforetbrut/lauramod>.

The behaviour described in the guides is the same on the three loaders, because it lives in shared code. Only the optional integrations below differ. None of them is required and none is bundled in the mod.

### Summary by loader (1.21.1)

| Integration | NeoForge | Forge | Fabric |
|---|---|---|---|
| Modded storage blocks (fetch, jobs, assigned chests) | yes, item handlers | yes, item handlers | yes, Transfer API |
| Backpack worn on her back | yes | yes | yes |
| Backpack used as extra storage | yes, item handlers | yes, item handlers | no |
| Applied Energistics 2 network | yes | no | no |
| Curios trinkets | yes | no | no |
| Farmer's Delight cooking pot | yes | no | yes, Farmer's Delight Refabricated |
| KubeJS binding and events | yes | no | no |
| Carry On blacklist | yes | yes | yes |
| Waystones and other teleports | yes | yes | yes |

**Fabric note.** On Fabric 1.21.1 only Farmer's Delight Refabricated is integrated. The Applied Energistics 2, Curios and KubeJS integrations do not exist on Fabric. Backpack storage and trinkets are not wired on Fabric yet: she can wear a backpack, but it does not add storage.

**Forge note.** The Forge 1.21.1 project of version 2.0.0 contains no Applied Energistics 2, Curios, Farmer's Delight or KubeJS bridge. Modded storage and backpack storage work through Forge item handlers.

### Modded storage

Every container she uses (fetch, pantry, job chests, deposits) is reached through the loader's item API, so storage blocks of other mods work like vanilla chests.

When she searches an area on her own, these blocks are skipped because they are machines, not storage: furnaces, brewing stands, hoppers, dispensers and droppers, jukeboxes, lecterns, chiseled bookshelves, campfires and crafters.

### Applied Energistics 2

[Applied Energistics 2](https://appliedenergistics.org/), NeoForge only.

- Any block of a powered ME network (interface, ME chest, drive, cable with a terminal...) gives her the whole network storage, not only the slots of that block.
- Fetch: with such a block within `fetch.radius`, "bring me 32 iron ingots" takes them from the network.
- Jobs: assign a network block as a chest (`/laura chest storage` while looking at it) and she stores her production in the network.
- Taking and storing items uses the energy of the network, like a terminal.

### Curios

[Curios](https://modrinth.com/mod/curios), NeoForge only.

- She has these trinket slots: `necklace`, `ring`, `bracelet`, `charm`, `head`, `back`, `belt`.
- Right click her with a trinket. It goes into a free slot that accepts it. She gains 8 affection and you get the "Put a Ring on It" advancement.
- Food and items listed in `gifts.json` are treated as food or gifts, not as trinkets.
- Her inventory screen does not show trinket slots in 2.0.0.

### Farmer's Delight

[Farmer's Delight](https://modrinth.com/mod/farmers-delight) on NeoForge, [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) on Fabric.

As a cook (job or single task) she also uses cooking pots that stand in her work area:

1. She picks a pot recipe for which she carries every ingredient and the serving containers (bowls for example). Recipes with up to 6 ingredients are supported.
2. She fills a heated, empty pot.
3. She waits, then collects the meals and stores them like her other dishes.

Ingredients come from her inventory and from chests assigned to `ingredients`.

### KubeJS

[KubeJS](https://kubejs.com/), NeoForge only. Scripts get the `Laura` binding and the `LauraEvents` event group. See [KUBEJS.md](KUBEJS.md).

### Backpacks

She can wear one item on her back.

- Accepted items: everything in the item tag `lauramod:wearable_on_back`, and every item whose id contains `backpack`.
- Default content of the tag: `minecraft:bundle` and the backpacks of [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) (`backpack`, `copper_backpack`, `iron_backpack`, `gold_backpack`, `diamond_backpack`, `netherite_backpack`).
- Put it on: right click her with it while her back is free, or use the back slot of her inventory. Take it off from the same slot.
- The item is drawn on her back.
- On NeoForge and Forge, a backpack that exposes its inventory through an item handler becomes extra storage. Items go to her own bag first, then to the backpack. Jobs, fetch and pickup use both.
- An item without such an inventory (the bundle for example) is worn for the look only.

Worked example, a datapack that lets her wear a satchel of another mod. File `data/lauramod/tags/item/wearable_on_back.json`:

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:satchel", "required": false }
  ]
}
```

### Fetch tag

The block tag `lauramod:fetch_harvestable` lists what she may break to fetch an item. Default content: `#minecraft:logs`, `#minecraft:flowers`, `#minecraft:crops`, sugar cane, pumpkin, melon, cactus, bamboo, sweet berry bush, brown and red mushrooms, cocoa, nether wart, kelp, short and tall grass, fern and large fern, vine, glow lichen, moss carpet.

Add modded plants with a datapack file `data/lauramod/tags/block/fetch_harvestable.json`:

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:blueberry_bush", "required": false }
  ]
}
```

### Carry On

[Carry On](https://modrinth.com/mod/carry-on): she is in the tag `carryon:entity_blacklist`, so players cannot pick her up.

### Waystones and other teleports

[Waystones](https://modrinth.com/mod/waystones), home and teleport commands, ender pearls: no special support is needed. Every two seconds the mod checks the companions that follow you. One that is further than `follow.teleportDistance` or in another dimension is brought next to you. One left in unloaded chunks is recalled.

Companions in stay, home, wander or work mode are not moved.

---

## Français

### Cibles

La version 2.0.0 existe pour Minecraft 1.21.1 sur NeoForge, Forge et Fabric. Les autres cibles sont listées dans le README du dépôt : <https://github.com/laforetbrut/lauramod>.

Le comportement décrit dans les guides est le même sur les trois chargeurs, car il se trouve dans du code partagé. Seules les intégrations facultatives ci-dessous diffèrent. Aucune n'est obligatoire et aucune n'est incluse dans le mod.

### Résumé par chargeur (1.21.1)

| Intégration | NeoForge | Forge | Fabric |
|---|---|---|---|
| Rangements de mods (rapporter, métiers, coffres attribués) | oui, item handlers | oui, item handlers | oui, Transfer API |
| Sac à dos porté sur son dos | oui | oui | oui |
| Sac à dos utilisé comme rangement supplémentaire | oui, item handlers | oui, item handlers | non |
| Réseau Applied Energistics 2 | oui | non | non |
| Bijoux Curios | oui | non | non |
| Marmite de Farmer's Delight | oui | non | oui, Farmer's Delight Refabricated |
| Liaison et événements KubeJS | oui | non | non |
| Liste noire de Carry On | oui | oui | oui |
| Waystones et autres téléportations | oui | oui | oui |

**Note Fabric.** Sur Fabric 1.21.1, seul Farmer's Delight Refabricated est intégré. Les intégrations Applied Energistics 2, Curios et KubeJS n'existent pas sur Fabric. Le rangement des sacs à dos et les bijoux ne sont pas encore câblés sur Fabric : elle peut porter un sac à dos, mais il n'ajoute pas de rangement.

**Note Forge.** Le projet Forge 1.21.1 de la version 2.0.0 ne contient aucun pont Applied Energistics 2, Curios, Farmer's Delight ou KubeJS. Les rangements de mods et le rangement des sacs à dos fonctionnent grâce aux item handlers de Forge.

### Rangements de mods

Chaque conteneur qu'elle utilise (rapporter, garde-manger, coffres de métier, dépôts) est atteint par l'API d'objets du chargeur : les rangements d'autres mods fonctionnent comme les coffres du jeu de base.

Quand elle fouille une zone d'elle-même, ces blocs sont ignorés car ce sont des machines, pas des rangements : fourneaux, alambics, entonnoirs, distributeurs et droppers, jukebox, lutrins, bibliothèques sculptées, feux de camp et fabricateurs.

### Applied Energistics 2

[Applied Energistics 2](https://appliedenergistics.org/), NeoForge uniquement.

- N'importe quel bloc d'un réseau ME alimenté (interface, coffre ME, lecteur, câble avec un terminal...) lui donne accès à tout le stockage du réseau, pas seulement aux emplacements de ce bloc.
- Rapporter : avec un tel bloc dans le rayon `fetch.radius`, « apporte-moi 32 lingots de fer » les prend dans le réseau.
- Métiers : attribuez un bloc du réseau comme coffre (`/laura chest storage` en le regardant) et elle range sa production dans le réseau.
- Prendre et ranger des objets consomme l'énergie du réseau, comme un terminal.

### Curios

[Curios](https://modrinth.com/mod/curios), NeoForge uniquement.

- Elle dispose de ces emplacements de bijoux : `necklace`, `ring`, `bracelet`, `charm`, `head`, `back`, `belt`.
- Faites un clic droit sur elle avec un bijou. Il va dans un emplacement libre qui l'accepte. Elle gagne 8 points d'affection et vous obtenez le progrès « La bague au doigt ».
- La nourriture et les objets listés dans `gifts.json` sont traités comme de la nourriture ou des cadeaux, pas comme des bijoux.
- Son écran d'inventaire n'affiche pas les emplacements de bijoux en 2.0.0.

### Farmer's Delight

[Farmer's Delight](https://modrinth.com/mod/farmers-delight) sur NeoForge, [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) sur Fabric.

Cuisinière (métier ou tâche unique), elle utilise aussi les marmites placées dans sa zone de travail :

1. Elle choisit une recette de marmite dont elle porte tous les ingrédients et les récipients de service (des bols par exemple). Les recettes de 6 ingrédients au plus sont gérées.
2. Elle remplit une marmite chauffée et vide.
3. Elle attend, puis récupère les plats et les range comme ses autres plats.

Les ingrédients viennent de son inventaire et des coffres attribués à `ingredients`.

### KubeJS

[KubeJS](https://kubejs.com/), NeoForge uniquement. Les scripts reçoivent la liaison `Laura` et le groupe d'événements `LauraEvents`. Voir [KUBEJS.md](KUBEJS.md).

### Sacs à dos

Elle peut porter un objet sur son dos.

- Objets acceptés : tout ce qui se trouve dans le tag d'objets `lauramod:wearable_on_back`, et tout objet dont l'identifiant contient `backpack`.
- Contenu par défaut du tag : `minecraft:bundle` et les sacs à dos de [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) (`backpack`, `copper_backpack`, `iron_backpack`, `gold_backpack`, `diamond_backpack`, `netherite_backpack`).
- Pour le lui mettre : clic droit sur elle avec l'objet quand son dos est libre, ou l'emplacement de dos de son inventaire. Retirez-le depuis ce même emplacement.
- L'objet est dessiné sur son dos.
- Sur NeoForge et Forge, un sac à dos qui expose son inventaire par un item handler devient un rangement supplémentaire. Les objets vont d'abord dans son propre sac, puis dans le sac à dos. Les métiers, la recherche d'objets et le ramassage utilisent les deux.
- Un objet sans inventaire de ce type (le sac du jeu de base par exemple) est porté uniquement pour l'apparence.

Exemple complet, un datapack qui lui permet de porter une sacoche d'un autre mod. Fichier `data/lauramod/tags/item/wearable_on_back.json` :

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:satchel", "required": false }
  ]
}
```

### Tag de récolte pour rapporter

Le tag de blocs `lauramod:fetch_harvestable` liste ce qu'elle peut casser pour rapporter un objet. Contenu par défaut : `#minecraft:logs`, `#minecraft:flowers`, `#minecraft:crops`, canne à sucre, citrouille, pastèque, cactus, bambou, buisson à baies sucrées, champignons bruns et rouges, cacao, verrues du Nether, varech, herbes courtes et hautes, fougère et grande fougère, lianes, lichen lumineux, tapis de mousse.

Ajoutez des plantes de mods avec un fichier de datapack `data/lauramod/tags/block/fetch_harvestable.json` :

```json
{
  "replace": false,
  "values": [
    { "id": "examplemod:blueberry_bush", "required": false }
  ]
}
```

### Carry On

[Carry On](https://modrinth.com/mod/carry-on) : elle figure dans le tag `carryon:entity_blacklist`, les joueurs ne peuvent donc pas la porter.

### Waystones et autres téléportations

[Waystones](https://modrinth.com/mod/waystones), commandes de maison et de téléportation, perles de l'Ender : aucune prise en charge particulière n'est nécessaire. Toutes les deux secondes, le mod vérifie les compagnes qui vous suivent. Celle qui se trouve au-delà de `follow.teleportDistance` ou dans une autre dimension est amenée près de vous. Celle qui est restée dans des chunks déchargés est rappelée.

Les compagnes en mode rester, maison, se promener ou travail ne sont pas déplacées.
