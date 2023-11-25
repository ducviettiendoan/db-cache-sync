# db-cache-sync (to be updated)

A sample service dealing with various scenarios where cache and database synchronization is essential in real-time using background processes.

## Getting Started

To test the cache design in Service B (sb):

1. **Set up DB:**
   
2. **Set up Kafka with Docker:**
   
3. **Set up cache:**
   
4. **Set up .env:**
    - In both /sa and /sb directories, create a .env file containing your Postgres username/password.
    - Format: `DB_USERNAME=<your db username>` and `DB_PASSWORD=<your db password>`.

5. **Run the app:**
    - `cd sb`
    - `mvn package`
    - `mvn spring-boot:run`

## Description and Motivation

### 1. Single Service: Lazy load + write through = significantly improving read operations

![Lazy Load + Write Through](url_to_image1)

### 2. Multiple Services: 

Consider a microservice architecture with two separated services (A and B) sharing the same DB. A common challenge arises when designing systems: How can service A's cache sync up with the shared DB if service B modifies it? Direct modification of service A's cache by service B is not ideal due to the key principles of microservice architecture. One solution is to use a separate service to establish communication when the database undergoes any modification. Kafka is a suitable tool for real-time data synchronization.

![Microservices Architecture](url_to_image2)

### 3. Improve availability and scalability
By  
### 4. Jenkins Pipeline Daily Automatic Sync (Additional)

In addition to real-time sync-up for cache and database, a daily automated sync-up job ensures that the cache stays synchronized with the main data storage, even with failed Kafka messages. Prerequisites for setting up and running the automated job:

- Install Jenkins on your local machine.
- Ensure Docker engine (or Docker Desktop) is installed.
- In the terminal or PowerShell, navigate to the `workspace` directory where Jenkins is installed (e.g., `~/.jenkins/workspace`). Clone the project: `git clone https://github.com/ducviettiendoan/db-cache-sync.git`. The main directory needed is `/jenkins-job/`.

- Open Jenkins (`localhost:8080`), go to `Manage Jenkins` > `Plugins` to install the Docker & Docker Compose Build Step Plugin.

- Create a new pipeline in the Jenkins dashboard. Use the following script, adjusting the `dir()` to the Jenkins workspace directory you cloned:

```Groovy
pipeline {
    agent any
    stages {
        stage('Run Docker Compose') {
            steps {
                dir('path_to_jenkins_jobs_folder_in_workspace') {
                    // Run docker-compose up
                    sh 'docker-compose up --build'
                }
            }
        }
    }
}

- Save your Jenkins job and ready to build the job. You could either manually run the job or set up the job to run periodically (For example in build periodically section, `H H * * *` should run the job everyday at a random time. Refer to this docs for more information: https://www.cloudbees.com/blog/how-to-schedule-a-jenkins-job). 
- Before running jenkins-job, make sure that sb is running in your Docker. You can build the Jenkins job now.
## Service sa

## Service sb

