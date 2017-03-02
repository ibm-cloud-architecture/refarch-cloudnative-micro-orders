#!/bin/bash

_term() {
    for pid in ${pids}; do
        kill -TERM "${pid}" 2>/dev/null
    done
}

trap _term SIGINT SIGTERM

if [ ! -z ${JDBC_URL} ]; then
    # parse out these params from the JDBC URL
    DB_IP=`echo ${JDBC_URL} | sed -e 's/jdbc:mysql:\/\///' | sed -e 's/:.*//g'`
    DB_PORT=`echo ${JDBC_URL} | sed -e 's/jdbc:mysql:\/\///' | sed -e 's/.*:\([^\/]*\)\/.*$/\1/g'`
    DB_NAME=`echo ${JDBC_URL} | sed -e 's/jdbc:mysql:\/\///' | sed -e 's/.*\///'`
fi

sed -i -e 's/__HTTP_PORT__/'${HTTP_PORT}'/g' /config/server.xml
sed -i -e 's/__DB_IP__/'${DB_IP}'/g' /config/server.xml
sed -i -e 's/__DB_PORT__/'${DB_PORT}'/g' /config/server.xml
sed -i -e 's/__DB_NAME__/'${DB_NAME}'/g' /config/server.xml
sed -i -e 's/__DB_USER__/'${DB_USER}'/g' /config/server.xml
sed -i -e 's/__DB_PASSWD__/'${DB_PASSWD}'/g' /config/server.xml

# kafka properties
sed -i -e 's/__KAFKA_USERNAME__/'${KAFKA_USERNAME}'/g' /config/server.xml
sed -i -e 's/__KAFKA_PASSWORD__/'${KAFKA_PASSWORD}'/g' /config/server.xml
sed -i -e 's/__KAFKA_BROKERS__/'${KAFKA_BROKER_LIST}'/g' /config/producer.properties

# newrelic properties
sed -i -e 's/app_name: My Application/app_name: '${CG_NAME}'/g' /agents/newrelic/newrelic.yaml
sed -i -e "s/license_key: 'YOUR_LICENSE_KEY'/license_key: '${NEW_RELIC_LICENSE_KEY}'/g" /agents/newrelic/newrelic.yaml

# start the sidecar -- note if the container dies i have to re-run a new instance
# as the sidecar doesnt' get restarted with the container (we don't use an init system)
java -jar /spring-orders-sidecar-0.0.1.jar &
pids="${pids} $!"

# start wlp
/opt/ibm/docker/docker-server "run" "defaultServer" &
pids="${pids} $!"

# wait for all jobs to finish, or get the above signals
wait
