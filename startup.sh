#!/bin/bash
source scripts/max_heap.sh

# Set Max Heap
export JAVA_OPTS="${JAVA_OPTS} -Xmx${max_heap}m"

# Set basic java options
export JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

echo "Starting Java Application"

# Start the application
exec java ${JAVA_OPTS} -jar /app.jar