#!/bin/bash

ITEM_ID=13401

function parse_arguments {
	# ORDERS_HOST
	if [ -z "${ORDERS_HOST}" ]; then
		echo "ORDERS_HOST not set. Using parameter \"$1\"";
		ORDERS_HOST=$1;
	fi

	if [ -z "${ORDERS_HOST}" ]; then
		echo "ORDERS_HOST not set. Using default key";
		ORDERS_HOST=127.0.0.1;
	fi

	# ORDERS_PORT
	if [ -z "${ORDERS_PORT}" ]; then
		echo "ORDERS_PORT not set. Using parameter \"$2\"";
		ORDERS_PORT=$2;
	fi

	if [ -z "${ORDERS_PORT}" ]; then
		echo "ORDERS_PORT not set. Using default key";
		ORDERS_PORT=9446;
	fi

		# AUTH_HOST
	if [ -z "${AUTH_HOST}" ]; then
		echo "AUTH_HOST not set. Using parameter \"$1\"";
		AUTH_HOST=$1;
	fi

	if [ -z "${AUTH_HOST}" ]; then
		echo "AUTH_HOST not set. Using default key";
		AUTH_HOST=127.0.0.1;
	fi

	# AUTH_PORT
	if [ -z "${AUTH_PORT}" ]; then
		echo "AUTH_PORT not set. Using parameter \"$2\"";
		AUTH_PORT=$2;
	fi

	if [ -z "${AUTH_PORT}" ]; then
		echo "AUTH_PORT not set. Using default key";
		AUTH_PORT=9443;
	fi
}

function get_token {
	ACCESS_TOKEN=$(curl -k -d 'grant_type=password&client_id=bluecomputeweb&client_secret=bluecomputewebs3cret&username=user&password=password&scope=openid' https://${AUTH_HOST}:${AUTH_PORT}/oidc/endpoint/OP/token | jq -r '.access_token')
	echo $ACCESS_TOKEN
}

function create_order {
	CURL=$(curl -k -X POST --url https://${ORDERS_HOST}:${ORDERS_PORT}/orders/rest/orders --header "Content-Type: application/json" --header "Authorization: Bearer ${ACCESS_TOKEN}" -d '{"itemId":13401, "count":1}')
	echo $CURL

	# Check for 201 Status Code
	if [ "$CURL" != "201" ]; then
		printf "create_order: ❌ \n${CURL}\n";
        exit 1;
    else
    	echo "create_order: ✅";
    fi
}

function get_order {
	echo $ACCESS_TOKEN
	CURL=$(curl -k --request GET --url https://${ORDERS_HOST}:${ORDERS_PORT}/orders/rest/orders --header "Authorization: Bearer ${ACCESS_TOKEN}" --header "Content-Type: application/json")
	echo "Found order with itemId: \"${CURL}\""

	# if [ "$CURL" != "$ITEM_ID" ]; then
	# 	echo "get_order: ❌ could not find itemId";
  #       exit 1;
  #   else
  #   	echo "get_order: ✅";
  #   fi
}

# Setup
parse_arguments $1 $2 $3 $4 $5
get_auth
get_token

# API Tests
echo "Starting Tests"
get_order
create_order
get_order