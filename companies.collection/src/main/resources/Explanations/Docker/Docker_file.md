# Dockerfile Documentation

## Multi-Stage Docker Build

```dockerfile
# Stage 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /collection-app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /collection-app
COPY --from=build /collection-app/target/companies.collection-0.0.1-SNAPSHOT.jar collection-app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "collection-app.jar"]
```

---

## Stages Explanation

**Stage 1: Build (Build Stage)**

* **Purpose:** This stage is all about **compiling your code** and creating the `.jar` file.
* **What it has:** A **full development environment** — Maven, Java JDK, and all build tools.
* **What happens:** Docker copies your project into this stage, runs Maven, and produces the compiled `.jar` in the `target` folder.
* **Result:** You get a `.jar` file, but this image is **heavy** because it includes all the tools needed for building, which you don't need to run the app.

**Stage 2: Run (Runtime Stage)**

* **Purpose:** This stage is for **running the application**, not building it.
* **What it has:** Only the **Java Runtime Environment (JRE)** — much smaller and lightweight.
* **What happens:** Docker copies the `.jar` from the first stage into this stage and sets up the container to run it.
* **Result:** A small, clean container that contains **only what's necessary to run the app**, which is better for performance, security, and deployment.

**Think of it like:** **Stage 1 is your kitchen where you cook**, Stage 2 is the **plate you serve to eat**. You don't bring the whole kitchen to the table, just the meal. 🍽️

---

## Explanation of the Code in Steps

### Stage 1: Build Environment

#### `FROM maven:3.9.8-eclipse-temurin-21 AS build`

This line tells Docker to start from an existing image that already has Maven (version 3.9.8) and Java 21 installed. Maven is the tool that will compile your project and create the runnable .jar file. The AS build part gives this stage a name—build—so we can refer to it later when copying files. Think of it as saying: "Set up a temporary workspace for building my project."

#### `WORKDIR /collection-app`

This sets the working directory inside the container to /collection-app. Any commands that follow, like copying files or running Maven, will happen inside this folder. It's like moving into a folder in your computer before starting work.

#### `COPY . .`

This copies all the files from your current project folder on your computer into the container's /collection-app folder. The first . refers to your computer's folder, and the second . refers to the container folder. Essentially, it's "bring my code into the container."

#### `RUN mvn clean package -DskipTests`

This runs a Maven command to build your Java project. mvn clean package compiles the code, resolves dependencies, and creates a .jar file in the target folder. -DskipTests skips running tests so the build happens faster. By the end of this step, your application is compiled and ready to run.

---

### Stage 2: Runtime Environment

#### `FROM eclipse-temurin:21-jre-alpine`

Now we start a new image, but this time it only has the Java Runtime Environment (JRE), not Maven. The JRE is enough to run your app, and this image is smaller, making your final Docker container lighter. Using a smaller image is important for performance and storage.

#### `WORKDIR /collection-app`

Again, we set a working directory inside this new image. This is where we will place the compiled .jar file and where the application will run.

#### `COPY --from=build /collection-app/target/companies.collection-0.0.1-SNAPSHOT.jar collection-app.jar`

Here we copy the .jar file we built in the first stage (build) into this new image. --from=build tells Docker to look in the previous stage named build. We also rename the file to collection-app.jar for simplicity. This separates the build environment from the run environment, which is a best practice.

#### `EXPOSE 8080`

This tells Docker that the container will use port 8080. Your Spring Boot app listens on this port. This is mostly for documentation and for Docker networking, letting other containers or the host know which port the app uses.

#### `ENTRYPOINT ["java", "-jar", "collection-app.jar"]`

This is the command that runs when the container starts. It tells Docker to execute java -jar collection-app.jar, which launches your Spring Boot application. Think of it as the "start button" for your app inside the container.

---

## Steps to Run This Dockerfile

### Step 1: Build the Docker Image

Use the `docker build` command to create an image from your Dockerfile:

```bash
docker build -t collection-app .
```

**Explanation:**
* `-t collection-app` → gives your image a name (`collection-app` in this case).
* `.` → means "look for the Dockerfile in the current folder."

After this command finishes, Docker will have **built your image** using both stages from the Dockerfile.

### Step 2: Run the Docker Container

```bash
docker run -d --name my-collection-app -p 8080:8080 collection-app
```

**Explanation:**
* `--name my-collection-app` → gives your container the name my-collection-app.
* `-p 8080:8080` → maps port **8080** of your container to port **8080** on your computer, so you can open `http://localhost:8080` in your browser.
* `collection-app` → the name of the image you just built.
* `-d` → runs it in **detached mode**, meaning the terminal won't be blocked.

After running this, your Spring Boot app should start inside the container.