FROM openjdk:8

ENV HTTP_PORT=8080

# install mysql jdbc driver
RUN apt-get update
RUN apt-get install libmysql-java jq

EXPOSE ${HTTP_PORT}

ADD app.jar /app.jar

ADD entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

# Copy agent files
COPY agents/ /agents/
ADD config/jvm.options /config/jvm.options

# Add the new relic agent
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip -O /tmp/newrelic.zip \
  && unzip -u -d /agents /tmp/newrelic.zip newrelic/newrelic.jar \
  && rm /tmp/newrelic.zip


ENTRYPOINT [ "/entrypoint.sh" ]
