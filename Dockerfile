# --- ETAPA DE COMPILACIÓN (BUILD) ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar el archivo pom.xml y descargar dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el jar ejecutable omitiendo pruebas unitarias
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- ETAPA DE EJECUCIÓN (RUNTIME) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el archivo jar generado en la etapa de build
COPY --from=build /app/target/loteclick-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto por defecto de la aplicación
EXPOSE 8082

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
