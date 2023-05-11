# Ontop Challenge
Service to facilitate transfer from user ontop wallet to user bank account

[Read the
requirements here](./docs/ontop-challenge.pdf).

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

Once up and running you can use the [postman collection](./docs/postman_collection.json) to import use case
examples.

### Local Environment variables

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

CLIENT_HOST=http://mockoon.tools.getontop.com:3000

```
The database is an in-memory H2 database with bootstrap data -> `./src/main/resources/data.sql`\
While the app is running, the H2 database console can be accessed at -> `http://localhost:8080/ontop/h2-console`
```bash
# h2 console connection parameters
url: jdbc:h2:mem:ontop
username: sa
password: password
```

## Solution Design
The solution design can be found [here](/docs/solution-design.md)

### Areas for code Improvements
- add integrations tests
- more unit tests
