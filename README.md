# OnTop Challenge
Service to facilitate transfer from user ontop wallet to user bank account

[Read the
requirements here](./docs/specification.pdf).

## Setting up
```bash
Java 17
Docker
Redis #optional
Kafka #optional
```
⚠ The services marked optional are not required if one intends to run the app with docker

## Running

```bash
# Locally
$ startup redis and kafka server locally
$ ./mvnw spring-boot:run

# Docker
$./mvnw package
$ docker-compose up  # use -d to detach
```

Once up and running you can use the `postman_collection.json` to import use case
examples.

### Environment variables

```bash
# The environment variables have been set up with the defaults and should work as is. 
# Nevertheless you can change as required

SERVER_PORT=8080

KAFKA_BROKER_CONNECT=localhost:9092
H2_DATABASE_URL=mem:ontop

REDIS_HOST=localhost
REDIS_PORT=6379

ONTOP_ACCOUNT_NAME=ONTOP INC
ONTOP_ACCOUNT_NO=0245253419
ONTOP_ROUTING_NUMBER=028444018

PAYMENT_PROVIDER_HOST=http://mockoon.tools.getontop.com:3000
WALLET_CLIENT_HOST=http://mockoon.tools.getontop.com:3000

```
The database is an in-memory H2 database with bootstrap data -> `./src/main/resources/data.sql`