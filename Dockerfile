# STAGE: Build
FROM gradle:4.9.0-jdk8-alpine as builder

# Create Working Directory
ENV BUILD_DIR=/home/gradle/app/
RUN mkdir $BUILD_DIR
WORKDIR $BUILD_DIR

# Download Dependencies
COPY build.gradle $BUILD_DIR
RUN gradle build -x :bootRepackage -x test --continue

# Copy Code Over and Build jar
COPY src src
RUN gradle build -x test

# STAGE: Deploy
FROM openjdk:8-jre-alpine

# Install Extra Packages
RUN apk --no-cache update \
 && apk add jq bash bc ca-certificates curl \
 && update-ca-certificates

# Create app directory
ENV APP_HOME=/app
RUN mkdir $APP_HOME
WORKDIR $APP_HOME

# Copy jar file over from builder stage
COPY --from=builder /home/gradle/app/build/libs/micro-orders-0.0.1.jar $APP_HOME
RUN mv ./micro-orders-0.0.1.jar app.jar

COPY startup.sh startup.sh
COPY scripts scripts

# Create User and Chown
RUN addgroup -g 2000 blue \
	&& adduser -u 2000 -G blue -s /bin/bash -D blue \
	&& chown -R blue:blue $APP_HOME \
	&& chmod -R 0775 $APP_HOME

USER blue

EXPOSE 8084 8094
ENTRYPOINT ["./startup.sh"]
