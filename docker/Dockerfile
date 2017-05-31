FROM openjdk:8-alpine

ENV HTTP_PORT=8080

# install mysql jdbc driver
RUN apk --no-cache update \
 && apk add jq bc bash wget

EXPOSE ${HTTP_PORT}

ADD app.jar /app.jar

# Copy agent files
COPY agents/ /agents/
ADD config/jvm.options /config/jvm.options

# Add the new relic agent
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip -O /tmp/newrelic.zip \
  && unzip -d /agents /tmp/newrelic.zip newrelic/newrelic.jar \
  && rm /tmp/newrelic.zip

ADD entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]
