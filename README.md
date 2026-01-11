# Microservices Application - Bibliothèque

## Description
Cette application est une architecture de microservices pour un système de bibliothèque utilisant Spring Boot, Eureka pour la découverte de services, Spring Cloud Gateway pour le routage, Kafka pour la communication asynchrone, et MySQL pour les bases de données.

## Architecture
```
                    GESTION D’EMPRUNTS

                         Client
                           |
                     Gateway Service
                           |
                       Eureka Server
                           |
           -----------------------------------------
           |                 |                     |
      User Service       Book Service        Emprunt Service
           |                 |                     |
         MySQL              MySQL                MySQL
        db_user            db_book           db_emprunter


                     Kafka (Communication Asynchrone)

                 Emprunt Service
                       |
                       v
             Topic : emprunt-created
                       |
                       v
              Notification Service
                 (log / console)
```

- **Eureka Server** : Service de découverte (port 8761)
- **API Gateway** : Routage des requêtes (port 9999)
- **User Service** : Gestion des utilisateurs (port 8082)
- **Book Service** : Gestion des livres (port 8081)
- **Emprunt Service** : Gestion des emprunts (port 8085)
- **Notification Service** : Notifications via Kafka (port 8086)
- **Bases de données** : MySQL (db_user, db_book, db_emprunter)
- **Message Broker** : Kafka avec Zookeeper

## Prérequis
- Docker et Docker Compose
- Java 21+
- Maven

## Installation et Lancement
1. Clonez le repository.
2. Naviguez vers le dossier racine.
3. Lancez les services :
   ```
   docker-compose up -d
   ```
4. Vérifiez que tous les conteneurs sont démarrés :
   ```
   docker-compose ps
   ```

## Configuration Docker Compose

Voici le fichier `docker-compose.yaml` complet avec des commentaires explicatifs :

```yaml
version: "2.4"  # Version de Docker Compose

services:

  # Service Zookeeper pour Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"  # Port exposé pour Zookeeper
    networks:
      - biblio-network

  # Service Kafka pour la communication asynchrone
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: kafka
    depends_on:
      - zookeeper  # Dépend de Zookeeper
    ports:
      - "9092:9092"  # Port interne Kafka
      - "29092:29092"  # Port externe pour les clients
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - biblio-network

  # Base de données MySQL pour le service User
  db_user:
    image: mysql:8.0
    container_name: db_user
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: db_user
    ports:
      - "3307:3306"  # Port exposé: 3307 (externe) -> 3306 (interne)
    volumes:
      - db_user_data:/var/lib/mysql  # Volume pour persister les données
    networks:
      - biblio-network

  # Base de données MySQL pour le service Book
  db_book:
    image: mysql:8.0
    container_name: db_book
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: db_book
    ports:
      - "3308:3306"
    volumes:
      - db_book_data:/var/lib/mysql
    networks:
      - biblio-network

  # Base de données MySQL pour le service Emprunt
  db_emprunter:
    image: mysql:8.0
    container_name: db_emprunter
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: db_emprunter
    ports:
      - "3309:3306"
    volumes:
      - db_emprunter_data:/var/lib/mysql
    networks:
      - biblio-network

  # Service de découverte Eureka
  eureka-server:
    build: ./eurika  # Construit l'image depuis le dossier eurika
    container_name: eureka-service
    ports:
      - "8761:8761"
    environment:
      - SPRING_APPLICATION_NAME=eureka-service
    depends_on:
      - db_user
      - db_book
      - db_emprunter
      - kafka
    networks:
      - biblio-network

  # API Gateway pour le routage des requêtes
  gateway-service:
    build: ./gateway
    container_name: gateway-service
    ports:
      - "9999:8080"  # Port externe 9999 -> port interne 8080
    depends_on:
      - eureka-server
    environment:
      - SPRING_APPLICATION_NAME=gateway-service
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka
    networks:
      - biblio-network

  # Service de gestion des utilisateurs
  user-service:
    build: ./user
    container_name: user-service
    depends_on:
      - eureka-server
      - db_user
    environment:
      - SPRING_APPLICATION_NAME=user-service
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka
    ports:
      - "8082:8082"
    networks:
      - biblio-network

  # Service de gestion des livres
  book-service:
    build: ./book
    container_name: book-service
    depends_on:
      - eureka-server
      - db_book
    environment:
      - SPRING_APPLICATION_NAME=book-service
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka
    ports:
      - "8081:8081"
    networks:
      - biblio-network

  # Service de gestion des emprunts
  emprunt-service:
    build: ./emprunter
    container_name: emprunt-service
    depends_on:
      - eureka-server
      - db_emprunter
      - kafka
    environment:
      - SPRING_APPLICATION_NAME=emprunt-service
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka
    ports:
      - "8085:8085"
    networks:
      - biblio-network

  # Service de notifications (écoute Kafka)
  notification-service:
    build: ./notification
    container_name: notification-service
    depends_on:
      - eureka-server
      - kafka
    environment:
      - SPRING_APPLICATION_NAME=notification-service
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-service:8761/eureka
    ports:
      - "8086:8086"
    networks:
      - biblio-network

# Volumes pour persister les données des bases de données
volumes:
  db_user_data:
  db_book_data:
  db_emprunter_data:

# Réseau pour connecter tous les services
networks:
  biblio-network:
    driver: bridge
```

