# db-cache-sync 

A sample service dealing with various scenarios where cache and database synchronization is essential in real-time using background processes.
## Description
This project has 3 services in total. For now, sb and sa services only support GET and POST methods. 
### Service sb
This is the central service which has its own cache, and the goal is to sync sb's cache with the PostgresDB. Since it's the central service (meaning other services such as sa could  depend on it), we add replications to sb. The default number of replications is 2, but modifications can be made in the /sb/k8s.yml file. 
### Service sa
This service is used to interrupt the sync state of sb's cache by interacting directly to the PostgresDB. To keep the sync state for sb (real-time) we use a Kafka background process to publish messages and let sb receive a message with the updated information.
### Service jenkins-job
A Jenkins pipeline uses this service to automatically check for any out-of-sync data between the main data source and sb's cache. This process could be scheduled at a specific time.

## Getting Started
1. **Pre-installation:**
    - Install minikube: https://minikube.sigs.k8s.io/docs/start/
    - Install kubectl: https://kubernetes.io/docs/tasks/tools/
    - Install Docker: https://docs.docker.com/engine/install/ 
    **NOTE**: To run the entire project (including sa, sb, and jenkins) minikube containers always have to run 

2. **Set up minikube:**
    - Once minikube and Docker are installed, start the minikube container with `minikube start --memory 4096 --cpus 2`. You can change the number of resources, but 4Gb memory and 2 CPU cores are the least required.
    - Tunnel: Open a new terminal and type `minikube tunnel`. This helps expose the external IP for the created service in k8s to set the hostname to `localhost` and expose the service's port.
    - Monitoring cluster pods: If you are using Vscode, install kubernetes extension. Once installed, open the extension and under "Cluster" create a new cluster. If minikube is installed, there should be an option to create new Cluster with "Minikube local cluster". Restart and reopen Vscode then you should see minikube dropdown in the "Cluster" section. 


5. **Set up K8s Config Map:**
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

6. **Run the app:**
    - Make sure Docker engine is running
    - Apply configMap to get secrets: `kubectl apply -f env.yml`
    - Run resources service: `kubectl apply -f resource.yml`
    - Run sb service: `cd sb/` then `kubectl apply -f k8s.yml`
    - Run sa service: `cd sa/` then `kubectl apply -f k8s.yml`
    - Run jenkins service: `cd jenkins/` then `kubectl apply -f k8s.yml`

## Description and Motivation

### 1. Single Service: Lazy load + write-through = significantly improving read operations

![Lazy Load + Write Through](/images/single-service-diagram.png)

### 2. Multiple Services: 

Consider a microservice architecture with two separate services (A and B) sharing the same DB. A common challenge arises when designing systems: How can service A's cache sync up with the shared DB if service B modifies it? Direct modification of service A's cache by service B is not ideal due to the key principle of microservice architecture is decoupling. One solution is to use a separate service to establish communication when the database undergoes any modification. Hence, Kafka is a suitable tool for real-time data synchronization.

![Microservices Architecture](/images/multiple-service-diagram.png)

### 3. Improve availability and scalability
By tracing and monitoring sb service when it interacts with high amount of requests from external service (sa), one sb server could surely be overloaded. Hence, to optimize the workflow for this database cache sync simulation, we want to manage multiple services/docker containers with Kubernetes using Minikube open source. Using the LoadBalancer service to manage workloads of sb server replicas allows better availablity and scalability, helping the Redis cache service sync up with Postgres better in real-time. All of the used resources are configured in **resource.yml**. Inside each service folder, there is also a **k8s.yml** file to create the server service (with provisioned resource) in the same K8s cluster. 

**WRITE THROUGH CACHE DESIGN**
![Write Through cache design](/images/wt.png)

**MULITPLE SERVICE CACHE SYNC DESIGN**

![Multiple service design](/images/multi-serv.png)

**JENKINS SERVICE DESIGN**

![Jenkins scheduled syncing job](/images/jenkins.png)

### 4. Jenkins Pipeline Scheduled Automatic Sync (Additional)

In addition to real-time sync-up for cache and database, a daily automated sync-up job ensures that the cache stays synchronized with the main data storage, even with failed Kafka messages. Prerequisites for setting up and running the automated job:

- Install Jenkins on your local machine.
- Ensure Docker engine (or Docker Desktop) is installed.
- In the terminal or PowerShell, navigate to the `workspace` directory where Jenkins is installed (e.g., `~/.jenkins/workspace`). Clone the project: `git clone https://github.com/ducviettiendoan/db-cache-sync.git`. The main directory needed is `/jenkins-job/`. 
**Note:** Make sure Jenkins is running on port 8080.

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
```
- Save your Jenkins job and be ready to build the job. You could either manually run the job or set up the job to run periodically (For example, in the build periodically section, `H H * * *` should run the job every day at a random time. Refer to this docs for more information: https://www.cloudbees.com/blog/how-to-schedule-a-jenkins-job). 
- Before running jenkins-job, make sure that sb is running in your Docker. You can build the Jenkins job now.

### 5. Additional util file
To save some time for testing, there is a `.py` file to send multiple requests to either SA or SB you could play around with. 
