###### refarch-cloudnative-micro-orders

## Microprofile based Microservice Apps Integration with MySQL Database Server

This repository contains the **MicroProfile** implementation of the **Orders Service** which is a part of the 'IBM Cloud Native Reference Architecture' suite, available at https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes

<p align="center">
  <a href="https://microprofile.io/">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-wfd/blob/microprofile/static/imgs/microprofile_small.png" width="300" height="100">
  </a>
</p>

1. [Introduction](#introduction)
2. [How it works](#how-it-works)
3. [API Endpoints](#api-endpoints)
4. [Implementation](#implementation)
    1. [Liberty app accelerator](#liberty-app-accelerator)
    2. [Microprofile](#microprofile)
5. [Features and App details](#features)
6. [Building the app](#building-the-app)
7. [Running the app and stopping it](#running-the-app-and-stopping-it)
    1. [Pre-requisites](#pre-requisites)
    2. [Locally in JVM](#locally-in-jvm)
    3. [Locally in Containers](#locally-in-containers)
    4. [Locally in Minikube](#locally-in-minikube)
    5. [Remotely in ICP](#remotely-in-icp)
8. [DevOps Strategy](#devops-strategy)
9. [References](#references)

### Introduction

This project is built to demonstrate how to build Orders Microservices applications using Microprofile. This application provides basic operations of saving and querying orders from a relational database as part of the Orders function of BlueCompute.

- Based on [MicroProfile](https://microprofile.io/).
- Persist order data to a MySQL database.
- OAuth 2.0 protected APIs
- Devops - TBD
- Deployment options for local, Docker Container-based runtimes, Minikube environment and ICP/BMX.

### How it works

Orders Microservice serves 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes. Though it is a part of a bigger application, Order service is itself an application in turn that persists the data of orders to a MYSQL database.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/orders_microservice.png">
</p>

### API Endpoints

The Orders Microservice REST APIs are protected by OpenID Connect. These APIs identifies and validates the caller using mp-jwt tokens.

```
GET     /orders/rest/orders 
```

- Returns all orders. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim. A JSON object array is returned consisting of only orders created by the customer ID

```
GET     /orders/rest/orders/{id}  
```

- Return order by ID. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim. If the `id` of the order is owned by the customer passed in the `IBM-App-User` header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

```
POST   /orders/rest/orders
```

- Create an order. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the customer ID of the caller encoded in the user_name claim. The Order object must be passed as JSON object in the request body with the following format:

```
{
  "itemId": <item id>,
  "count": <number of items in order>,
}
```

On success, `HTTP 201` is returned with the ID of the created order in the `Location` response header.

You can use cURL or Chrome POSTMAN to send get/post/put/delete requests to the application.

### Implementation

#### [Liberty app accelerator](https://liberty-app-accelerator.wasdev.developer.ibm.com/start/)

For Liberty, there is nice tool called [Liberty Accelerator](https://liberty-app-accelerator.wasdev.developer.ibm.com/start/) that generates a simple project based upon your configuration. Using this, you can build and deploy to Liberty either using the Maven or Gradle build.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/LibertyAcc_Home.png">
</p>

Just check the options of your choice and click Generate project. You can either Download it as a zip or you can create git project.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/LibertyAcc_PrjGen.png">
</p>

Once you are done with this, you will have a sample microprofile based application that you can deploy on Liberty.

Using Liberty Accelerator is your choice. You can also create the entire project manually, but using Liberty Accelerator will make things easier.

#### [MicroProfile](https://microprofile.io/)

MicroProfile is an open platform that optimizes the Enterprise Java for microservices architecture. In this application, we are using [**MicroProfile 1.2**](https://github.com/eclipse/microprofile-bom). This includes

- MicroProfile 1.0 ([JAX-RS 2.0](https://jcp.org/en/jsr/detail?id=339), [CDI 1.2](https://jcp.org/en/jsr/detail?id=346), and [JSON-P 1.0](https://jcp.org/en/jsr/detail?id=353))
- MicroProfile 1.1 (MicroProfile 1.0, [MicroProfile Config 1.0.](https://github.com/eclipse/microprofile-config))
- [MicroProfile Config 1.1](https://github.com/eclipse/microprofile-config) (supercedes MicroProfile Config 1.0), [MicroProfile Fault Tolerance 1.0](https://github.com/eclipse/microprofile-fault-tolerance), [MicroProfile Health Check 1.0](https://github.com/eclipse/microprofile-health), [MicroProfile Metrics 1.0](https://github.com/eclipse/microprofile-metrics), [MicroProfile JWT Authentication 1.0](https://github.com/eclipse/microprofile-jwt-auth).

You can make use of this feature by including this dependency in Maven.

```
<dependency>
<groupId>org.eclipse.microprofile</groupId>
<artifactId>microprofile</artifactId>
<version>1.2</version>
<type>pom</type>
<scope>provided</scope>
</dependency>
```

You should also include a feature in [server.xml](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/blob/microprofile/src/main/liberty/config/server.xml).

```
<server description="Sample Liberty server">

  <featureManager>
      <feature>microprofile-1.2</feature>
  </featureManager>

  <httpEndpoint httpPort="${default.http.port}" httpsPort="${default.https.port}"
      id="defaultHttpEndpoint" host="*" />

</server>
```
### Features

1. Java SE 8 - Used Java Programming language

2. CDI 1.2 - Used CDI for typesafe dependency injection

3. JAX-RS 2.0.1 - JAX-RS is used for providing both standard client and server APIs for RESTful communication by MicroProfile applications.

4. Eclipse MicroProfile Config 1.1 - Configuration data comes from different sources like system properties, system environment variables, .properties etc. These values may change dynamically. Using this feature, helps us to pick up configured values immediately after they got changed.

The config values are sorted according to their ordinal. We can override the lower importance values from outside. The config sources by default, below is the order of importance.

- System.getProperties()
- System.getenv()
- all META-INF/microprofile-config.properties files on the ClassPath.

In our sample application, we obtained the configuration programatically.

5. MicroProfile JWT Authentication 1.0 - Used Microprofile JWT Authentication for token based authentication. It uses OpenIDConnect based JSON Web Tokens (JWT) for role based access control of rest endpoints. This allows the system to verify, authorize and authenticate the user based the security token.

### Building the app

To build the application, we used maven build. Maven is a project management tool that is based on the Project Object Model (POM). Typically, people use Maven for project builds, dependencies, and documentation. Maven simplifies the project build. In this task, you use Maven to build the project.

1. Clone this repository.

   `git clone https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders.git`
   
2. `cd refarch-cloudnative-micro-orders/`

3. Checkout MicroProfile branch.

   `git checkout microprofile`

4. Run this command. This command builds the project and installs it.

   `mvn install`
   
   If this runs successfully, you will be able to see the below messages.
   
```
[INFO] --- maven-failsafe-plugin:2.18.1:verify (verify-results) @ orders ---
[INFO] Failsafe report directory: /Users/user@ibm.com/BlueCompute/refarch-cloudnative-micro-orders/target/test-reports/it
[INFO] 
[INFO] --- maven-install-plugin:2.4:install (default-install) @ orders ---
[INFO] Installing /Users/user@ibm.com/BlueCompute/refarch-cloudnative-micro-orders/target/orders-1.0-SNAPSHOT.war to /Users/user@ibm.com/.m2/repository/projects/orders/1.0-SNAPSHOT/orders-1.0-SNAPSHOT.war
[INFO] Installing /Users/user@ibm.com/BlueCompute/refarch-cloudnative-micro-orders/pom.xml to /Users/user@ibm.com/.m2/repository/projects/orders/1.0-SNAPSHOT/orders-1.0-SNAPSHOT.pom
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 45.990 s
[INFO] Finished at: 2018-02-26T13:50:46-05:00
[INFO] Final Memory: 21M/250M
[INFO] ------------------------------------------------------------------------
```

### Running the app and stopping it

### Pre-requisites

**Set Up MYSQL on IBM Cloud**

1. [Provision](https://console.ng.bluemix.net/catalog/services/compose-for-mysql) and instance of MySQL into your Bluemix space.
    - Select name for your instance.
    - Click the `Create` button.
    
2. Refresh the page until you see `Status: Ready`.

3. Now obtain `MySQL` service credentials.
    - Click on `Service Credentials` tab.
    - Then click on the `View Credentials` dropdown next to the credentials.
    
4. See the `uri` field, which has the format `mysql://user:password@host:port/database`, and extract the following:
    - **user:** MySQL user.
    - **password:** MySQL password.
    - **host**: MySQL host.
    - **port:** MySQL port.
    - **database:** MySQL database.
    
5. Keep those credential handy for when deploying the Inventory service.

6. **Create `items` table and load sample data. You should see message _Data loaded to inventorydb.items._**

    ```
    # cd ..
    # cd mysql/scripts
    # bash create_orders_db.sh {USER} {PASSWORD} {HOST} {PORT}
    ```
    
    - Replace `{USER}` with MySQL user.
    - Replace `{PASSWORD}` with MySQL password.
    - Replace `{HOST}` with MySQL host.
    - Replace `{PORT}` with MySQL port.

MySQL database is now setup in Compose.

In this case, your env variables will be 

```
export jdbcURL=jdbc:mysql://{HOST}:{PORT}/ordersdb?useSSL=false
export dbuser={USER}
export dbpassword={PASSWORD}
```

**Set Up MYSQL on Docker locally**

```
    # cd ..
    # cd mysql
```

1. Build the docker image

`docker build -t mysql .`

2. Run the container.

`docker run -p 9041:3306 -d --name mysql -e MYSQL_ROOT_PASSWORD=password mysql`

3. Create `items` table and load sample data

`docker exec mysql ./create_orders_db.sh root password 0.0.0.0 3306`

In this case, your jdbcURL will be 

```
export jdbcURL=jdbc:mysql://localhost:9041/ordersdb?useSSL=false
export dbuser=root
export dbpassword=password
```

**Set up RabbitMQ on Docker locally**

1. Build the docker image

`docker pull rabbitmq`

2. Run the container.

`docker run -d -p 5672:5672 -p 15672:15672  --name rabbitmq rabbitmq`

*As Orders service needs an Oauth token, make sure the auth service is up and running before running the Orders.*

#### Locally in JVM

To run the Orders microservice locally in JVM, please complete the [Building the app](#building-the-app) section.

#### Locally in Containers

To run Catalog microservice locally in container, you need [Docker](https://www.docker.com/) to be locally present in your system.

### Locally in JVM

1. Make sure the [Auth Service](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth/tree/microprofile) is up and running. Also, make sure you copied your SSL certificate in a file as mentioned [here]()

Since we are using default keystore in our server, we need to get the key from the keystore of the OpenID Provider and put it in the truststore of the service.

Use the below lines to copy the SSL certificate from the Authentication server to the service.

```
cd target/liberty/wlp/usr/servers/defaultServer/resources/security

keytool -importcert -keystore key.jks -storepass keypass -alias libertyop -file /Users/user@ibm.com/BlueCompute/refarch-cloudnative-auth/target/liberty/wlp/usr/servers/defaultServer/resources/security/libertyOP.cer -noprompt

cd /Users/user@ibm.com/BlueCompute/refarch-cloudnative-micro-orders

```

2. Set the JDBC URL before you start your application. The host and port depends on the service you use. You can run the MYSQL server locally on your system using the MYSQL docker container or use the [MYSQL Compose](https://www.ibm.com/cloud/compose/mysql) available in [IBM Cloud](https://www.ibm.com/cloud/).

   ```
   export jdbcURL=jdbc:mysql://<Your host>:<Port>/ordersdb?useSSL=false
   export dbuser=<DB_USER_NAME>
   export dbpassword=<PASSWORD>
   ``` 
3. Start your server.

   `mvn liberty:start-server -DtestServerHttpPort=9084 -DtestServerHttpsPort=8443`

   You will see the below.
   
```
[INFO] Starting server defaultServer.
[INFO] Server defaultServer started with process ID 13979.
[INFO] Waiting up to 30 seconds for server confirmation:  CWWKF0011I to be found in /Users/user@ibm.com/BlueCompute/refarch-cloudnative-micro-orders/target/liberty/wlp/usr/servers/defaultServer/logs/messages.log
[INFO] CWWKM2010I: Searching for CWWKF0011I in /Users/user@ibm.com/BlueCompute/refarch-cloudnative-micro-orders/target/liberty/wlp/usr/servers/defaultServer/logs/messages.log. This search will timeout after 30 seconds.
[INFO] CWWKM2015I: Match number: 1 is [26/2/18 14:30:08:305 EST] 00000019 com.ibm.ws.kernel.feature.internal.FeatureManager            A CWWKF0011I: The server defaultServer is ready to run a smarter planet..
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 11.984 s
[INFO] Finished at: 2018-02-26T14:30:08-05:00
[INFO] Final Memory: 17M/303M
[INFO] ------------------------------------------------------------------------
```
4. Validate the order in the following way.

Before validating the Order service, grab the access token from the [Auth Service](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth/tree/microprofile). Sample of an access token generated from the [Auth Service](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth/tree/microprofile) is as follows.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/accesstoken.png">
</p>

You can validate the order service as below

**Get all orders**

Run the following to retrieve all orders for the foo customer ID. Be sure to use the JWT retrieved from the previous step in place of <access token>

```
curl -k -H "Content-Type: application/json" -H "Authorization: Bearer <access token>" https://localhost:8443/orders/rest/orders
```

You will see something like below

```
[{"id":"1f9a4904-9d98-48cf-9508-6450a536e6b9","date":1521227554000,"itemId":13406,"customerId":"foo","count":1},{"id":"3cad602b-37b2-4d3e-a923-dcf33d927018","date":1521162698000,"itemId":13406,"customerId":"foo","count":1},{"id":"46b43aef-9d41-46fb-b140-c60f70e7f5c5","date":1521165435000,"itemId":13410,"customerId":"foo","count":4},{"id":"48efa02c-33e9-4924-91ba-f073ecff2430","date":1521165207000,"itemId":13411,"customerId":"foo","count":3}]
```

**Create an order**

Run the following to create an order for the foo customer ID. Be sure to use the JWT retrieved from the previous step in place of <access token>.

```
curl -k -H "Content-Type: application/json" -H "Authorization: Bearer <access token>" -X POST -d '{"itemId":13405, "count":4}' https://localhost:8443/orders/rest/orders
```

5. If you are done accessing the application, you can stop your server using the following command.

   `mvn liberty:stop-server -DtestServerHttpPort=9084 -DtestServerHttpsPort=8443`

Once you do this, you see the below messages.

```
[INFO] CWWKM2001I: Invoke command is [/Users/Hemankita.Perabathini@ibm.com/BlueCompute/refarch-cloudnative-micro-orders/target/liberty/wlp/bin/server, stop, defaultServer].
[INFO] objc[14023]: Class JavaLaunchHelper is implemented in both /Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/bin/java (0x10c4e74c0) and /Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/libinstrument.dylib (0x10c5e14e0). One of the two will be used. Which one is undefined.
[INFO] Stopping server defaultServer.
[INFO] Server defaultServer stopped.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 1.268 s
[INFO] Finished at: 2018-02-26T14:35:04-05:00
[INFO] Final Memory: 13M/309M
[INFO] ------------------------------------------------------------------------
```

### Locally in Containers

To run the application in docker, we first need to define a Docker file.

#### Docker file

We are using Docker to containerize the application. With Docker, you can pack, ship, and run applications on a portable, lightweight container that can run anywhere virtually.

```
FROM websphere-liberty:microProfile

MAINTAINER IBM Java engineering at IBM Cloud

COPY /target/liberty/wlp/usr/servers/defaultServer /config/
COPY target/liberty/wlp/usr/shared /opt/ibm/wlp/usr/shared/

# Install required features if not present
RUN installUtility install --acceptLicense defaultServer

CMD ["/opt/ibm/wlp/bin/server", "run", "defaultServer"]

# Upgrade to production license if URL to JAR provided
ARG LICENSE_JAR_URL
RUN \
  if [ $LICENSE_JAR_URL ]; then \
    wget $LICENSE_JAR_URL -O /tmp/license.jar \
    && java -jar /tmp/license.jar -acceptLicense /opt/ibm \
    && rm /tmp/license.jar; \
  fi
```

- The `FROM` instruction sets the base image. You're setting the base image to `websphere-liberty:microProfile`.
- The `MAINTAINER` instruction sets the Author field. Here it is `IBM Java engineering at IBM Cloud`.
- The `COPY` instruction copies directories and files from a specified source to a destination in the container file system.
  - You're copying the `/target/liberty/wlp/usr/servers/defaultServer` to the `config` directory in the container.
  - You're replacing the contents of `/opt/ibm/wlp/usr/shared/` with the contents of `target/liberty/wlp/usr/shared`.
- The `RUN` instruction runs the commands.
  - The instruction is a precondition to install all the utilities in the server.xml file. You can use the RUN command to install the utilities on the base image.
- The `CMD` instruction provides defaults for an executing container.

#### Running the application locally in a docker container

1. Build the docker image.

`docker build -t orders:microprofile .`

Once this is done, you will see something similar to the below messages.
```
Successfully built 7c05e5afbc40
Successfully tagged orders:microprofile
```
You can see the docker images by using this command.

`docker images`

```
REPOSITORY                                      TAG                 IMAGE ID            CREATED             SIZE
orders                                          microprofile        7c05e5afbc40        39 seconds ago      413MB
```
2. Run the docker image.

`docker run -d -p 9380:9080 -p 8443:9443 --name orders -t --link mysql:mysql --env jdbcURL=jdbc:mysql://mysql:3306/inventorydb?useSSL=false --env dbuser=root --env dbpassword=password orders:microprofile`

When it is done, you can verify it using the below command.

`docker ps`

You will see something like below.

```
CONTAINER ID        IMAGE                               COMMAND                  CREATED              STATUS              PORTS                                                                             NAMES
f7a89f9f33ac        orders:microprofile                 "/opt/ibm/wlp/bin/se…"   About a minute ago   Up About a minute   0.0.0.0:9380->9080/tcp, 0.0.0.0:8443->9443/tcp                                    order
002cb06e9076        auth:microprofile                   "/opt/ibm/docker/doc…"   8 minutes ago        Up 8 minutes        0.0.0.0:9580->9080/tcp, 0.0.0.0:7443->9443/tcp                                    auth
7f50df9b03a3        catalog:microprofile                "/opt/ibm/wlp/bin/se…"   11 minutes ago       Up 11 minutes       9443/tcp, 0.0.0.0:9280->9080/tcp                                                  catalog
e1fe5ab7cfbc        ibmcase/bluecompute-elasticsearch   "/run.sh"                16 minutes ago       Up 16 minutes       0.0.0.0:9200->9200/tcp, 9300/tcp                                                  elasticsearch
3149cb57629f        inventory:microprofile              "/opt/ibm/wlp/bin/se…"   17 minutes ago       Up 17 minutes       9443/tcp, 0.0.0.0:9180->9080/tcp                                                  inventory
526f5c1e6cb2        rabbitmq                            "docker-entrypoint.s…"   19 minutes ago       Up 19 minutes       4369/tcp, 0.0.0.0:5672->5672/tcp, 5671/tcp, 25672/tcp, 0.0.0.0:15672->15672/tcp   rabbitmq
b87156ca98e5        mysql                               "docker-entrypoint.s…"   19 minutes ago       Up 19 minutes       0.0.0.0:9041->3306/tcp                                                            mysql
```
3. Once you are done accessing the application, you can come out of the process. 

4. You can also remove the container if desired. This can be done in the following way.

`docker ps`

```
CONTAINER ID        IMAGE                        COMMAND                  CREATED             STATUS              PORTS                              NAMES
f7a89f9f33ac        orders:microprofile          "/opt/ibm/wlp/bin/se…"   About a minute ago   Up About a minute   0.0.0.0:9380->9080/tcp, 0.0.0.0:8443->9443/tcp 
```

Grab the container id.

- Do `docker stop <CONTAINER ID>`
In this case it will be, `docker stop f7a89f9f33ac`
- Do `docker rm <CONTAINER ID>`
In this case it will be, `docker rm f7a89f9f33ac`








