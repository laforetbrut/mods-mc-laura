# Security Policy

My Girlfriend Laura (mod id `lauramod`), author / auteur : vyrriox.

- [English](#security-policy)
- [Français](#politique-de-sécurité)

## Supported versions

| Version | Supported |
|---------|-----------|
| 2.0.x   | Yes       |
| 1.x     | No        |

## Reporting a vulnerability

Please do not open a public issue, discussion or pull request. Report it privately through a
[GitHub private security advisory](https://github.com/laforetbrut/lauramod/security/advisories/new).

Reports are not taken by email: the advisory is the only private channel of this project.
You will get an answer as soon as possible.

A bug without any security impact goes to a normal
[issue](https://github.com/laforetbrut/lauramod/issues/new/choose).

## What to include

- The mod version, the loader and its version, the Minecraft version.
- Where it happens: dedicated server, singleplayer or a world opened to LAN, and whether the
  attacker needs the mod on their client.
- The options you changed in `config/lauramod/lauramod-common.json` (sections `skins`, `models`,
  `permissions` and `summoning` above all) and in `config/lauramod/lauramod-client.json`
  (sections `network` and `display`).
- The steps to reproduce, with the file (skin, model), the address, the command or the packet used.
- Who can do it (any player, the partner of the companion, an operator) and what they gain:
  a crash, a file written or read outside the mod folders, an action on somebody else's
  companion, a setting of the server bypassed.
- Logs, with personal data removed (IP addresses, tokens, account details).

## Areas that matter for this mod

**File uploads (skins and models).** Players can send PNG skins to a server (`skins.allowUploads`,
on by default) and `.bbmodel` models (`models.allowUploads`, off by default). The server checks
each file, cleans its name, stores it in the `uploads` folder of `config/lauramod/skins` or
`config/lauramod/models`, then serves it to every client. Worth reporting: anything that gets
past a size limit (`skins.maxSkinKb`, `models.maxModelKb`), a per-player quota
(`maxUploadsPerPlayer`) or the name cleaning, that writes or reads outside these folders, or that
crashes a server or a client while a file is read (PNG, Blockbench model, animations, Molang).

**URL skins and the domain whitelist.** A URL skin is downloaded by each client, not by the
server. The address must belong to a domain of `skins.urlDomainWhitelist`; the client checks the
list again at every redirect, follows at most 3 of them, refuses local network addresses and
limits the size with `network.maxSkinDownloadKb`. Worth reporting: anything that makes a client
request an address outside the whitelist or inside a local network, or download more than its
limit. The lookup of player name skins, done by the server through the public Mojang API, belongs
to this area too. A server owner who puts `"*"` in the whitelist allows every domain: that is a
setting, not a vulnerability.

**The network channel.** The mod uses a single channel, `lauramod:main`, for everything the
client and the server exchange: menu actions, look changes, file downloads and uploads, speech
bubbles and status data. Worth reporting: a packet that lets a player act on a companion they may
not control, bypass an option of the server, obtain a file that is not in the skin and model
folders, or exhaust the memory of a server or of a client.

**Commands and permissions.** `/laura reload` and `/laura admin` need
`permissions.reloadPermissionLevel` (2 by default). Summoning needs
`summoning.summonPermissionLevel` (0 by default: everyone). Acting on the companion of another
player depends on `permissions.othersCanInteract` and `skins.othersCanChangeSkin` (both false by
default); operators (permission level 2) pass these checks for menu actions and look changes.
Worth reporting: any way around these checks.

The options are described in [docs/CONFIG.md](docs/CONFIG.md), the commands in
[docs/COMMANDS.md](docs/COMMANDS.md), skins and models in [docs/SKINS.md](docs/SKINS.md) and
[docs/MODELS.md](docs/MODELS.md).

---

# Politique de sécurité

## Versions prises en charge

| Version | Prise en charge |
|---------|-----------------|
| 2.0.x   | Oui             |
| 1.x     | Non             |

## Signaler une faille

Merci de ne pas ouvrir d'issue, de discussion ou de pull request publique. Signalez la faille en
privé via une
[GitHub private security advisory](https://github.com/laforetbrut/lauramod/security/advisories/new).

Aucun signalement n'est reçu par e-mail : l'advisory est le seul canal privé de ce projet.
Vous recevrez une réponse dès que possible.

Un bug sans conséquence pour la sécurité se signale dans une
[issue](https://github.com/laforetbrut/lauramod/issues/new/choose) ordinaire.

## Ce qu'il faut indiquer

- La version du mod, le chargeur et sa version, la version de Minecraft.
- Où le problème se produit : serveur dédié, solo ou monde ouvert au réseau local, et si
  l'attaquant a besoin du mod sur son client.
- Les options que vous avez modifiées dans `config/lauramod/lauramod-common.json` (surtout les
  sections `skins`, `models`, `permissions` et `summoning`) et dans
  `config/lauramod/lauramod-client.json` (sections `network` et `display`).
- Les étapes pour reproduire, avec le fichier (skin, modèle), l'adresse, la commande ou le paquet
  utilisé.
- Qui peut le faire (n'importe quel joueur, le partenaire de la compagne, un opérateur) et ce
  qu'il y gagne : un plantage, un fichier écrit ou lu hors des dossiers du mod, une action sur la
  compagne de quelqu'un d'autre, un réglage du serveur contourné.
- Les logs, sans données personnelles (adresses IP, jetons, informations de compte).

## Les zones sensibles de ce mod

**Envoi de fichiers (skins et modèles).** Les joueurs peuvent envoyer à un serveur des skins PNG
(`skins.allowUploads`, activé par défaut) et des modèles `.bbmodel` (`models.allowUploads`,
désactivé par défaut). Le serveur vérifie chaque fichier, nettoie son nom, le range dans le
dossier `uploads` de `config/lauramod/skins` ou de `config/lauramod/models`, puis le distribue à
tous les clients. À signaler : tout ce qui franchit une limite de taille (`skins.maxSkinKb`,
`models.maxModelKb`), un quota par joueur (`maxUploadsPerPlayer`) ou le nettoyage des noms, qui
écrit ou lit hors de ces dossiers, ou qui fait planter un serveur ou un client pendant la lecture
d'un fichier (PNG, modèle Blockbench, animations, Molang).

**Skins par URL et liste blanche de domaines.** Un skin par URL est téléchargé par chaque client,
pas par le serveur. L'adresse doit appartenir à un domaine de `skins.urlDomainWhitelist` ; le
client revérifie la liste à chaque redirection, en suit 3 au plus, refuse les adresses du réseau
local et limite la taille avec `network.maxSkinDownloadKb`. À signaler : tout ce qui amène un
client à interroger une adresse hors de la liste blanche ou située dans un réseau local, ou à
télécharger plus que sa limite. La recherche des skins par pseudo de joueur, faite par le serveur
auprès de l'API publique de Mojang, fait aussi partie de cette zone. Un propriétaire de serveur
qui met `"*"` dans la liste blanche autorise tous les domaines : c'est un réglage, pas une faille.

**Le canal réseau.** Le mod utilise un seul canal, `lauramod:main`, pour tout ce que le client et
le serveur échangent : actions du menu, changements d'apparence, téléchargements et envois de
fichiers, bulles de dialogue et données d'état. À signaler : un paquet qui permet à un joueur
d'agir sur une compagne qu'il n'a pas le droit de contrôler, de contourner une option du serveur,
d'obtenir un fichier qui ne se trouve pas dans les dossiers de skins et de modèles, ou d'épuiser
la mémoire d'un serveur ou d'un client.

**Commandes et permissions.** `/laura reload` et `/laura admin` demandent
`permissions.reloadPermissionLevel` (2 par défaut). L'invocation demande
`summoning.summonPermissionLevel` (0 par défaut : tout le monde). Agir sur la compagne d'un autre
joueur dépend de `permissions.othersCanInteract` et de `skins.othersCanChangeSkin` (false par
défaut pour les deux) ; les opérateurs (niveau de permission 2) passent ces vérifications pour les
actions du menu et les changements d'apparence. À signaler : tout moyen de contourner ces
vérifications.

Les options sont décrites dans [docs/CONFIG.md](docs/CONFIG.md), les commandes dans
[docs/COMMANDS.md](docs/COMMANDS.md), les skins et les modèles dans
[docs/SKINS.md](docs/SKINS.md) et [docs/MODELS.md](docs/MODELS.md).
