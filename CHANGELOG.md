# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-02-20

### Added / Ajouté
- **Massive Interaction Database**: Over 500+ unique localized interactions across 11 categories (Greetings, Love, Philosophy, Combat, etc.).
- **6-Block Follow Distance**: Optimized AI to stay at a "partner" distance (approx. 6 blocks) instead of standing on top of the player.
- **Stuck Detection**: Laura now complains in chat if she's stuck or cannot reach the player for more than 10 seconds.
- **Hourly Playful Interaction**: Every hour, Laura playfully hits the player and apologizes with localized dialogue.
- **Portal Support**: Laura can now follow the player through Nether and End portals.
- **Dimension Uniqueness**: Improved `LauraWorldData` ensures only one Laura exists globally across all dimensions.
- **Player Binding**: Laura is strictly bound to her summoner.
- **Anti-Corruption**: Audited NBT persistence to prevent data loss or state corruption.
- **Multilingual Excellence**: Full support for French, English, German, Spanish, Italian, and Portuguese.
- **A.I Optimization**: AI Logic optimized to run conditional checks once per second (using tick modulo), drastically improving server performance.
- **Visual Effects**: Added distinct particles when Laura spawns, takes damage while sad/angry, and when she farts.
- **Chat Formatting**: Laura's chat name is now beautifully formatted in pink.
- **Bug Fixes**: Resolved critical `mods.toml` parsing crash on NeoForge 1.21.1 and fixed missing dependencies for Forge 1.20.1.

---

- **Base d'Interactions Massive**: Plus de 500 interactions localisées uniques à travers 11 catégories.
- **Distance de Suivi de 6 Blocs**: IA optimisée pour maintenir une distance de "partenaire".
- **Détection de Blocage**: Laura se plaint si elle est coincée pendant plus de 10 secondes.
- **Interaction Ludique Horaire**: Toutes les heures, Laura frappe joyeusement le joueur et s'excuse.
- **Support des Portails**: Laura peut maintenant suivre le joueur à travers les portails du Nether et de l'End.
- **Unicité Dimensionnelle**: `LauraWorldData` garantit une seule Laura globalement.
- **Liaison au Joueur**: Laura est liée strictement à son invocateur.
- **Anti-Corruption**: Audit de la persistance NBT.
- **Excellence Multilingue**: Support complet (FR, EN, DE, ES, IT, PT).
- **Optimisation de l'IA**: La logique globale de l'IA a été optimisée grâce à un système de modulo limitant les opérations lourdes à 1 fois par seconde, améliorant drastiquement les performances serveur.
- **Effets Visuels**: Ajout de particules visuelles distinctes lors de l'apparition de l'entité, de blessures (triste/en colère), et lors de flatulences.
- **Pseudo Rose**: Le nom de Laura s'affiche désormais nativement en rose (`<§dLaura§r>`) dans le tchat.
- **Correctifs de Bugs**: Résolution du crash critique de démarrage sur NeoForge 1.21.1 (TOML config) et réparation de la compilation Forge 1.20.1.

## [1.0.0] - 2026-02-16

### Added / Ajouté
- Initial Release with 1000 HP, Regeneration, Inventory, and Social logic.
- 6 Languages support (Basic).
- Inventory System (9 slots).
