#!/bin/bash

# JDBC environment variables
export jdbcURL=jdbc:mysql://localhost:9039/ordersdb?useSSL=false
export dbuser=root
export dbpassword=password

# JWT
export jwksUri=https://localhost:9443/oidc/endpoint/OP/jwk
export jwksIssuer=https://localhost:9443/oidc/endpoint/OP
export administratorRealm=https://localhost:9443/oidc/endpoint/OP

# RabbitMQ
export rabbit=localhost

# Zipkin
# Although optional, defining this will prevent warning and error messages
export zipkinHost=localhost
export zipkinPort=9411

# Some environment variables in preparation for communicating with other MicroServices, although not necessary
export auth_health=https://localhost:9443/health
export inventory_url=http://localhost:9081/inventory/rest/inventory/stock
export inventory_health=http://localhost:9081/health
