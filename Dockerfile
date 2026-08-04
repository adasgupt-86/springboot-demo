#########################################
# Stage 1
#########################################

FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests

#########################################
# Stage 2
#########################################

FROM eclipse-temurin:21-jre

LABEL maintainer="Abhishek Dasgupta"

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=5s CMD wget --spider http://localhost:8080/actuator/health || exit 1
