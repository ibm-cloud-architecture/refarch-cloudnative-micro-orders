#!/bin/bash
source scripts/max_heap.sh
source scripts/parse_mysql.sh

set -x;
# Set Max Heap
export JAVA_OPTS="${JAVA_OPTS} -Xmx${max_heap}m"

# Set basic java options
export JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

# Parse MySQL info and put it into JAVA_OPTS
parse_mysql

# Parse messagehub
if [ -n "${messagehub}" ]; then 
    echo "Found messagehub secret"

    # Construct messagehub environment variables
    messagehub_creds=`echo ${messagehub}`
    kafka_username=`echo ${messagehub_creds} | jq '.user' | sed -e 's/"//g'`
    kafka_password=`echo ${messagehub_creds} | jq '.password' | sed -e 's/"//g'`
    kafka_brokerlist=`echo ${messagehub_creds} | jq '.kafka_brokers_sasl | join(" ")' | sed -e 's/"//g'`
    
    JAVA_OPTS="${JAVA_OPTS} \
    -Dspring.application.messagehub.user=${kafka_username} \
    -Dspring.application.messagehub.password=${kafka_password}"
    
    count=0
    for broker in ${kafka_brokerlist}; do
        JAVA_OPTS="${JAVA_OPTS} -Dspring.application.messagehub.kafka_brokers_sasl[${count}]=${broker}"
        count=$((count + 1))
    done
    
    kafka_apikey=`echo ${messagehub_creds} | jq '.api_key' | sed -e 's/"//g'`
    kafka_adminurl=`echo ${messagehub_creds} | jq '.kafka_admin_url' | sed -e 's/"//g'`
fi

# Parse HS256_KEY
if [ -n "${HS256_KEY}" ]; then
    echo "Found HS256_KEY"
    hs256_key=${HS256_KEY}
    JAVA_OPTS="${JAVA_OPTS} -Djwt.sharedSecret=${hs256_key}"
fi

# disable eureka
JAVA_OPTS="${JAVA_OPTS} -Deureka.client.enabled=false -Deureka.client.registerWithEureka=false -Deureka.fetchRegistry=false"

echo "Starting Java Application"

set +x;
# Start the application
exec java ${JAVA_OPTS} -jar /app.jar