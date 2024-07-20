FROM gradle:8.8.0-jdk21 AS builder

COPY ../.. /usr/src
WORKDIR /usr/src
RUN --mount=type=cache,target=/root/.gradle gradle clean build -xtest

FROM amazoncorretto:21-alpine

ARG PORT
ENV PORT=${PORT:-8080}

WORKDIR /usr/src/
COPY --from=builder /usr/src/build/libs/*.jar /usr/src/app.jar

EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "/usr/src/app.jar"]
