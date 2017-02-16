# Orders Microservice

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative*

## Introduction

This project is built to demonstrate how to build a Microservices application implemented as a web application deployed to an IBM WebSphere Liberty Profile docker container. It provides basic operations of saving and querying orders from a database as part of the Orders function of BlueCompute. The project covers following technical areas:

 - Leverage [ibmliberty](https://console.ng.bluemix.net/docs/images/docker_image_ibmliberty/ibmliberty_starter.html) Docker image
 - Deploy the Orders microservices to containers on the [IBM Bluemix Container Service](https://console.ng.bluemix.net/docs/containers/container_index.html).
 - Persist order data to the MySQL database
 - Integrate with the [Spring Cloud Netflix Eureka](https://cloud.spring.io/spring-cloud-netflix/) framework using a Spring Boot Sidecar application
 - Produce messages on [IBM Message Hub](https://console.ng.bluemix.net/docs/services/MessageHub/index.html#messagehub) service on Bluemix for asynchronous communication with [Inventory Microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-inventory).
 
## Use Case
 
![Orders Microservice](orders_microservice.png)

- Orders Microservice persists orders in a MySQL database.  
- When a new order is placed, a record is saved in the database and a message is posted on MessageHub to notify interested subscribers
  - In BlueCompute case, the Inventory Microservice consumes the Order message to update the available stock of the item.
- When retrieving orders, return only orders belonging to the user identity passed from API Connect in the header `IBM-App-User`.  See the [BlueCompute Security architecture](https://github.com/ibm-cloud-architecture/refarch-cloudnative/blob/master/static/security.md) and [Authentication microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) for more details on how identity is propagated.

## REST API

The Orders Microservice REST API is behind the Zuul Proxy, which validates the caller using signed JWT tokens.  As such, only API exposed by API Connect are considered public API.  All Public REST API are OAuth 2.0 protected by the API Connect OAuth provider.  

- `GET /micro/orders` (public)
  - Returns all orders.  The caller of this API must pass API Connect a valid OAuth token.  API Connect will pass down the customer ID in the `IBM-App-User` header.  A JSON object array is returned consisting of only orders created by the customer ID.

- `GET /micro/orders/{id}` (public)
  - Return order by ID.  The caller of this API must pass API Connect a valid OAuth token.  API Connect will pass down the customer ID in the `IBM-App-User` header.  If the `id` of the order is owned by the customer passed in the `IBM-App-User` header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

- `POST /micro/orders` (public)
  - Create an order.  The caller of this API must pass API Connect a valid OAuth token.  API Connect will pass down the customer ID in the `IBM-App-User` header.  The Order object must be passed as JSON object in the request body with the following format:
    ```
    {
      "itemId": <item id>,
      "count": <number of items in order>,
    }
    ```

    On success, `HTTP 201` is returned with the ID of the created order in the `Location` response header.

## Pre-requisites

### Create an IBM MessageHub Service Instance

*Note that two components use MessageHub in BlueCompute, this Orders microservice and the [Inventory microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-inventory).  If deploying both services, the two components must share the same MessageHub instance.*

1. Login to your Bluemix console
2. Open browser to create a Message Hub service using this link: [https://console.ng.bluemix.net/catalog/services/message-hub/](https://console.ng.bluemix.net/catalog/services/message-hub/)
3. Name the Message Hub service name like `refarch-messagehub`
4. Use the `Standard` plan, then click `Create`
5. Once the service has been created, open the `Service Credentials` tab and take a note of the service credentials.  For example,

   ```
   {
     "mqlight_lookup_url": "https://mqlight-lookup-prod01.messagehub.services.us-south.bluemix.net/Lookup?serviceId=xxxxxxx",
     "api_key": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
     "kafka_admin_url": "https://kafka-admin-prod01.messagehub.services.us-south.bluemix.net:443",
     "kafka_rest_url": "https://kafka-rest-prod01.messagehub.services.us-south.bluemix.net:443",
     "kafka_brokers_sasl": [
       "kafka01-prod01.messagehub.services.us-south.bluemix.net:9093",
       "kafka02-prod01.messagehub.services.us-south.bluemix.net:9093",
       "kafka03-prod01.messagehub.services.us-south.bluemix.net:9093",
       "kafka04-prod01.messagehub.services.us-south.bluemix.net:9093",
       "kafka05-prod01.messagehub.services.us-south.bluemix.net:9093"
     ],
     "user": "<MessageHub Username>",
     "password": "<MessageHub Password>"
   }
   ```

### Install Docker

Install [Docker](https://www.docker.com)

### Install Cloud Foundry CLI and IBM Containers plugin

Install the [Cloud Foundry CLI](https://console.ng.bluemix.net/docs/starters/install_cli.html) and the [IBM Containers Plugin](https://console.ng.bluemix.net/docs/cli/plugins/containers/index.html)

### Create a MySQL database instance

*Note that two components in BlueCompute use MySQL databases, this service and the [Inventory microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-inventory).  If deploying both services, it is possible to have the data reside in the same MySQL instance, but in production deployments it is recommended that each microservice has its own separate database.*

Install MySQL instance.  Here are two options:

1. On IBM Bluemix, you can create one using [Compose for MySQL](https://console.ng.bluemix.net/catalog/services/compose-for-mysql/).
   1. Once it is ready, on the `Service Credentials, note the `uri` and `uri_cli` property.  Run the `url_cli` command in a console:
   
      ```
     # mysql -u admin -p --host bluemix-sandbox-dal-9-portal.0.dblayer.com --port xxxxx --ssl-mode=REQUIRED
      ```
      
      The password is embedded in the `uri` property after `admin`.
   
   2. Create the `orders` table in the `compose` database:
   
      ```
      mysql> use compose;
      Database changed
      mysql> source mysql/create_orders_table.sql
      Query OK, 0 rows affected (0.11 sec)
      ```
   
   3. The MySQL JDBC URL is constructed using the `uri` property in the Compose credentials.  For example, for a `uri` such as the following,
      ```
      mysql://admin:zzzzzzzzzzzzzz@bluemix-sandbox-dal-9-portal.0.dblayer.com:22627/compose
      ```
      
      The JDBC URL is:
      ```
      jdbc:mysql://bluemix-sandbox-dal-9-portal.0.dblayer.com:22627/compose
      ```
      
      The database username is `admin` and the password is `zzzzzzzzzzzzzz`.
   

2. Using the IBM Bluemix container service, create a MySQL container using the following steps:
   1. Install the [Cloud Foundry CLI](https://github.com/cloudfoundry/cli/releases)
   2. Install the [IBM Containers plugin](https://console.ng.bluemix.net/docs/containers/container_cli_cfic.html)
   3. Pull the mysql image from docker hub
      
      ```
      # docker pull mysql
      ```
      
   4. Push the mysql image into the private bluemix registry
      
      ```
      # docker tag registry.ng.bluemix.net/$(cf ic namespace get)/mysql
      ```
   
   5. Request a public IP for the container:
      ```
      # cf ic ip request
      OK
      The IP address "x.x.x.x" was obtained.
      ```
   
   6. Run the MySQL docker container:
      *you may wish to change the root password for the MySQL instance
      
      ```
      # cf ic run -d -p x.x.x.x:3306:3306 --name mysql-orders -e "MYSQL_ROOT_PASSWORD=adminpasswd" registry.ng.bluemix.net/$(cf ic namespace get)/mysql 
      ```
      
   7. Copy the script `mysql/create_orders_db.sh` to the container.  You may note the parameters at the top of the script and change them if you do not want to use the defaults:
      
      ```
      # cf ic cp mysql/create_orders_table.sql mysql-orders:/root/create_orders_table.sql
      # cf ic cp mysql/create_orders_db.sh mysql-orders:/root/create_orders_db.sh
      ```
      
   8. Execute the script in the container:
      ```
      # cf ic exec mysql-orders /root/create_orders_db.sh
      ```
      
      This creates the database and `orders` table, with the username and password.
      
      The MySQL JDBC URL is constructed as the following:
      ```
      jdbc:mysql://<public_ip>:3306/ordersdb
      ```

Note that in the BlueCompute production deployment a highly available MySQL Cluster instance is created on-premise following the instructions [here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-resiliency).

## Deploy to BlueMix

You can use the following button to deploy the Orders microservice to Bluemix, or you can follow the instructions manually below.

The deployment creates a topic named `orders` in an existing IBM Message Hub instance and deploys the orders microservice in a container group on the IBM Bluemix Container Service.

[![Create BlueCompute Deployment Toolchain](https://console.ng.bluemix.net/devops/graphics/create_toolchain_button.png)](https://console.ng.bluemix.net/devops/setup/deploy?repository=https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders.git)

## Create a Topic in IBM Message Hub

In the Bluemix console, under `Services`, locate the Message Hub service under `Application Services`.  Click on the instance to be taken to the management portal.

Click on the `+` icon to create a topic.  Name the topic `orders`, with 1 partition and 24 hour retention.  Click `Save` when complete.

## Build the Docker container

1. Build the application.  This builds both the WAR file for the Orders REST API and also the Spring Sidecar application:

   ```
   # ./gradlew build
   ```

2. Copy the binaries to the docker container
   
   ```
   # ./gradlew docker
   ```

3. Build the docker container
   ```
   # cd docker
   # docker build -t orders-microservice .
   ```

## Run the Docker container locally (optional)

Execute the following to run the Docker container locally.  Make sure to update the `Eureka URL`, `MySQL JDBC URL`, `MySQL DB Username`, `MySQL DB Password`, `MessageHub Username`, and `MessageHub Password`.

```
# docker run -d --name orders-microservice -P \
  -e eureka.client.fetchRegistry=true \
  -e eureka.client.registerWithEureka=true \
  -e eureka.client.serviceUrl.defaultZone=<Eureka URL> \
  -e JDBC_URL=<MySQL jdbc url> \
  -e DB_USER=<MySQL DB username> \
  -e DB_PASSWD=<MySQL DB password> \
  -e KAFKA_BROKER_LIST=kafka01-prod01.messagehub.services.us-south.bluemix.net:9093,kafka02-prod01.messagehub.services.us-south.bluemix.net:9093,kafka03-prod01.messagehub.services.us-south.bluemix.net:9093,kafka04-prod01.messagehub.services.us-south.bluemix.net:9093,kafka05-prod01.messagehub.services.us-south.bluemix.net:9093 \
  -e KAFKA_USERNAME=<MessageHub Username> \
  -e KAFKA_PASSWORD=<MessageHub Password> \
  orders-microservice
```


## Run the Docker container on Bluemix

1. Log into the Cloud Foundry CLI
   ```
   # cf login
   ```
   
   Be sure to set the correct target space where the MessageHub instance was provisioned.
   
2. Initialize the Bluemix Containers plugin
   
   ```
   # cf ic init
   ```
   
   Ensure that the container namespace is set:
   ```
   # cf ic namespace get
   ```
   
   If it is not set, use the following command to set it:
   ```
   # cf ic namespace set <namespace>
   ```
   
3. Tag and push the docker image to the Bluemix private registry:

   ```
   # docker tag orders-microservice registry.ng.bluemix.net/$(cf ic namespace get)/orders-microservice
   # docker push registry.ng.bluemix.net/$(cf ic namespace get)/orders-microservice
   ```

4. Execute the following to run the Docker container on Bluemix Container Service.  Make sure to replace the Make sure to update the `Eureka URL`, `MySQL JDBC URL`, `MySQL DB Username`, `MySQL DB Password`, `MessageHub Username`, and `MessageHub Password`.

   ```
   # cf ic group create --name orders-microservice \
     --publish 9080 --publish 8080 \
     --memory 256 \
     -e eureka.client.fetchRegistry=true \
     -e eureka.client.registerWithEureka=true \
     -e eureka.client.serviceUrl.defaultZone=<Eureka URL> \
     -e JDBC_URL=<MySQL jdbc url> \
     -e DB_USER=<MySQL DB username> \
     -e DB_PASSWD=<MySQL DB password> \
     -e KAFKA_BROKER_LIST=kafka01-prod01.messagehub.services.us-south.bluemix.net:9093,kafka02-prod01.messagehub.services.us-south.bluemix.net:9093,kafka03-prod01.messagehub.services.us-south.bluemix.net:9093,kafka04-prod01.messagehub.services.us-south.bluemix.net:9093,kafka05-prod01.messagehub.services.us-south.bluemix.net:9093 \
     -e KAFKA_USERNAME=<MessageHub Username> \
     -e KAFKA_PASSWORD=<MessageHub Password> \
     registry.ng.bluemix.net/$(cf ic namespace get)/orders-microservice
   ```

## Validate the Orders microservice

Zuul performs authorization using signed JWT tokens generated by API Connect.  As such, to call the REST API directly from Bluemix, you must temporarily map a route to the container group which bypasses Zuul.

```
# cf ic route map -n <temp-routename> -d mybluemix.net orders-microservice 
```

### Set up Kafka Console sample to read messages from Bluemix:

1. Clone the git repo: [https://github.com/ibm-messaging/message-hub-samples](https://github.com/ibm-messaging/message-hub-samples).

2. Change the topic name to `orders` instead of `kafka-java-console-sample-topic`.  Edit the file `kafka-java-console-sample/src/com/messagehub/samples/MessageHubConsoleSample.java`, locate the line:

   ```
   private static final String TOPIC_NAME = "kafka-java-console-sample-topic";
   ```
   
   Change it to:
   
   ```
   private static final String TOPIC_NAME = "orders";
   ```
   
   Save the file.

3. Rebuild and execute the project.  Be sure to replace `Kafka Admin URL` and `Kafka API Key` with the credentials from your MessageHub instance:
   
   ```
   # cd kafka-java-console-sample
   # gradle build
   # java -jar  build/libs/kafka-java-console-sample-2.0.jar "kafka01-prod01.messagehub.services.us-south.bluemix.net:9093,kafka02-prod01.messagehub.services.us-south.bluemix.net:9093,kafka03-prod01.messagehub.services.us-south.bluemix.net:9093,kafka04-prod01.messagehub.services.us-south.bluemix.net:9093,kafka05-prod01.messagehub.services.us-south.bluemix.net:9093" <Kafka Admin URL> <Kafka API Key> -consumer
   ```

   Keep the terminal with the Kafka console application open.
   
### Create an order

The caller must pass a header, ```IBM-App-User```, to the API, which is passed by API Connect to identify the caller.

```
# curl -H "Content-Type: application/json" -H "IBM-App-User: abcdefg" -X POST -d '{"itemId":13401, "count":1}' https://<temp-routename>/micro/orders
```

In the Kafka console application terminal, you should see some messages being consumed on MessageHub, e.g.:

```
[2017-01-27 15:45:25,552] INFO Message consumed: ConsumerRecord(topic = orders, partition = 0, offset = 1, CreateTime = 1485548828784, checksum = 4229253196, serialized key size = 5, serialized value size = 47, key = order, value = "{id = 1, itemId=13401, customerId=abcdefg, count=1}") (com.messagehub.samples.ConsumerRunnable)
```

### Get all orders

The caller must pass a header, `IBM-App-User`, to the API, which is passed by API Connect to identify the caller.

```
# curl -H "IBM-App-User: abcdefg" https://temp-routename/micro/orders
[{id = 1, itemId=13401, customerId=abcdefg, count=1}, {id = 2, itemId=13401, customerId=abcdefg, count=1}]
```

### Unmap the temporary route

When verification is complete, unmap the route so that the only access to the orders microservice is through Zuul.  

```
# cf ic route unmap -n <temp-routename> -d mybluemix.net orders-microservice 
```

Delete the temporary route.

```
# cf delete-route -n <temp-routename> mybluemix.net
```
