#!/bin/bash

ITEM_ID=13401

function parse_arguments() {
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
		ORDERS_PORT=8084;
	fi

	# HS256_KEY
	if [ -z "${HS256_KEY}" ]; then
		echo "HS256_KEY not set. Using parameter \"$3\"";
		HS256_KEY=$3;
	fi

	if [ -z "${HS256_KEY}" ]; then
		echo "HS256_KEY not set. Using default key";
		HS256_KEY=E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a;
	fi

	echo "Using http://${MICROSERVICE_HOST}:${MICROSERVICE_PORT}"
}

function create_jwt_admin() {
	# Secret Key
	secret=${HS256_KEY};
	# JWT Header
	jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
	# JWT Payload
	jwt2=$(echo -n "{\"scope\":[\"admin\"],\"user_name\":\"admin\"}" | openssl enc -base64);
	# JWT Signature: Header and Payload
	jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
	# JWT Signature: Create signed hash with secret key
	jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${secret}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
	# Complete JWT
	jwt=$(echo -n "${jwt3}.${jwt4}");

	#echo $jwt
}

function create_jwt_blue() {
	# Secret Key
	secret=${HS256_KEY};
	# JWT Header
	jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
	# JWT Payload
	jwt2=$(echo -n "{\"scope\":[\"blue\"],\"user_name\":\"admin\"}" | openssl enc -base64);
	# JWT Signature: Header and Payload
	jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
	# JWT Signature: Create signed hash with secret key
	jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${secret}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
	# Complete JWT
	jwt_blue=$(echo -n "${jwt3}.${jwt4}");

	#echo $jwt_blue
}

function create_order() {
	CURL=$(curl --write-out %{http_code} --silent --output /dev/null --max-time 5 -X POST -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt_blue}" -d "{\"itemId\":${ITEM_ID},\"count\":1}" http://${ORDERS_HOST}:${ORDERS_PORT}/micro/orders);
	echo "create_user status code: \"${CURL}\""

	# Check for 201 Status Code
	if [ -z "${CURL}" ] || [ "$CURL" != "201" ]; then
		printf "create_order: ❌ \n${CURL}\n";
        exit 1;
    else
    	echo "create_order: ✅";
    fi
}

function get_order() {
	CURL=$(curl -s --max-time 5 -H "Authorization: Bearer ${jwt_blue}" http://${ORDERS_HOST}:${ORDERS_PORT}/micro/orders | jq -r '.[0].itemId' | grep ${ITEM_ID});
	echo "Found order with itemId: \"${CURL}\""

	if [ -z "${CURL}" ] || [ "$CURL" != "$ITEM_ID" ]; then
		echo "get_order: ❌ could not find itemId";
        exit 1;
    else
    	echo "get_order: ✅";
    fi
}

# Setup
parse_arguments $1 $2 $3 $4 $5
create_jwt_admin
create_jwt_blue

# API Tests
echo "Starting Tests"
create_order
get_order
