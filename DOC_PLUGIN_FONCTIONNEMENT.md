# PastequeSkyblock - Fonctionnement complet (v6.1 fix + premium pass)

## 1) Sequence de demarrage du plugin
1. Le plugin charge `config.yml`.
2. Il initialise le prefix serveur, les mondes, puis tous les managers.
3. Il enregistre les commandes:
   - `/is`
   - `/iscoop`
   - `/hdv`
   - `/money`, `/bal`, `/balance`
   - `/pay`
   - `/psky`
   - `/friends`, `/enemy`, `/alliance` (`/ally`, `/faction`)
   - `/pvpwarp`
   - `/endevent`
   - `/aend`
4. Il enregistre les listeners:
   - protections des iles
   - connexion / respawn / anti-void
   - chat ile/global
   - shops panneaux
   - GUI
   - PvP
   - ambassade
   - divers serveur (invasion + blocage /shop /sell)
   - event Ender Dragon
5. Il demarre les taches periodiques:
   - suivi des creations coop
   - bordure visuelle des iles
   - combat tag
   - invasions random
   - event End quotidien
   - purge HDV reguliere

## 2) Mondes et architecture
- Monde spawn principal: `spawn-world-name`.
- Monde solo skyblock (vide): `island-world-name`.
- Monde coop skyblock (vide): `coop-world-name`.
- Monde End event: `end-event.world-name` (ou premier THE_END detecte).
- Generateur vide: `EmptySkyblockGenerator`.

## 3) Ile solo (/is)
### Creation et base
1. `/is create` cree une ile solo unique par owner.
2. Une ile premium est generee:
   - relief organique (forme flottante plus naturelle)
   - zone centrale amenagee
   - ferme de depart
   - zone cobble gen
   - lanternes + deco
   - coffre de demarrage (eau/lave/glace/melon/graines/canne/torches/saplings...)
3. Le joueur est teleporte automatiquement sur son home.

### Homes
- `/is home`: teleporte au home principal.
- `/is sethome`: definit le home principal (owner uniquement, sur son ile).
- `/is farm`: teleporte au home farming (si debloque).
- `/is mine`: teleporte au home minage (si debloque).

### Extensions solo
- `/is unlock farming`
  - debite `island-expansions.solo.farming-price`
  - construit une ile farming premium (pont premium + champs irrigues + verger + coffre outils)
  - definit `farmingHome`.
- `/is unlock mining`
  - debite `island-expansions.solo.mining-price`
  - construit une ile minage premium (pont premium + quarry + veines de minerais + coffre minage)
  - definit `miningHome`.

### Gestion ile
- `/is rename <nom>`
- `/is members` (GUI expulsion)
- `/is invite <joueur>`
- `/is accept`
- `/is deny`
- `/is kick <joueur>`
- `/is trust <joueur>`
- `/is untrust <joueur>`
- `/is ban <joueur>`
- `/is unban <joueur>`
- `/is public`
- `/is private`
- `/is pvp [on/off]`
- `/is visit <joueur>`

### Progression et economie ile
- `/is level`: calcule la valeur des blocs selon `block-values`.
- `/is stats`: profil ile complet (nom, owner, membres, niveau, banque, unlocks).
- `/is top`: top 10 des iles par niveau.
- `/is bank`
- `/is bank deposit <montant>`
- `/is bank withdraw <montant>`

### Securite ile
- `/is reset` + `/is reset confirm` (confirmation 20s)
- `/is delete` + `/is delete confirm` (confirmation 20s)
- `/is rollback` (restaure dernier reset/delete capture)

### Chat/deifs
- `/is chat` (toggle chat ile force)
- `/is challenges` (GUI defis)

## 4) Ambassade (unlock invitations)
- Quand l'owner place `embassy.unlock-material` (par defaut `MELON_BLOCK`) sur son ile:
  - +1 invitation unlock
  - effets visuels + son + feu d'artifice
- Sans unlock ambassade, `/is invite` est bloque.

## 5) Ile coop (/iscoop)
### Creation multi-joueurs
1. `/iscoop create <joueurs...>` (2 a 6 joueurs total).
2. Tous les invites doivent accepter.
3. Si tous valident avant timeout:
   - creation de la coop
   - generation ile coop premium
   - teleportation de tous les membres.

### Commandes coop
- `/iscoop accept`
- `/iscoop deny`
- `/iscoop pending`
- `/iscoop list` (nouv: liste des coops, IDs, owner)
- `/iscoop home [id]`
- `/iscoop sethome`
- `/iscoop rename <nom>`
- `/iscoop unlock mine`
- `/iscoop mine [id]` (nouv: home minage coop)

### Generation coop premium
- Ile principale plus vaste et plus propre:
  - relief organique
  - place centrale amenagee
  - axe de circulation
  - arbres distribues
  - coffres de depart multiples
  - eclairage.
- Ile minage coop:
  - pont premium
  - relief minier
  - quarry central
  - veines de minerais
  - coffre equipement minage.

## 6) Protections globales
- Anti-build hors droits (solo + coop).
- Anti-open conteneurs hors droits.
- Anti-feu / anti-ignite sur mondes iles.
- Blocage flux inter-iles (eau/lave).
- Blocage pistons inter-zones.
- Blocage explosions sur mondes proteges.
- Si visiteur banni/prive/non membre coop:
  - renvoi spawn.

