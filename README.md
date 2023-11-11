# db-cache-sync (to be updated)
A sample service deals with different cases when cache and database need to sync up in real time using background processes

## Getting started
Test cache design in 1 service (sb):
1. Set up DB:
2. Set up Kafka with Docker:
3. Set up cache:
4. Set up .env:
    - In both /sa, /sb directory, create .env file contain your Postgres username/password. 
    - Format: `DB_USERNAME=<your db username>` and `DB_PASSWORD=<your db password>`.
5. Run app:
    - `cd sb`
    - `mvn package`
    - `mvn spring-boot:run`


## Description and motivation

## Service sa

## Service sb

