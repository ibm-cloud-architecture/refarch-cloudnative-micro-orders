FROM registry.ng.bluemix.net/ibmliberty

ARG db_name=ordersdb
ARG db_ip
ARG db_port=3306
ARG db_user=orders_dbuser
ARG db_passwd=Pass4ordersUs3r
ARG db_url

ENV DB_NAME ${db_name}
ENV DB_IP ${db_ip}
ENV DB_PORT ${db_port}
ENV DB_USER ${db_user}
ENV DB_PASSWD ${db_passwd}
ENV JDBC_URL ${db_url}

ENV HTTP_PORT=9080
ENV SIDECAR_APP_PORT=8080

# install mysql jdbc driver
RUN apt-get install libmysql-java
RUN mkdir -p /opt/ibm/wlp/usr/shared/resources/mysql
RUN chmod 777 /opt/ibm/wlp/usr/shared/resources/mysql
RUN cp /usr/share/java/mysql-connector-java-5.1.38.jar /opt/ibm/wlp/usr/shared/resources/mysql/
RUN chmod 755 /opt/ibm/wlp/usr/shared/resources/mysql/*.jar

RUN wget http://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/0.10.0.1/kafka-clients-0.10.0.1.jar -O /opt/ibm/wlp/usr/shared/resources/kafka-clients-0.10.0.1.jar

EXPOSE ${HTTP_PORT} ${SIDECAR_APP_PORT}

# rewrite server.xml for local execution
ADD config/server.xml /config/server.xml
ADD config/producer.properties /config/producer.properties

# copy dependencies
ADD config/apps/JavaOrdersAPI.war /config/apps/JavaOrdersAPI.war
ADD spring-orders-sidecar-0.0.1.jar /spring-orders-sidecar-0.0.1.jar

ADD entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

# Copy agent files
COPY agents/ /agents/
ADD config/jvm.options /config/jvm.options

# Add the new relic agent
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip -O /tmp/newrelic.zip \
  && unzip -u -d /agents /tmp/newrelic.zip newrelic/newrelic.jar \
  && rm /tmp/newrelic.zip

#CMD ["/opt/ibm/docker/docker-server", "run", "defaultServer"]

ENTRYPOINT [ "/entrypoint.sh" ]
CMD [""]