## 7) Combat / PvP
- PvP autorise uniquement:
  - zones PvP admin (`/psky pvp ...`)
  - ou ile solo avec PvP active.
- Combat tag:
  - tag 30s configurable
  - deconnexion pendant tag:
    - inventaire vide
    - perte eco (`combat.logout-loss`)
    - message de sanction stocke et affiche au reconnect.
- Vol eco en kill PvP:
  - `combat.pvp-steal-percent` avec cap `combat.pvp-steal-cap`.
- Monde externe arena (`external-arena-world-name`) est exclu de ces regles plugin.

## 8) Chat
- En monde iles solo:
  - message avec `@` en tete -> canal global du monde ile.
  - sinon canal local ile/plot.
  - avec `/is chat` actif -> canal equipe ile force.

## 9) Economie
- Solde persistant joueur.
- Solde initial: `economy.starting-balance`.
- Monnaie: `economy.currency-name`.
- Commandes:
  - `/money`, `/bal`, `/balance`
  - `/pay <joueur> <montant>`
- Nouveaute:
  - bonus quotidien de connexion (`economy.daily-login-reward`)
  - stocke dans `daily-rewards.yml`.

## 10) HDV (hotel des ventes)
- `/hdv` ouvre GUI pagine.
- `/hdv page <n>`
- `/hdv vendre <prix> <quantite>`:
  - prend l'objet en main
  - retire frais publication `auction.listing-fee`
  - limite annonces par joueur `auction.max-active-listings-per-player`.
- `/hdv recup`
- `/hdv recup all`
- Achat via clic GUI:
  - debit acheteur
  - credit vendeur
  - transfert item.
- Expiration auto:
  - retour item en claimable.

## 11) Shops panneaux ([PSHOP])
- Creation reservee admin.
- Format:
  - ligne 1: tag
  - ligne 2: material
  - ligne 3: amount
  - ligne 4: prix achat/vente (B/S)
- Clic droit = achat.
- Clic gauche = vente.

## 12) Defis
- Defis config dans `config.yml`.
- GUI `/is challenges`.
- Verifie items requis, retire inventaire, donne recompense eco, marque complete.

## 13) Social
- Amis:
  - `/friends add/remove/list`
- Ennemis:
  - `/enemy add/remove/list`
- Alliance:
  - `/alliance create <nom>`
  - `/alliance invite <joueur>`
  - `/alliance accept`
  - `/alliance deny`
  - `/alliance leave`
  - `/alliance list`

## 14) Invasions hub
- Invasion random periodique (ou manuelle admin).
- Spawn mobs autour du spawn hub.
- Mobs tagges metadata `pasteque_invasion`.
- Kill d'un mob invasion:
  - drop melons
  - gain eco `invasions.kill-reward`.

## 15) End Event - Pasteque Dragon
### Lancement
- Auto quotidien a 20:00 (une fois par jour) ou manuel via `/aend launch`.
- Nettoie monde End event.
- Spawn du `Pasteque Dragon` (nom custom + vie config).

### Correctif applique (bug dragon qui meurt instant)
- Ajout d'une stabilisation de lancement.
- Si dragon invalide/disparu pendant event:
  - respawn automatique du dragon
  - l'event continue (pas de fin instantanee).
- Si mort pendant stabilisation:
  - relance automatique du dragon.
- L'event se termine seulement:
  - timer expire (`stop(false)`)
  - ou kill valide du dragon tracked (`stop(true)`).

### Joueurs
- `/endevent` -> teleport event si actif.
- `/endevent status` -> temps restant si actif.

### Admin End
- `/aend launch`
- `/aend stop`
- `/aend setspawn`

### Recompenses fin event
- Top degats:
  - 1er: 100000
  - 2e: 50000
  - 3e: 25000
  - 4e: 25000
- Dernier coup: +15000

## 16) Admin (/psky)
- `/psky setspawn`
- `/psky setpvpwarp`
- `/psky createfor <joueur>`
- `/psky reset <joueur>`
- `/psky tp <joueur>`
- `/psky islands`
- `/psky reload`
- `/psky money <give|take|set> <joueur> <montant>`
- `/psky hdv list`
- `/psky hdv remove <id>`
- `/psky events invasion`
- `/psky pvp pos1`
- `/psky pvp pos2`
- `/psky pvp create <nom>`
- `/psky pvp delete <nom>`

## 17) GUI disponibles
- Menu principal `/is`:
  - homes, unlocks, pvp, niveau, banque, top, profil, membres, etc.
- Membres ile:
  - expulsion via clic.
- Defis:
  - validation via clic.
- HDV:
  - pages, achat, retours.
- Coop:
  - creation, list, mine, unlock mine.
- Admin:
  - raccourcis moderation.

## 18) Sauvegardes (fichiers data)
- `islands.yml`
- `coop.yml`
- `economy.yml`
- `auction.yml`
- `challenges.yml`
- `social.yml`
- `pvp.yml`
- `combatlog.yml`
- `daily-rewards.yml` (ajout)

## 19) Notes techniques
- Plugin cible Spigot 1.9.4.
- Confirmation reset/delete avec expiration configurable.
- Purge HDV et taches periodiques executees par scheduler Bukkit.
