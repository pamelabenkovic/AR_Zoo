# === Build stage ===
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom i source da Maven može napraviti dependency cache
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
# Build JAR (Spring Boot će ga "repackage"-ati)
RUN mvn -B -DskipTests package

# === Runtime stage ===
FROM eclipse-temurin:21-jre
WORKDIR /app

# Uzmi JAR iz build stage-a (naziv može varirati -> *.jar je najsigurnije)
COPY --from=build /workspace/target/*.jar /app/app.jar

# Cloud hosting očekuje PORT iz env varijable; u Springu si stavio server.port=${PORT:8080}
EXPOSE 8080

# Dodan JAVA_OPTS za lako tweakanje memorije itd.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
