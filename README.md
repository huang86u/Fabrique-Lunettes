# **La Fabrique de Lunettes**

Bienvenue dans le projet de la ùùFabrique de Lunettes ! 
Ce projet implémente un système distribué permettant de commander des lunettes connectées, de les faire fabriquer par une usine, et de suivre leur expédition grâce à une architecture orientée événements (EDA).

## Architecture du projet

Le projet est divisé en 3 modules Maven :
* `lunettes-shared` : Contient les modèles de données (`Commande`, `TypeLunette`) et le protocole de sérialisation des messages "maison".
* `lunettes-backend` : Le serveur/usine qui écoute les commandes MQTT, valide les requêtes et pilote la machine de fabrication.
* `lunettes-javafx` : L'interface graphique cliente permettant de naviguer dans le catalogue et de passer commande.

## Prérequis

Pour exécuter ce projet sur votre machine, vous devez installer :
1. **Java Development Kit (JDK) 21**
2. **Apache Maven**
3. **Eclipse Mosquitto** (Broker MQTT) : [Télécharger ici](https://mosquitto.org/download/)


## Configuration

Ce projet dépend d'une librairie privée hébergée sur GitHub (`fabricateur`). Pour que Maven puisse la télécharger, vous devez configurer vos identifiants.

1. Allez dans votre dossier utilisateur (`C:\Users\VotreNom\.m2\`sur Windows).
2. Créez ou modifiez le fichier `settings.xml`.
3. Ajoutez-y la configuration suivante avec les accès fournis par le professeur :

```xml
<settings xmlns="[http://maven.apache.org/SETTINGS/1.0.0](http://maven.apache.org/SETTINGS/1.0.0)"
          xmlns:xsi="[http://www.w3.org/2001/XMLSchema-instance](http://www.w3.org/2001/XMLSchema-instance)"
          xsi:schemaLocation="[http://maven.apache.org/SETTINGS/1.0.0](http://maven.apache.org/SETTINGS/1.0.0)
                              [http://maven.apache.org/xsd/settings-1.0.0.xsd](http://maven.apache.org/xsd/settings-1.0.0.xsd)">
    <servers>
        <server>
            <id>github-le-prof-de-raizo</id>
            <username>le-prof-de-raizo</username>
            <password>VOTRE_TOKEN_GITHUB_ICI</password>
        </server>
    </servers>
</settings>