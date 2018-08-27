#!/bin/bash

function parse_arguments() {
	#set -x;
	# CUSTOMER_HOST
	if [ -z "${CUSTOMER_HOST}" ]; then
		echo "CUSTOMER_HOST not set. Using parameter \"$1\"";
		CUSTOMER_HOST=$1;
	fi

	if [ -z "${CUSTOMER_HOST}" ]; then
		echo "CUSTOMER_HOST not set. Using default key";
		CUSTOMER_HOST=127.0.0.1;
	fi

	# CUSTOMER_PORT
	if [ -z "${CUSTOMER_PORT}" ]; then
		echo "CUSTOMER_PORT not set. Using parameter \"$2\"";
		CUSTOMER_PORT=$2;
	fi

	if [ -z "${CUSTOMER_PORT}" ]; then
		echo "CUSTOMER_PORT not set. Using default key";
		CUSTOMER_PORT=8080;
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

	# TEST_USER
	if [ -z "${TEST_USER}" ]; then
		echo "TEST_USER not set. Using parameter \"$4\"";
		TEST_USER=$4;
	fi

	if [ -z "${TEST_USER}" ]; then
		echo "TEST_USER not set. Using default key";
		TEST_USER=user;
	fi

	# TEST_PASSWORD
	if [ -z "${TEST_PASSWORD}" ]; then
		echo "TEST_PASSWORD not set. Using parameter \"$5\"";
		TEST_PASSWORD=$5;
	fi

	if [ -z "${TEST_PASSWORD}" ]; then
		echo "TEST_PASSWORD not set. Using default key";
		TEST_PASSWORD=passw0rd;
	fi

	#set +x;
}

function create_jwt_admin() {
	# Secret Key
	secret=${HS256_KEY};
	# JWT Header
	jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
	# JWT Payload
	jwt2=$(echo -n "{\"scope\":[\"admin\"],\"user_name\":\"${TEST_USER}\"}" | openssl enc -base64);
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
	jwt2=$(echo -n "{\"scope\":[\"blue\"],\"user_name\":\"${TEST_USER}\"}" | openssl enc -base64);
	# JWT Signature: Header and Payload
	jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
	# JWT Signature: Create signed hash with secret key
	jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${secret}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
	# Complete JWT
	jwt_blue=$(echo -n "${jwt3}.${jwt4}");

	#echo $jwt_blue
}

function create_user() {
	CURL=$(curl --write-out %{http_code} --silent --output /dev/null --max-time 5 -X POST "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/micro/customer" -H "Content-type: application/json" -H "Authorization: Bearer ${jwt}" -d "{\"username\": \"${TEST_USER}\", \"password\": \"${TEST_PASSWORD}\", \"firstName\": \"user\", \"lastName\": \"name\", \"email\": \"user@name.com\"}");

	# Check for 201 Status Code
	if [ "$CURL" != "201" ]; then
		printf "create_user: ❌ \n${CURL}\n";
        exit 1;
    else 
    	echo "create_user: ✅";
    fi
}

function search_user() {
	CURL=$(curl -s --max-time 5 -X GET "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/micro/customer/search?username=${TEST_USER}" -H 'Content-type: application/json' -H "Authorization: Bearer ${jwt}" | jq -r '.[0].username' | grep ${TEST_USER});
	#echo "Found user with name: \"${CURL}\""

	if [ "$CURL" != "$TEST_USER" ]; then
		echo "search_user: ❌ could not find user";
        exit 1;
    else 
    	echo "search_user: ✅";
    fi
}

function delete_user() {
	CUSTOMER_ID=$(curl -s --max-time 5 -X GET "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/micro/customer/search?username=${TEST_USER}" -H 'Content-type: application/json' -H "Authorization: Bearer ${jwt}" | jq -r '.[0].customerId');
	
	#echo "Deleting customer with name: ${TEST_USER} and id: ${CUSTOMER_ID}"
	CURL=$(curl --write-out %{http_code} --silent --output /dev/null --max-time 5 -X DELETE "http://${CUSTOMER_HOST}:${CUSTOMER_PORT}/micro/customer/${CUSTOMER_ID}" -H "Content-type: application/json" -H "Authorization: Bearer ${jwt}");

	# Check for 201 Status Code
	if [ "$CURL" != "200" ]; then
		printf "delete_user: ❌ \n${CURL}\n";
        exit 1;
    else 
    	echo "delete_user: ✅";
    fi
}

# Setup
parse_arguments $1 $2 $3 $4 $5
create_jwt_admin
create_jwt_blue

# API Tests
echo "Starting Tests"
create_user
search_user
delete_user