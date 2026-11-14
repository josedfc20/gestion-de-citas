# syntax=docker/dockerfile:1
# Imagen de producción del backend (paso 7a). La construye la CI y la publica en
# GitHub Container Registry al crear una etiqueta v*; en el PC, docker-compose.prod.yml.

# Etapa 1: compilar el .jar. Corre en la arquitectura de quien construye
# (BUILDPLATFORM): el .jar es el mismo para Intel y ARM, así que no se emula ARM.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21.0.13_9-jdk-noble AS compilar
WORKDIR /fuente
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Primero solo las dependencias: se guardan en caché mientras no cambie el pom.xml.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
# Sin las pruebas: ya las pasa el trabajo "Backend" de la CI con ./mvnw verify.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/gestion-de-citas-backend-*.jar \
        extract --layers --launcher --destination target/capas

# Etapa 2: solo el JRE 21 y la aplicación, separada en capas (las librerías casi
# nunca cambian, así que una versión nueva descarga poco).
FROM eclipse-temurin:21.0.13_9-jre-noble
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=compilar /fuente/target/capas/dependencies/ ./
COPY --from=compilar /fuente/target/capas/spring-boot-loader/ ./
COPY --from=compilar /fuente/target/capas/snapshot-dependencies/ ./
COPY --from=compilar /fuente/target/capas/application/ ./
# Nunca como root.
USER app
EXPOSE 8080
# Java usa como mucho el 75 % de la memoria que le deje el contenedor.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
