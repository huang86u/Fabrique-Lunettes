# Fabrique de Lunettes 

Ce dépôt contient le code source et les livrables du projet "Fabrique de Lunettes" réalisé dans le cadre du M1 MIAGE à l'IDMC.

Ce document explique uniquement comment récupérer et lancer les exécutables (`.jar`) prêts à l'emploi de notre application. 
*Pour l'architecture détaillée, le fonctionnement du protocole MQTT et nos choix de conception, veuillez vous référer à notre rapport PDF.*

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
