Projet Hackathon : API de Détection et Réponse aux Incidents (ADRI API)
Introduction
Ce dépôt contient le backend de notre solution de hackathon, l'API de Détection et Réponse aux Incidents (ADRI API). Ce service est conçu pour surveiller les logs d'une application cible, détecter les comportements anormaux basés sur des règles prédéfinies, alerter les équipes de sécurité via AWS SNS et implémenter des réponses automatisées (simulées) comme le blocage d'adresses IP malveillantes.

Ce projet a été développé dans le cadre d'un hackathon de 3 jours, avec un focus sur la création d'un Produit Minimum Viable (MVP) fonctionnel et démontrable.

Fonctionnalités Clés
Génération de logs d'activité : L'application cible simule des interactions utilisateur (connexions, accès à des ressources) et génère des logs détaillés.

Parsing des logs : Analyse les lignes de logs pour extraire les informations pertinentes (timestamp, IP source, type d'événement, etc.).

Détection d'anomalies : Identifie les comportements suspects (ex: tentatives de force brute, scans de vulnérabilités) via des règles simples.

Stockage des anomalies : Enregistre les détails des menaces détectées dans une base de données H2 intégrée.

Alertes de sécurité : Envoie des notifications en temps réel via AWS SNS dès qu'une anomalie est identifiée.

Gestion des IPs bloquées : Maintient une liste noire d'adresses IP qui sont automatiquement bloquées après la détection d'activités malveillantes.

API REST : Expose des endpoints pour récupérer les anomalies détectées et gérer la liste des IPs bloquées, permettant l'intégration avec un frontend de visualisation.

Technologies Utilisées
Langage : Java 17

Framework : Spring Boot 3.5.3

Persistance : Spring Data JPA, Hibernate

Base de Données : H2 Database (embarquée)

Services Cloud : AWS SNS (Simple Notification Service)

Outils de Build : Maven

Dépendances Utilitaires : Lombok

Démarrage Rapide
Prérequis
Assurez-vous d'avoir les éléments suivants installés sur votre machine :

Java Development Kit (JDK) 17 ou plus récent.

Maven (ou Gradle si vous avez choisi cette option lors de la création du projet).

Visual Studio Code avec les extensions "Extension Pack for Java" et "Spring Boot Extension Pack".

AWS CLI configuré avec des identifiants ayant les permissions nécessaires pour publier sur SNS.

Créez un utilisateur IAM avec la politique sns:Publish sur votre topic SNS. Pour un hackathon, AmazonSNSFullAccess peut être utilisé temporairement.

Configurez AWS CLI : aws configure (entrez vos Access Key ID, Secret Access Key et région eu-north-1).

Un topic AWS SNS nommé microservices-topic (ou un nom de votre choix) dans la région eu-north-1. Récupérez son ARN complet.

Configuration du Projet
Cloner le dépôt :

git clone <URL_DE_VOTRE_DEPOT_GITHUB>
cd application-cible # Naviguez vers le répertoire racine de votre projet backend

Mettre à jour src/main/resources/application.properties :
Ouvrez ce fichier et assurez-vous que les configurations AWS SNS sont correctes.

# Configuration du port de l'application cible
server.port=8080

# Configuration des logs pour écrire dans un fichier
logging.file.name=app-cible.log
logging.level.com.hackathon.applicationcible=INFO
logging.level.org.springframework.web=INFO

# Configuration H2 Database
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.datasource.url=jdbc:h2:file:./data/anomalies-db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# Configuration AWS SNS
aws.region=eu-north-1 # Assurez-vous que c'est votre région
aws.sns.topic-arn=arn:aws:sns:eu-north-1:YOUR_AWS_ACCOUNT_ID:microservices-topic # REMPLACEZ PAR VOTRE ARN RÉEL

N'oubliez pas de remplacer YOUR_AWS_ACCOUNT_ID par votre ID de compte AWS réel !

Lancer l'Application
Depuis le répertoire racine du projet (application-cible), exécutez :

./mvnw spring-boot:run

L'application démarrera sur http://localhost:8080.

Utilisation et Démonstration
1. Générer des Logs d'Activité (Application Cible)
Utilisez un navigateur ou un outil comme Postman/Insomnia pour envoyer des requêtes aux endpoints suivants. Cela générera des logs dans le fichier app-cible.log.

Connexion réussie (simulée) :

POST vers http://localhost:8080/api/login?username=user&password=password

Connexion échouée (simulée - pour la force brute) :

POST vers http://localhost:8080/api/login?username=wrong&password=wrong (Répétez 5+ fois en moins d'une minute pour déclencher l'anomalie "Force Brute")

Accès public :

GET vers http://localhost:8080/api/public

Accès secret (tentative non autorisée - pour le scan de vulnérabilité) :

GET vers http://localhost:8080/api/secret

2. Déclencher la Détection d'Anomalies
Après avoir généré des logs (surtout des logs d'échec de connexion ou d'accès secret), déclenchez le moteur de détection :

POST vers http://localhost:8080/api/anomalies/detect (corps vide)

Regardez les logs de votre application Spring Boot dans le terminal ; vous devriez voir des messages ANOMALIE DÉTECTÉE.

3. Visualiser les Anomalies Détectées
Récupérez la liste des anomalies stockées dans la base de données H2 :

GET vers http://localhost:8080/api/anomalies

Vous pouvez aussi accéder à la console H2 pour voir les données brutes : http://localhost:8080/h2-console.

URL JDBC : jdbc:h2:file:./data/anomalies-db

User Name : sa

Password : (laisser vide)
Exécutez SELECT * FROM ANOMALY; pour voir les enregistrements.

4. Vérifier les Alertes AWS SNS
Si des anomalies ont été détectées, des messages auront été publiés sur votre topic AWS SNS :

Allez sur la console AWS, service SNS.

Cliquez sur votre topic (microservices-topic).

Dans la section "Messages published", vous verrez l'historique.

Si vous avez une file SQS abonnée, allez sur la console SQS, cliquez sur votre file, puis "Poll for messages" pour voir les alertes.

5. Tester le Blocage d'IP (Réponse Automatisée Simulée)
Si une anomalie de "Force Brute" ou "Scan de Vulnérabilité" a été détectée pour une IP, cette IP devrait être "bloquée" par l'application :

Après avoir déclenché une anomalie pour votre IP (ex: en faisant 5+ échecs de login), essayez une nouvelle requête vers http://localhost:8080/api/public depuis la même IP.

Vous devriez recevoir une réponse 403 Forbidden avec le message "Votre adresse IP est bloquée."

Pour voir la liste des IPs bloquées :

GET vers http://localhost:8080/api/blocked-ips

Pour débloquer une IP (pour les tests) :

DELETE vers http://localhost:8080/api/blocked-ips/YOUR_IP_ADDRESS (remplacez YOUR_IP_ADDRESS par l'IP à débloquer, ex: 127.0.0.1)

Perspectives d'Amélioration
Pour une solution plus robuste, les améliorations futures pourraient inclure :

Détection d'autres types d'attaques : Injection SQL, Cross-Site Scripting (XSS), Broken Access Control, Path Traversal.

Persistance du blocage d'IP : Stocker les IPs bloquées dans la base de données H2 (ou une autre DB) pour qu'elles persistent après le redémarrage de l'application.

Intégration CloudWatch Logs : Utiliser AWS CloudWatch Logs comme source de logs pour une solution plus scalable.

Tableau de bord Frontend Angular : Développer l'interface utilisateur Angular pour une visualisation interactive et une gestion des incidents.

Règles de détection plus avancées : Utilisation de machine learning pour des détections d'anomalies plus complexes.

Actions de réponse réelles : Intégration avec des pare-feu ou des outils d'orchestration pour des actions de réponse non simulées.

Authentification et Autorisation : Mettre en place une authentification et une autorisation robustes pour l'API de détection elle-même.
