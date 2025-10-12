# Docker Compose File used to push the image to DockerHub

## Docker-compose.yml File

```yaml
services:
  app:
    image: mouadthf/collection-app:latest
    container_name: collection-app-container
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobsdb1
      SPRING_DATASOURCE_USERNAME: mouadthf
      SPRING_DATASOURCE_PASSWORD: mdthf97
      SELENIUM_URL: http://selenium:4444
    depends_on:
      - postgres
      - selenium

  postgres:
    image: postgres:16
    container_name: jobs-container
    environment:
      POSTGRES_DB: jobsdb1
      POSTGRES_USER: mouadthf
      POSTGRES_PASSWORD: mdthf97
    ports:
      - "5433:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  selenium:
    image: selenium/standalone-edge:latest
    container_name: selenium
    ports:
      - "4444:4444"

volumes:
  pgdata:
```

---

## How to push it?
### Build the app locally

First, we need to build and run the app locally using the command `docker-compose up --build` and using the line `build: .` instead of `image: mouadthf/collection-app:latest`. 

Once the app finihed running we stop the container using `docker-compose down
`.

The difference between `docker-compose up --build` and `docker-compose build` : 
* The first, builds the images and then starts (runs) the containers.
* The second one, only builds (or rebuilds) the images defined in our docker-compose.yml, based on the Dockerfile and any build context specified. To run the container we use `docker-compose up
`.

### Building Your Docker Image

Next, we create our own standalone image for the app using `docker build -t mouadthf/collection-app:latest .`

This command reads the Dockerfile, installs our app, and wraps it inside an image that Docker can run anywhere. The `-t` flag adds a name (`mouadthf/collection-app:latest`) so Docker knows what to call it. 

**Note:** This command doesn't take into account the docker-compose file—it builds the app only from the Dockerfile, so the services listed in the docker-compose file won't be included in the generated image.

### Log in to Docker Hub

Running docker login connects our computer to our Docker Hub account, so we’re allowed to upload our own images.
Without logging in, we wouldn’t be able to push (upload) our image to our repository.

### Push our image to Docker Hub

`docker push mouadthf/collection-app:latest` uploads the image we built to our Docker Hub repository.
Once pushed, it’s stored online and anyone can download it.
This means people don’t need our source code or Maven build tools — they just pull and run our pre-built image.

### Share with others

Now that our image is online and our Compose file refers to it, we can share both with anyone.
They simply need Docker installed, our docker-compose.yml file mentioned above, and the command docker-compose up.
This will pull all the correct images automatically and run the app exactly the same way it runs on our machine — no extra setup, no local builds, no Maven.

---
