# Running Distributed Job Scheduler Locally

This guide walks you through setting up and running the **Distributed Job Scheduler** on your local machine.

---

# Prerequisites

Ensure you have the following software installed before starting.

| Software | Required Version |
|----------|------------------|
| Java | 21+ |
| Maven | 3.9+ |
| Docker | Latest |
| Docker Compose | Latest |
| Git | Latest |

Verify your installation:

```bash
java -version
mvn -version
docker --version
docker compose version
git --version
```

---

# Clone the Repository

```bash
git clone https://github.com/YashwantSaste/Distributed-Job-Scheduler.git

cd Distributed-Job-Scheduler
```

---

# Start Infrastructure Services

The project uses Docker Compose to provision the required infrastructure, including:

- PostgreSQL
- Redis
- Apache Kafka
- Zookeeper

The Docker Compose configuration is located inside the `docker` directory.

Navigate to the directory:

```bash
cd docker
```

Start the containers:

```bash
docker compose up -d
```

Verify that all containers are running:

```bash
docker ps
```

You should see containers for:

- PostgreSQL
- Redis
- Kafka
- Zookeeper

Return to the project root:

```bash
cd ..
```

---

# Build the Project

From the project root, build all modules.

```bash
mvn clean install
```

To skip unit tests during the build:

```bash
mvn clean install -DskipTests
```

A successful build compiles all project modules and installs them into your local Maven repository.

---

# Project Modules

The project is organized as a multi-module Maven application.

```
Distributed-Job-Scheduler
│
├── api-server
├── common
├── consumer-service
├── database
├── docker
├── executor-service
├── job-service
├── kafka
├── redis
├── scheduler-core
├── search-service
└── watcher-service
```

---

# Running the Services

Start each service in a separate terminal from the project root.

## API Server

```bash
cd api-server

mvn spring-boot:run
```

---

## Job Service

```bash
cd job-service

mvn spring-boot:run
```

---

## Search Service

```bash
cd search-service

mvn spring-boot:run
```

---

## Watcher Service

```bash
cd watcher-service

mvn spring-boot:run
```

---

## Consumer Service

```bash
cd consumer-service

mvn spring-boot:run
```

---

## Executor Service

```bash
cd executor-service

mvn spring-boot:run
```

---

# Verify the Setup

After starting all services, ensure:

- PostgreSQL is connected.
- Redis is connected.
- Kafka is connected.
- All Spring Boot applications start successfully.
- No service reports startup errors.

You can also verify that Kafka consumers have subscribed successfully by checking the application logs.

---

# Access the API

Once the API Server is running, access the application at:

```
http://localhost:8080
```

If Swagger/OpenAPI is enabled, open:

```
http://localhost:8080/swagger-ui/index.html
```

or

```
http://localhost:8080/swagger-ui.html
```

---

# Testing Job Scheduling

Create a new job using the REST API.

The request flows through the system as follows:

```
Client
   │
   ▼
API Server
   │
   ▼
Job Service
   │
   ▼
PostgreSQL
   │
   ▼
Watcher Service
   │
   ▼
Redis Distributed Lock
   │
   ▼
Kafka
   │
   ▼
Consumer Service
   │
   ▼
Executor Service
   │
   ▼
Job Execution
```

Monitor the logs of the Watcher, Consumer, and Executor services to observe the complete execution flow.

---

# Stopping the Application

Stop each service using:

```
Ctrl + C
```

To stop the infrastructure:

```bash
cd docker

docker compose down
```

To remove associated volumes:

```bash
docker compose down -v
```

---

# Cleaning the Project

Remove all generated build artifacts:

```bash
mvn clean
```

To completely reset the infrastructure:

```bash
cd docker

docker compose down -v
```

---

# Troubleshooting

## Kafka Connection Issues

Ensure Kafka and Zookeeper containers are running.

```bash
docker ps
```

Restart the infrastructure if necessary:

```bash
cd docker

docker compose restart
```

---

## PostgreSQL Connection Failed

Verify that the PostgreSQL container is running.

```bash
docker ps
```

Also ensure the configured database credentials match those in the application configuration.

---

## Redis Connection Failed

Verify the Redis container is running.

```bash
docker ps
```

---

## Docker Is Not Running

Start Docker Desktop (Windows/macOS) or the Docker daemon (Linux) before starting the containers.

Verify:

```bash
docker info
```

---

## Port Already in Use

Linux/macOS:

```bash
lsof -i :8080
```

Windows:

```cmd
netstat -ano | findstr :8080
```

Stop the conflicting process or change the application's port configuration.

---

## Maven Build Failure

Clean the project and rebuild.

```bash
mvn clean install
```

If dependencies fail to download, ensure you have an active internet connection and Maven Central is reachable.

---

# Shutting Everything Down

To completely stop the project:

Stop all running services.

Then execute:

```bash
cd docker

docker compose down -v
```

Finally, clean the build:

```bash
mvn clean
```

Your local environment is now reset and ready for the next run.
