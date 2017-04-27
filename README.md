# Orders Microservice

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative*

## Introduction

This project is built to demonstrate how to build a Microservices application implemented as a Spring Boot application deployed to IBM Bluemix Container Service. It provides basic operations of saving and querying orders from a relational database as part of the Orders function of BlueCompute. The project covers following technical areas:

 - Build a microservice as a Spring Boot Application
 - Deploy the Orders microservices to containers on the [IBM Bluemix Container Service](https://console.ng.bluemix.net/docs/containers/container_index.html).
 - Persist order data to a MySQL database
 - OAuth 2.0 protected APIs using Spring Security framework
 - Produce messages on [IBM Message Hub](https://console.ng.bluemix.net/docs/services/MessageHub/index.html#messagehub) service on Bluemix for asynchronous communication with [Inventory Microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-inventory).
 
## Use Case
 
![Orders Microservice](orders_microservice.png)

- Orders Microservice persists orders in a MySQL database.  
- When a new order is placed, a record is saved in the database and a message is posted on MessageHub to notify interested subscribers
  - In BlueCompute case, the Inventory Microservice consumes the Order message to update the available stock of the item.
- When retrieving orders using the OAuth 2.0 protected APIs, return only orders belonging to the user identity encoded in the `user_name` claim in the JWT payload.  See the [BlueCompute Security architecture](https://github.com/ibm-cloud-architecture/refarch-cloudnative/blob/master/static/security.md) and [Authentication microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) for more details on how identity is propagated.

## REST API

The Orders Microservice REST API is OAuth 2.0 protected and identifies and validates the caller using signed JWT tokens.  

- `GET /micro/orders`
  - Returns all orders.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim.  A JSON object array is returned consisting of only orders created by the customer ID.

- `GET /micro/orders/{id}`
  - Return order by ID.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim.  If the `id` of the order is owned by the customer passed in the `IBM-App-User` header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

- `POST /micro/orders`
  - Create an order.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim.  The Order object must be passed as JSON object in the request body with the following format:
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
   
#### Create a Topic in IBM Message Hub

In the Bluemix console, under `Services`, locate the Message Hub service under `Application Services`.  Click on the instance to be taken to the management portal.

Click on the `+` icon to create a topic.  Name the topic `orders`, with 1 partition and 24 hour retention.  Click `Save` when complete.


### Install Docker

Install [Docker](https://www.docker.com)

### Install Bluemix CLI and IBM Container Service plugins

Install the [bx CLI](https://clis.ng.bluemix.net/ui/home.html), the Bluemix container-registry Plugin and the Bluemix container-service plugin.  The plugins can be installed directly [here](http://plugins.ng.bluemix.net/ui/repository.html), or using the following commands:

```
# bx plugin install container-service -r Bluemix
# bx plugin install conatiner-registry -r Bluemix
```

### Install kubectl

Install the [kubectl CLI](https://kubernetes.io/docs/tasks/kubectl/install/).


### Create a Compose for MySQL database instance

*Note that two components in BlueCompute use MySQL databases, this service and the [Inventory microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-inventory).  If deploying both services, it is possible to have the data reside in the same MySQL instance, but in production deployments it is recommended that each microservice has its own separate database.*

On IBM Bluemix, [Compose for MySQL](https://console.ng.bluemix.net/catalog/services/compose-for-mysql/) is a hosted managed MySQL instance.  Create an instance.

1. Once it is ready, on the `Service Credentials` tab, note the `uri` and `uri_cli` property.  Run the `url_cli` command in a console:
   
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

### Create HS256 shared secret

As the APIs in this microservice are OAuth protected, the HS256 shared secret used to sign the JWT generated by the [Authorization Server](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) is needed to validate the access token provided by the caller.

A 2048-bit secret can be generated using the following command:

```
# cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 256 | head -n 1 | xargs echo -n
```

Note that if the [Authorization Server](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) is also deployed, it must use the *same* HS256 shared secret.

## Build the code

Build the application.

```
# ./gradlew build
```

## Validate the Orders microservice

### Start the Service

Execute the following to run the Docker container locally.  Make sure to update the `HS256 shared secret`, `MySQL JDBC URL`, `MySQL DB Username`, `MySQL DB Password`, `MessageHub Username`, and `MessageHub Password`.

```
#  java \
-Djwt.sharedSecret=<HS256 shared secret> \
-Dspring.datasource.url=<MySQL JDBC URL> \
-Dspring.datasource.username=<MySQL DB Username> \
-Dspring.datasource.password=<MySQL DB Password> \
-Dspring.datasource.port=<MySQL DB Port> \
-Dspring.application.messagehub.user=<MessageHub Username> \
-Dspring.application.messagehub.password=<MessageHub Password> \
-Dspring.application.messagehub.kafka_brokers_sasl[0]=kafka04-prod01.messagehub.services.us-south.bluemix.net:9093 \
-Dspring.application.messagehub.kafka_brokers_sasl[1]=kafka01-prod01.messagehub.services.us-south.bluemix.net:9093 \
-Dspring.application.messagehub.kafka_brokers_sasl[2]=kafka03-prod01.messagehub.services.us-south.bluemix.net:9093 \
-Dspring.application.messagehub.kafka_brokers_sasl[3]=kafka02-prod01.messagehub.services.us-south.bluemix.net:9093 \
-Dspring.application.messagehub.kafka_brokers_sasl[4]=kafka05-prod01.messagehub.services.us-south.bluemix.net:9093 \
-Deureka.client.enabled=false \
-Deureka.client.registerWithEureka=false \
-Deureka.fetchRegistry=false \
-jar build/libs/micro-orders-0.0.1.jar
```

This starts the Orders microservice in the current console.


### Set up Kafka Console sample to read messages from MessageHub:

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

### Generate a temporary JWT

Use the shared secret to generate a valid JWT signed with the shared secret generated above.  You can do this at [jwt.io](https://jwt.io) using the Debugger.  Paste the HS256 shared secret in the bottom in the box (and leave base64 encoded unchecked).  You can use the following payload:

```
{
  "scope": [ "blue" ],
  "user_name": "admin"
}
```

Copy the text that appears in "Encoded"; this is the signed JWT that will be used for the "Create Customer" call.


### Create an order

Run the following to create an order for the `admin` customer ID.  Be sure to use the JWT retrieved from the previous step in place of `<JWT>`.

```
# curl -H "Content-Type: application/json" -H "Authorization: Bearer <JWT>" -X POST -d '{"itemId":13401, "count":1}' http://localhost:8080/micro/orders
```

In the Kafka console application terminal, you should see some messages being consumed on MessageHub, e.g.:

```
[2017-01-27 15:45:25,552] INFO Message consumed: ConsumerRecord(topic = orders, partition = 0, offset = 1, CreateTime = 1485548828784, checksum = 4229253196, serialized key size = 5, serialized value size = 47, key = order, value = "{id = 1, itemId=13401, customerId=admin, count=1}") (com.messagehub.samples.ConsumerRunnable)
```

### Get all orders

Run the following to retrieve all orders for the `admin` customer ID.  Be sure to use the JWT retrieved from the previous step in place of `<JWT>`.

```
# curl -H "Authorization: Bearer <JWT>" http://localhost:8080/micro/orders
[{id = 1, itemId=13401, customerId=admin, count=1}, {id = 2, itemId=13401, customerId=admin, count=1}]
```

## Deploy to Bluemix

The service can be packaged as a Docker container and deployed to a Kubernetes cluster running on Bluemix. 

### Build the Docker Image

Copy the binaries to the `docker` directory and build the image.
   
```
# ./gradlew docker
# cd docker
# docker build -t orders-microservice .
```

### Push the Docker image to the Bluemix private container registry

1. Log into the Bluemix CLI

   ```
   # bx login
   ```
   
   Be sure to set the correct target space where the Compose for MySQL and the MessageHub instance was provisioned.
   
2. Initialize the Bluemix Container Service plugin
   
   ```
   # bx cs init
   ```
   
   Initialize the Bluemix Container Registry plugin:
   
   ```
   # bx cr login
   ```
   
   Get the registry namespace:
   
   ```
   # bx cr namespaces
   ```
   
   If there are no namespaces available, use the following command to create one:
   
   ```
   # bx cr namespace-add <namespace>
   ```
   
3. Tag and push the docker image to the Bluemix private registry:

   ```
   # docker tag orders-microservice registry.ng.bluemix.net/<namespace>/orders-microservice
   # docker push registry.ng.bluemix.net/<namespace>/orders-microservice
   ```

### Create a Kubernetes Cluster (if applicable)
   
If a Kubernetes cluster has not previously been created, create a free Kubernetes cluster using the following:
   
```
# bx cs cluster-create --name <cluster_name>
```
   
You can monitor the cluster creation using `bx cs clusters` and `bx cs workers <cluster_name>`. 
   
### Set up kubectl

Once the cluster has been created, download the configuration:
   
```
# bx cs cluster-config <cluster_name>
```
   
Cut and paste the `export KUBECONFIG` command to set up the kubectl CLI to talk to the Kubernetes instance.
   
### Bind Bluemix services to the Kubernetes cluster
   
Bind the Compose for MySQL service to the `default` namespace in the Kubernetes cluster:
   
```
# bx cs cluster-service-bind <cluster_name> default <Compose-for-MySQL service>
```
   
This should create a Kubernetes secret instance that is usually named `binding-<Compose-for-MySQL service>` (e.g. `binding-refarch-mysql`).
   
```
# kubectl get secrets
```
   
Do the same for the MessageHub service:
   
```
# bx cs cluster-service-bind <cluster_name> default <MessageHub service>
```
   
### Create a secret for the HS256 shared key

Create a secret for the HS256 shared key in the Kubernetes cluster.
   
```
# kubectl create secret generic hs256-key --from-literal=key=<HS256-key>
```
   
### Update the deployment yaml for the Customer microservice:
   
Open and editor and update the yaml:
   
```
# vi kubernetes/orders.yaml
```
   
1. Update the the path under `spec.template.spec.containers[0].image` to correspond to the image pushed to the registry (in step 3).
2. Update the secret name under `spec.template.spec.volumes.name.secret[0].secretName` to correspond to the name of the Kubernetes secret for the Compose for MySQL binding (e.g. `binding-refarch-compose-mysql`)
3. Update the secret name under `spec.template.spec.volumes.name.secret[1].secretName` to correspond to the name of the Kubernetes secret for the MessageHub binding (e.g. `binding-refarch-messagehub`)
4. Update the secret name under `spec.template.spec.volumes.name.secret[2].secretName` to correspond to the name of the Kubernetes secret for the HS256 shared secret (e.g. `hs256-key` by default).
   
Here is an example of what the updated deployment may look like:
   
```
---
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: orders-microservice
spec:
  replicas: 2
  template:
    metadata:
      labels:
        app: bluecompute
        tier: backend
        micro: orders
    spec:
      containers:
      - name: orders-service
        image: registry.ng.bluemix.net/chrisking/us-micro-orders:jkwong-dev
        imagePullPolicy: Always
        volumeMounts:
        - mountPath: /var/run/secrets/binding-refarch-compose-mysql
          name: binding-refarch-compose-mysql
        - mountPath: /var/run/secrets/binding-refarch-messagehub
          name: binding-refarch-messagehub
        - mountPath: /var/run/secrets/hs256-key
          name: hs256-key
        ports:
        - containerPort: 8080
      volumes:
      - name: binding-refarch-compose-mysql
        secret:
          defaultMode: 420
          secretName: binding-refarch-compose-mysql
      - name: binding-refarch-messagehub
        secret:
          defaultMode: 420
          secretName: binding-refarch-messagehub
      - name: hs256-key
        secret:
          defaultMode: 420
          secretName: hs256-key
```
      
### Create the deployment

Deploy the pods.
   
```
# kubectl create -f kubernetes/customer.yaml
```

Also deploy the service
   
```
# kubectl create -f kubernetes/customer-service.yaml
```

