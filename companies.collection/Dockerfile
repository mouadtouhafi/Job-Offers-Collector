# Stage 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /collection-app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /collection-app
COPY --from=build /collection-app/target/companies.collection-0.0.1-SNAPSHOT.jar collection-app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "collection-app.jar"]