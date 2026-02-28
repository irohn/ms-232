FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml ./
COPY src ./src

# Cache dependencies before copying the rest of the repository.
RUN mvn -B -q dependency:go-offline

COPY resources ./resources
COPY scripts ./scripts
COPY sql ./sql
COPY loadins ./loadins

RUN mvn -B -DskipTests package
RUN cp "$(ls /build/bin/*.jar | grep -v '/original-' | head -n 1)" /build/server.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/server.jar /app/server.jar
COPY --from=builder /build/resources /app/resources
COPY --from=builder /build/src /app/src
COPY --from=builder /build/scripts /app/scripts
COPY --from=builder /build/sql /app/sql
COPY --from=builder /build/loadins /app/loadins

RUN mkdir -p /app/logs /app/dat

CMD ["java", "-jar", "/app/server.jar"]
