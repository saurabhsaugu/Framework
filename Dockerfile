FROM maven:3.8.8-openjdk-11

WORKDIR /workspace
COPY . /workspace

# Optionally pre-download dependencies
RUN mvn -q -DskipTests dependency:go-offline

# Default command runs tests (override in docker run)
CMD ["mvn", "test", "-Ddriver=web"]

