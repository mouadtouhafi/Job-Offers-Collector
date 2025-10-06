# Docker Compose Documentation

## Docker-compose.yml File

```yaml
version: '3.8'
services:
  app:
    build: .
    container_name: collection-app-container
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobsdb1
      SPRING_DATASOURCE_USERNAME: mouadthf
      SPRING_DATASOURCE_PASSWORD: MyPassword
    depends_on:
      - postgres

  postgres:
    image: postgres:16
    container_name: jobs-container
    restart: unless-stopped
    environment:
      POSTGRES_DB: jobsdb1
      POSTGRES_USER: mouadthf
      POSTGRES_PASSWORD: MyPassword
    ports:
      - "5433:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

---

## What is Docker Compose?

Docker Compose is a tool that lets you define and run multiple Docker containers at the same time using a single YAML file **(docker-compose.yml)**.

* Without Compose, you'd have to run each container manually with `docker run`, remembering all ports, environment variables, and dependencies.
* Compose automates and organizes this, so your app and database (or other services) start together with the right settings.

---

## Explanation of the Code in Steps

### File Configuration

#### `version: '3.8'`

This line specifies the version of the Docker Compose file format. Using version 3.8 ensures compatibility with modern Docker features, such as named volumes, service dependencies, and networking between containers. Think of it like telling Docker, "Use the latest rules for interpreting this file."

#### `services:`

The services section defines all the containers Docker Compose will manage. Each service represents a separate container. In your file, you have two services:

* **app** for your Spring Boot application.
* **postgres** for the PostgreSQL database.

Compose will automatically handle the networking, so these containers can communicate by name.

---

### App Service Configuration

```yaml
app:
  build: .
```

The **app** service builds a Docker image from your local Dockerfile (the `.` means "current folder"). This container will run your Spring Boot application. By defining this in Compose, you don't need to manually run **docker build** or **docker run** - Compose does it for you.

#### `container_name: collection-app-container`

This assigns a friendly name to the container (**collection-app-container**). Without it, Docker would generate a random name. Naming containers makes it easier to manage them later, like checking logs or stopping them.

#### `ports: - "8080:8080"`

This maps port 8080 inside the container to port 8080 on your computer, allowing you to access the app in your browser at ***http://localhost:8080***. It's like opening a door from your computer into the container.

#### Environment Variables

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobsdb1
  SPRING_DATASOURCE_USERNAME: mouadthf
  SPRING_DATASOURCE_PASSWORD: MyPassword
```

These lines set environment variables inside the container, telling Spring Boot how to connect to the PostgreSQL database. The hostname **postgres** refers to the database service defined below, so Docker Compose automatically links them.

#### `depends_on: - postgres`

This ensures that the **postgres** container starts before the app, preventing errors where the app tries to connect to a database that isn't ready yet.

---

### PostgreSQL Service Configuration

```yaml
postgres:
  image: postgres:16
```

This service uses the official ***PostgreSQL*** image, version 16, to run a database container. You don't need to install PostgreSQL yourself; the image already has it configured.

#### `container_name: jobs-container`

Assigns a friendly name to the database container, making it easy to manage.

#### `restart: unless-stopped`

This ensures the container restarts automatically if it crashes, unless you stop it manually. It keeps your database running reliably.

#### Database Environment Variables

```yaml
environment:
  POSTGRES_DB: jobsdb1
  POSTGRES_USER: mouadthf
  POSTGRES_PASSWORD: MyPassword
```

These environment variables initialize the database with a specific name, user, and password when the container is first created. It ensures your app has the correct credentials to connect.

#### `ports: - "5433:5432"`

