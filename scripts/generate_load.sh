#!/bin/bash
HOST="localhost";
PORT="8084";
URL="http://${HOST}:${PORT}";
DEPLOYMENT="orders-orders";
SERVICE_PATH="micro/orders";

HS256_KEY=E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a;
ITEM_ID=13401

# trap ctrl-c and call ctrl_c() to stop port forwarding
trap ctrl_c INT

function ctrl_c() {
	echo "** Trapped CTRL-C... Killing Port Forwarding and Stopping Load";
	killall kubectl;
	exit 0;
}

function start_port_forwarding() {
	echo "Forwarding service port ${PORT}";
	kubectl port-forward deployment/${DEPLOYMENT} ${PORT}:${PORT} --pod-running-timeout=1h &
	echo "Sleeping for 3 seconds while connection is established...";
	sleep 3;
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

# Port Forwarding
start_port_forwarding

# Load Generation
echo "Generating load..."
create_jwt_blue

while true; do
	curl -s -H "Authorization: Bearer ${jwt_blue}" ${URL}/${SERVICE_PATH} > /dev/null;
	sleep 0.2;
done