# Fabrique de Lunettes 

Ce dépôt contient le code source et les livrables du projet "Fabrique de Lunettes" réalisé dans le cadre du M1 MIAGE à l'IDMC.

Ce document explique comment récupérer et lancer les exécutables (`.jar`) prêts à l'emploi et décrit le protocole MQTT utilisé.

---

## Prérequis

Avant de lancer l'application, assurez-vous d'avoir les éléments suivants actifs sur votre machine :
1. **Java 21** (ou une version supérieure).
2. **Un broker MQTT** (comme [Eclipse Mosquitto](https://mosquitto.org/)). Le broker doit être lancé et écouter sur le port par défaut (`localhost:1883`).

---

## Récupération et Lancement

Les exécutables autonomes sont générés automatiquement par notre pipeline CI/CD.

### 1. Télécharger les livrables
Rendez-vous dans l'onglet **Releases** (à droite sur la page d'accueil de ce dépôt GitHub) et téléchargez les deux fichiers présents dans la section "Assets" de la dernière version :
* `lunettes-backend-1.0-SNAPSHOT-jar-with-dependencies.jar` *(Le composant Usine)*
* `lunettes-javafx-1.0-SNAPSHOT.jar` *(L'interface Client)*

### 2. Démarrer l'Usine (Backend)
Ouvrez un terminal dans le dossier où vous avez téléchargé les fichiers et lancez la commande suivante :
```bash
java -jar lunettes-backend-1.0-SNAPSHOT-jar-with-dependencies.jar
```
*L'usine va s'initialiser, se connecter au broker MQTT local et se mettre en écoute.*

### 3. Démarrer l'interface Graphique (Interface)
Laissez le premier terminal ouvert. Ouvrez un second terminal dans ce même dossier et lancez le client JavaFX:

```bash
java -jar lunettes-javafx-1.0-SNAPSHOT.jar
```
### Notes de configuration
Par défaut, l'application se connecte au broker via l'URL `tcp://localhost:1883`
Si vous devez recompiler le projet vous-même ou modifier cette adresse, vous pouvez éditer les fichiers de configuration application.properties (pour le backend) et frontend.properties (pour le frontend) présents dans le code source, puis lancer la commande mvn clean package.

## Protocole MQTT

Une commande utilise un identifiant UUID généré par le client. Le format de données est volontairement spécifique au projet et n'utilise ni JSON ni XML.

- Commande envoyée sur `orders/<uuid>` : `TYPE:QUANTITE,TYPE:QUANTITE`, par exemple `BANANA:2,CLAUDE:1`.
- Commande acceptée : `orders/<uuid>/validated`, sans données.
- Commande refusée : `orders/<uuid>/cancelled`, avec le détail de validation.
- Suivi optionnel : `orders/<uuid>/status`, avec `processing` ou `processed`.
- Livraison : `orders/<uuid>/delivery`, sous la forme `TYPE:NUMERO_SERIE,TYPE:NUMERO_SERIE`.
- Erreur de production : `orders/<uuid>/error`, avec la description de l'erreur.
- Vérification : publication vide sur `serials/<numero>/check`, réponse sur `serials/<numero>` avec le type ou `invalid`.

Une commande est refusée si son payload est mal formé, contient un type inconnu, si sa quantité totale est nulle, ou si une quantité n'est pas comprise entre 0 inclus et 10 exclu.

## Questions d'architecture

### Absence d'usine connectée

Le client vérifie la présence du backend via `backend/ping` et `backend/pong` avant d'autoriser l'envoi. Si le backend disparaît après validation d'une commande, une évolution production-ready consisterait à conserver les messages MQTT et à définir un délai d'expiration donnant lieu à une erreur visible côté client.

### Plusieurs commandes simultanées pour un même client

Le client bloque actuellement un nouvel envoi tant qu'une commande est active. Pour lever cette limite, il faut stocker plusieurs commandes actives indexées par UUID dans l'interface, conserver les abonnements de chacune, et afficher leur suivi séparément.

### Plusieurs usines

Avec plusieurs backends abonnés au même topic, une commande pourrait être traitée plusieurs fois. Il faut introduire une distribution exclusive des commandes, par exemple une file de travail MQTT dédiée avec attribution, ainsi qu'un stockage partagé des statuts et numéros livrés.