This maps port **5432** inside the container (PostgreSQL's default port) to port **5433** on your computer. This allows you to access the database from tools on your computer, like pgAdmin or DBeaver.

#### `volumes: - pgdata:/var/lib/postgresql/data`

This attaches a persistent volume to the database so that your data is saved even if the container is removed or recreated. Without it, the database would be lost every time the container stops.

---

### Volume Definition

#### `volumes: pgdata:`

This defines a named volume called **pgdata** that the database uses to store its files. Docker manages the location on your machine, and the volume ensures your database persists independently of containers.

---

### In Summary

* The app service builds and runs your Spring Boot app.
* The postgres service runs a PostgreSQL database with persistent storage.
* `depends_on` ensures correct startup order, ports map container services to your computer, and environment variables configure your app and database.
* The volumes section keeps your database data safe.

---

## Difference Between Dockerfile and docker-compose.yml

Dockerfile and Docker Compose as two tools that work together but do different things.

### 1. Dockerfile

* A Dockerfile is a text file with instructions to build a Docker image.
* It defines:
  * The base image (e.g., Java, Python)
  * Any dependencies your app needs
  * What files to include in the image
  * The command to run when the container starts
* Running docker build reads the Dockerfile and produces an image, which is a standalone package that can run anywhere with Docker installed.

**Key point:** Dockerfile → builds one image.

### 2. Docker Compose

* Docker Compose is a tool to run and manage multiple containers together.
* It uses a YAML file (docker-compose.yml) to define:
  * Which services (containers) exist
  * Which images to use (can be built from a Dockerfile or pulled from Docker Hub)
  * Ports, environment variables, volumes, networks
  * Dependencies between containers (depends_on)
* Running docker-compose up will:
  * Build the app image (if the Dockerfile is defined in build:)
  * Start all containers specified in the file
  * Handle networking so containers can communicate by name

**Key point:** Docker Compose → runs multiple containers together and manages their configuration.

### 3. How They Relate

* Dockerfile creates the image for your application.
* Docker Compose uses that image (or builds it) and runs it with other containers like databases or caches.
* Docker Compose also sets environment variables, ports, volumes, and ensures the containers start in the right order.

**Summary in one line:**

* **Dockerfile** = "how to build one container image"
* **Docker Compose** = "how to run that container image with other containers, and configure everything together."

---

## When to Use Docker Compose

**1. When to use Docker Compose**

You use Docker Compose **when your app needs more than one container**.

In your case:
* One container for your **Spring Boot app** (built from Dockerfile).
* One container for **PostgreSQL database** (from Docker Hub).

Instead of starting each one manually with long `docker run ...` commands, you define both in a single file (`docker-compose.yml`) and start them together.

**2. How to run Docker Compose**

Make sure your `docker-compose.yml` is in the root of your project (same place as Dockerfile).

**Step A – Build and Start**

```bash
docker-compose up --build
```

* `--build` ensures it rebuilds the app image using your Dockerfile.
* This will start **both containers** (`app` + `postgres`) as defined.
* Logs of both containers will be shown in your terminal.

**Step B – Run in the background (detached mode)**

```bash
docker-compose up -d
```

* Runs the containers in the background.
* You get your terminal back, but the app keeps running.
* Good for real usage.

⚠️ So you don't need to run them one after the other.
* Use `--build` when you need to rebuild.
* Use `-d` when you just want to start.

---

## Note: Understanding the Complete Workflow

When we run `docker build -t collection-app .`, Docker reads our Dockerfile in the current directory and creates an image for our application. At this stage, only our app image exists locally; PostgreSQL is not touched because `docker build .` only works on the Dockerfile we specify. This is why, if we check our Docker images or containers after this command, we will only see our app image and nothing for PostgreSQL.

To run both our application and PostgreSQL together, we should use Docker Compose with the command `docker-compose up --build`. This command does several things automatically. First, it reads our `docker-compose.yml` file and builds the app image using the Dockerfile (because of `build: .`). Second, it pulls the official PostgreSQL image (`postgres:16`) if it is not already present locally. Third, it starts two containers: one for our Spring Boot application (`collection-app-container`) and one for PostgreSQL (`jobs-container`). Because Docker Compose automatically puts the containers on the same network, our app can connect to PostgreSQL using the service name `postgres`.

Our Dockerfile is not ignored in this process. Docker Compose fully uses it to build the image for the app service. The PostgreSQL container, on the other hand, is created from the official image and does not require a Dockerfile. Therefore, when we run `docker-compose up --build`, it builds our app image from the Dockerfile, pulls the PostgreSQL image, and starts both containers, allowing our Spring Boot application to connect to the database. The port mapping in our YAML (like `5433:5432`) only affects access from our host machine; inside Docker, the app always connects to the internal PostgreSQL port `5432`.