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
1. Single Service: Lazy load + write through = significantly improve read operation 

--Image--

2. Multiple Services: Image a microservice architecture with 2 seperated services (A and B) shared the same DB (a common pattern). A common problem many firms actually dealing with when designing their systems is how can service A's cache sync up with the shared DB if service B modifies it. Now, because an important point of a microservice architecture is to decouple services, reduce dependence complexity, and even application response time, letting service B directly modifies service A's cache for all modifications made is not an ideal solution. There are many solutions for this problem and one of them is to use a seperated service to establish communication when database has any modification. Hence, Kafka is again a good tool to allow data synchronization in real-time.

--Image--
## Service sa

## Service sb

