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
		ORDERS_HOST=localhost;
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
		echo "AUTH_HOST not set. Using parameter \"$3\"";
		AUTH_HOST=$3;
	fi

	if [ -z "${AUTH_HOST}" ]; then
		echo "AUTH_HOST not set. Using default key";
		AUTH_HOST=localhost;
	fi

	# AUTH_PORT
	if [ -z "${AUTH_PORT}" ]; then
		echo "AUTH_PORT not set. Using parameter \"$4\"";
		AUTH_PORT=$4;
	fi

	if [ -z "${AUTH_PORT}" ]; then
		echo "AUTH_PORT not set. Using default key";
		AUTH_PORT=9443;
	fi

	# INV_HOST
	if [ -z "${INV_HOST}" ]; then
		echo "INV_HOST not set. Using parameter \"$5\"";
		INV_HOST=$5;
	fi

	if [ -z "${INV_HOST}" ]; then
		echo "INV_PORT not set. Using default key";
		INV_HOST=localhost;
	fi

	# INV_PORT
	if [ -z "${INV_PORT}" ]; then
		echo "INV_PORT not set. Using parameter \"$6\"";
		INV_PORT=$6;
	fi

	if [ -z "${INV_PORT}" ]; then
		echo "INV_PORT not set. Using default key";
		INV_PORT=9081;
	fi
}

function health_auth {
	echo "Checking Auth Health"
	CURL=$(curl -k https://${AUTH_HOST}:${AUTH_PORT}/health)
	echo $CURL
}

function health_inv {
	echo "Check Inventory Health"
	CURL=$(curl -k http://${INV_HOST}:${INV_PORT}/health)
	echo $CURL
}

function get_token {
	ACCESS_TOKEN=$(curl -k -d 'grant_type=password&client_id=bluecomputeweb&client_secret=bluecomputewebs3cret&username=user&password=password&scope=openid' https://${AUTH_HOST}:${AUTH_PORT}/oidc/endpoint/OP/token | jq -r '.access_token')
	# echo $ACCESS_TOKEN
}

function get_order_first {
	# echo $ACCESS_TOKEN
	CURL=$(curl -k --request GET --url https://${ORDERS_HOST}:${ORDERS_PORT}/orders/rest/orders --header "Authorization: Bearer ${ACCESS_TOKEN}" --header "Content-Type: application/json")
	# echo "Retrieved orders: ${CURL}"

	ORDERS_POD=$(kubectl get pods | grep -v "orders-orders-job" | grep orders-orders | awk '{print $1}')

  # kubectl describe pod $ORDERS_POD
  # kubectl logs $ORDERS_POD
	# AUTH_POD=$(kubectl get pods | grep auth-auth | awk '{print $1}')
  # kubectl describe pod $AUTH_POD
  # kubectl logs $AUTH_POD

	# No orders have been made
	if [ "$CURL" != "[]" ]; then
		echo "get_order: ❌ did not get empty list";
		echo $CURL
		echo "ORDERS POD LOGS:"
		kubectl logs $ORDERS_POD
        exit 1;
    else
    	echo "get_order: ✅";
    fi
}

function create_order {
	# echo "Sending request:"
	# echo "curl -k -X POST --url https://${ORDERS_HOST}:${ORDERS_PORT}/orders/rest/orders --header "Content-Type: application/json" --header "Authorization: Bearer ${ACCESS_TOKEN}" -d '{"itemId":13401, "count":1}'"
	CURL=$(curl -w %{http_code} -k -X POST --url https://${ORDERS_HOST}:${ORDERS_PORT}/orders/rest/orders --header "Content-Type: application/json" --header "Authorization: Bearer $ACCESS_TOKEN" -d "{\"itemId\":13401, \"count\":1}")
	# echo $CURL

	# ORDERS_POD=$(kubectl get pods | grep -v "orders-orders-job" | grep orders-orders | awk '{print $1}')
  # kubectl describe pod $ORDERS_POD
  # kubectl logs $ORDERS_POD
	# AUTH_POD=$(kubectl get pods | grep auth-auth | awk '{print $1}')
  # kubectl describe pod $AUTH_POD
  # kubectl logs $AUTH_POD

	# Check for 201 Status Code
	if [ "$CURL" != "201" ]; then
		printf "create_order: ❌ \n${CURL}\n";
        exit 1;
    else
    	echo "create_order: ✅";
    fi
}

function get_order {
	# echo $ACCESS_TOKEN
	CURL=$(curl -k --request GET --url https://${ORDERS_HOST}:${ORDERS_PORT}/orders/rest/orders --header "Authorization: Bearer ${ACCESS_TOKEN}" --header "Content-Type: application/json" | jq '.[0]' | jq '.itemId')
	# echo "Found order with itemId: \"${CURL}\""

	# Return created order
	if [ "$CURL" != "13401" ]; then
		echo "get_order: ❌ could not find itemId";
        exit 1;
    else
    	echo "get_order: ✅";
    fi
}

# Setup
parse_arguments $1 $2 $3 $4 $5 $6
health_auth
health_inv
get_token

# API Tests
echo "Starting Tests"
get_order_first
create_order
get_order