## Services et Routes

### Eureka Server
- **URL** : http://localhost:8761
- **Routes** :
  - GET / : Page d'accueil Eureka
  - GET /eureka/apps : Liste des services enregistrés

### API Gateway
- **URL** : http://localhost:9999
- **Routes** (via discovery locator) :
  - /user-service/** → User Service
  - /book-service/** → Book Service
  - /emprunt-service/** → Emprunt Service

### User Service
- **URL directe** : http://localhost:8082
- **Routes** :
  - GET /users : Liste des utilisateurs
  - POST /users : Créer un utilisateur (JSON: {"name":"string","email":"string"})
  - GET /users/{id} : Détails d'un utilisateur

### Book Service
- **URL directe** : http://localhost:8081
- **Routes** :
  - GET /books : Liste des livres
  - POST /books : Créer un livre (JSON: {"titre":"string"})
  - GET /books/{id} : Détails d'un livre

### Emprunt Service
- **URL directe** : http://localhost:8085
- **Routes** :
  - GET /emprunts : Liste des emprunts
  - POST /emprunts : Créer un emprunt (JSON: {"userId":number,"bookId":number})
  - GET /emprunts/{id} : Détails d'un emprunt

### Notification Service
- **Pas de routes REST** : Écoute les messages Kafka sur le topic "emprunt-created"
- **Logs** : `docker-compose logs notification-service`

## Exemples d'Utilisation

### Créer un utilisateur
<img width="1911" height="324" alt="image" src="https://github.com/user-attachments/assets/d3f14a37-6f9f-4acc-b729-fd42fd05d150" />


### Créer un livre
<img width="1887" height="302" alt="image" src="https://github.com/user-attachments/assets/405c1f5b-4b7b-4243-a273-d7fb4962d3c4" />


### Créer un emprunt
```
curl -X POST http://localhost:9999/emprunt-service/emprunts -H "Content-Type: application/json" -d '{"userId":1,"bookId":1}'
```

**Screenshot** :
```
HTTP/1.1 201 Created
Location: http://localhost:9999/emprunt-service/emprunts/1
```

### Vérifier les notifications
```
docker-compose logs notification-service
```

**Screenshot** :
```
notification-service | Notification reçue: Emprunt créé pour userId=1, bookId=1
```

### Lister les utilisateurs
<img width="1915" height="943" alt="image" src="https://github.com/user-attachments/assets/2d79deb6-8a4a-47b3-95d4-3e54601cac89" />


### Lister les livres
<img width="1917" height="931" alt="image" src="https://github.com/user-attachments/assets/38eea5d4-59ba-4c08-9ff7-a0552e1c0761" />


### Lister les emprunts
```
curl http://localhost:9999/emprunt-service/emprunts
```

**Screenshot** :
```
{
  "_embedded": {
    "emprunts": [
      {
        "userId": 1,
        "bookId": 1,
        "_links": {...}
      }
    ]
  },
  "_links": {...},
  "page": {...}
}
```

### Eureka Dashboard
<img width="1902" height="969" alt="image" src="https://github.com/user-attachments/assets/9b1bb220-5184-415f-b1f5-609750bcc8cc" />


## Technologies Utilisées
- Spring Boot 3.2.7
- Spring Cloud 2023.0.3
- Eureka Client/Server
- Spring Cloud Gateway
- Spring Kafka
- MySQL 8.0
- Docker
- Maven

## Dépannage
- Si un service ne démarre pas, vérifiez les logs : `docker-compose logs <service-name>`
- Pour rebuild un service : `docker-compose build <service-name>`
- Ports utilisés : 8761 (Eureka), 9999 (Gateway), 8081-8086 (Services)

## Contributeurs
- Yassine Darif | Mastre M2SI-INSEA

## Licence
MIT
