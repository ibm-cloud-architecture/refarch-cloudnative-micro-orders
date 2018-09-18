#!/bin/bash
source scripts/uri_parser.sh

function parse_from_uri() {
	# Do the URL parsing
	uri_parser $1

	# Construct mysql url
    mysql_url="jdbc:${uri_schema}://${uri_host}:${uri_port}/${uri_path:1}"
    mysql_user=${uri_user}
    mysql_password=${uri_password}
    mysql_port=${uri_port}

    JAVA_OPTS="${JAVA_OPTS} -Dspring.datasource.url=${mysql_url}"
    JAVA_OPTS="${JAVA_OPTS} -Dspring.datasource.username=${mysql_user}"
    JAVA_OPTS="${JAVA_OPTS} -Dspring.datasource.password=${mysql_password}"
    JAVA_OPTS="${JAVA_OPTS} -Dspring.datasource.port=${mysql_port}"	
}

function parse_mysql() {
	echo "Parsing mysql info"

	if [ -n "$MYSQL_URI" ]; then
		echo "Getting elements from MYSQL_URI"
		parse_from_uri $MYSQL_URI

	elif [ -n "$mysql" ]; then
		echo "Using old MySQL Chart";
	    mysql_uri=$(echo $mysql | jq -r .uri)
	    parse_from_uri $mysql_uri

    elif [ -n "$MYSQL_PASSWORD" ]; then
	    echo "Using MySQL Community Chart"
	    parse_from_uri "mysql://${MYSQL_USER}:${MYSQL_PASSWORD}@${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}"

	else
	    echo "No Password was set. Probably using passwordless root"
	    parse_from_uri "mysql://${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}"
	fi
}