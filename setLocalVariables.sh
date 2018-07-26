#!/usr/bin/env bash
# This simple bash script was made for your convenience to save time when export variables for local deployment.


# JDBC environment variables
export jdbcURL=jdbc:mysql://localhost:9041/ordersdb?useSSL=false
export dbuser=root
export dbpassword=password


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