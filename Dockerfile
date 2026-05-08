ARG BUILDPLATFORM=linux/amd64

#### Stage 1: Build the application
FROM amazoncorretto:21 AS build_image

# Set the current working directory inside the image
WORKDIR /app

# Copy gradle executable to the image
COPY gradlew .
COPY gradle gradle
COPY build.gradle .

# Set permission to execute file
RUN chmod +x gradlew

# Convert line endings from Windows (CRLF) to Unix (LF)
RUN sed -i 's/\r$//' gradlew

# Copy the project source
COPY src src
# COPY libs/newrelic newrelic

COPY src/main/scripts/wait-for-it.sh wait-for-it.sh
RUN sed -i 's/\r$//' wait-for-it.sh && chmod +x wait-for-it.sh

COPY src/main/scripts/start.sh start.sh
RUN sed -i 's/\r$//' start.sh && chmod +x start.sh

# Package the application
RUN ./gradlew bootJar

WORKDIR /app/build
RUN mkdir -p dependency  \
    && (cd dependency || return; jar -xf ../libs/*.jar)

#### Stage 2: A minimal docker image with command to run the app
FROM --platform=${BUILDPLATFORM} amazoncorretto:21 AS runner

# Set the current working directory inside the image
WORKDIR /app

ARG DEPENDENCY=/app/build/dependency

# Copy project dependencies from the build stage
COPY --from=BUILD_IMAGE ${DEPENDENCY}/BOOT-INF/lib ./lib
COPY --from=BUILD_IMAGE ${DEPENDENCY}/META-INF ./META-INF
COPY --from=BUILD_IMAGE ${DEPENDENCY}/BOOT-INF/classes .
# COPY --from=BUILD_IMAGE /app/newrelic ./newrelic

COPY --from=BUILD_IMAGE /app/wait-for-it.sh ./wait-for-it.sh
COPY --from=BUILD_IMAGE /app/start.sh ./start.sh

# Set Environment Variables
ENV SPRING_CONFIG_LOCATION=file:/app/application.properties

EXPOSE 8080

ENTRYPOINT ["./start.sh"]
