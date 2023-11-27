# db-cache-sync (to be updated)

A sample service dealing with various scenarios where cache and database synchronization is essential in real-time using background processes.

## Getting Started
1. **Pre-installation:**
    - Install minikube: https://minikube.sigs.k8s.io/docs/start/
    - Install kubectl: https://kubernetes.io/docs/tasks/tools/
    - Install Docker: https://docs.docker.com/engine/install/ 
    **NOTE**: To run the entire project (including sa, sb, and jenkins) minikube containers always have to run 

2. **Set up DB:**
   
3. **Set up Kafka with Docker:**
   
4. **Set up cache:**
   
5. **Set up .env:**
    - Install envsubst
    - In both /sa and /sb directories, create a .env file containing your Postgres username/password.
    - Format: `export DB_USERNAME=<your db username>` and `export DB_PASSWORD=<your db password>`.
    - `source .env` to export your env variable from envsubst

6. **Run the app:**
    - Create a configMap file for k8s secrtes in the root directory `env.yml` as follow
    ```YAML
    apiVersion: v1
    kind: ConfigMap
    metadata:
    name: env-config
    data:
    DB_USERNAME: <your Postgres username>
    DB_PASSWORD: <your Postgres password> #required
    ```
    - Run resources service: `kubectl apply -f resource.yml`
    - Run sb service: `cd sb` then `kubectl apply -f k8s.yml`
    - Run sa service: `cd sa` then `kubectl apply -f k8s.yml`
    - Run jenkins service: `cd jenkins` then `kubectl apply -f k8s.yml`

## Description and Motivation

### 1. Single Service: Lazy load + write through = significantly improving read operations

![Lazy Load + Write Through](url_to_image1)

### 2. Multiple Services: 

Consider a microservice architecture with two separated services (A and B) sharing the same DB. A common challenge arises when designing systems: How can service A's cache sync up with the shared DB if service B modifies it? Direct modification of service A's cache by service B is not ideal due to the key principles of microservice architecture. One solution is to use a separate service to establish communication when the database undergoes any modification. Kafka is a suitable tool for real-time data synchronization.

![Microservices Architecture](url_to_image2)

### 3. Improve availability and scalability
By tracing and monitoring sb service when interacts with high amount of requests from external service (sa), one sb server could surely be overloaded. Hence, it optimize the workflow for this database cache sync simulation we want manage multiple services/docker containers with Kubernetes using Minikube open source. Using the LoadBalancer service to produce sb server replicas allows better availablity and scalability helping the Redis cache service sync up with Postgres better in real-time. All of the used resources are configured in **resource.yml**. Inside each service folder, there is also a **k8s.yml** file to create the server service (with provisioned resource) in the same K8s cluster. 

**WRITE THROUGH CACHE DESIGN**
![Write Through cache design](/images/wt.png)

**MULITPLE SERVICE CACHE SYNC DESIGN**

![Multiple service design](/images/multi-serv.png)

**JENKINS SERVICE DESIGN**

![Jenkins scheduled syncing job](/images/jenkins.png)

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
        stage('Re-init resources and secrets') {
            steps{
                dir('path_to_workspace_folder') {
                    // Run docker-compose up
                    sh 'kubectl apply -f env.yml'
                    sh 'kubectl apply -f resource.yml'
                }
            }
        }
        stage('Run k8s jenkins-job'){
            steps {
                dir('path_to_jenkins_jobs_folder_in_workspace') {
                    // Run docker-compose up
                    sh 'kubectl apply -f k8s.yml'
                }
            }
        }
    }
}

- Save your Jenkins job and ready to build the job. You could either manually run the job or set up the job to run periodically (For example in build periodically section, `H H * * *` should run the job everyday at a random time. Refer to this docs for more information: https://www.cloudbees.com/blog/how-to-schedule-a-jenkins-job). 
- Before running jenkins-job, make sure that sb is running in your Docker. You can build the Jenkins job now.
## Service sa

## Service sb

