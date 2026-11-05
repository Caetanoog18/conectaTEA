FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw -B -DskipTests package


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S conectatea \
    && adduser -S -G conectatea conectatea

COPY --from=build /workspace/target/*.jar /app/conectatea.jar

USER conectatea

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/conectatea.jar"]