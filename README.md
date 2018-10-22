# refarch-cloudnative-micro-orders: Spring Boot MicroService with MySQL Database

[![Build Status](https://travis-ci.org/ibm-cloud-architecture/refarch-cloudnative-micro-orders.svg?branch=spring)](https://travis-ci.org/ibm-cloud-architecture/refarch-cloudnative-micro-orders)

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/spring*

## Table of Contents

  * [Introduction](#introduction)
  * [REST APIs](#rest-apis)
  * [Pre-requisites:](#pre-requisites)
  * [Deploy Orders Application to Kubernetes Cluster](#deploy-orders-application-to-kubernetes-cluster)
  * [Validate the Orders Microservice API](#validate-the-orders-microservice-api)
    * [Setup](#setup)
      * [a. Setup Orders Service Hostname and Port](#a-setup-orders-service-hostname-and-port)
      * [b. Create a temporary HS256 shared secret](#b-create-a-temporary-hs256-shared-secret)
      * [c. Generate a JWT Token with `blue` Scope](#c-generate-a-jwt-token-with-blue-scope)
    * [1. Create an Order](#1-create-an-order)
    * [2. Get all Orders](#2-get-all-orders)
  * [Deploy Orders Application on Docker](#deploy-orders-application-on-docker)
    * [Deploy the MySQL Docker Container](#deploy-the-mysql-docker-container)
    * [Deploy the Orders Docker Container](#deploy-the-orders-docker-container)
  * [Run Orders Service application on localhost](#run-orders-service-application-on-localhost)
  * [Deploy Orders Application on Open Liberty](#deploy-customer-application-on-openliberty)
  * [Optional: Setup CI/CD Pipeline](#optional-setup-cicd-pipeline)
  * [Conclusion](#conclusion)
  * [Contributing](#contributing)
    * [GOTCHAs](#gotchas)
    * [Contributing a New Chart Package to Microservices Reference Architecture Helm Repository](#contributing-a-new-chart-package-to-microservices-reference-architecture-helm-repository)

## Introduction

This project will demonstrate how to deploy a Spring Boot Application with a MySQL database onto a Kubernetes Cluster.

![Application Architecture](static/orders.png?raw=true)

Here is an overview of the project's features:

* Leverage [`Spring Boot`](https://projects.spring.io/spring*boot/) framework to build a MicroServices application.
* Uses [`Spring Data JPA`](http://projects.spring.io/spring*data-jpa/) to persist data to MySQL database.
* Uses [`MySQL`](https://www.mysql.com/) as the orders database.
* OAuth 2.0 protected APIs using Spring Security framework.
* Uses [`Docker`](https://docs.docker.com/) to package application binary and its dependencies.
* Uses [`Helm`](https://helm.sh/) to package application and MySQL deployment configuration and deploy to a [`Kubernetes`](https://kubernetes.io/) cluster. 
* When retrieving orders using the OAuth 2.0 protected APIs, return only orders belonging to the user identity encoded in the `user_name` claim in the JWT payload. 
  * See the [Authentication microservice](https://github.com/ibm*cloud-architecture/refarch-cloudnative-auth/tree/spring) for more details on how identity is propagated.

## REST APIs

The Orders MicroService REST API is OAuth 2.0 protected and identifies and validates the caller using signed JWT tokens.  

* `GET /micro/orders`
  * Returns all orders.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim.  A JSON object array is returned consisting of only orders created by the orders ID.

* `GET /micro/orders/{id}`
  * Return order by ID.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim.  If the `id` of the order is owned by the orders passed in the `IBM-App-User` header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

* `POST /micro/orders`
  * Create an order.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim.  The Order object must be passed as JSON object in the request body with the following format:
    ```json
    {
      "itemId": "item_id",
      "count": "number_of_items_in_order",
    }
    ```

    On success, `HTTP 201` is returned with the ID of the created order in the `Location` response header.

## Pre-requisites

* Create a Kubernetes Cluster by following the steps [here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes#create-a-kubernetes-cluster).
* Install the following CLI's on your laptop/workstation:
  * [`docker`](https://docs.docker.com/install/)
  * [`kubectl`](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
  * [`helm`](https://docs.helm.sh/using_helm/#installing-helm)
* Clone orders repository:

```bash
$ git clone -b spring --single-branch https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders.git
$ cd refarch-cloudnative-micro-orders
```

## Deploy Orders Application to Kubernetes Cluster

In this section, we are going to deploy the Orders Application, along with a MySQL service, to a Kubernetes cluster using Helm. To do so, follow the instructions below:

```bash
# Install MariaDB Chart
$ helm upgrade --install orders-mariadb \
  --version 4.4.2 \
  --set nameOverride=orders-mariadb \
  --set rootUser.password=admin123 \
  --set db.user=dbuser \
  --set db.password=password \
  --set db.name=ordersdb \
  --set replication.enabled=false \
  --set master.persistence.enabled=false \
  --set slave.replicas=1 \
  --set slave.persistence.enabled=false \
  stable/mariadb

# Go to Chart Directory
$ cd chart/orders

# Deploy Orders to Kubernetes cluster
$ helm upgrade --install orders --set service.type=NodePort .
```

The last command will give you instructions on how to access/test the Orders application. Please note that before the Orders application starts, the MySQL deployment must be fully up and running, which normally takes a couple of minutes. With Kubernetes [Init Containers](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/), the Orders Deployment polls for MySQL readiness status so that Orders can start once MySQL is ready, or error out if MySQL fails to start.

Also, once MySQL is fully up and running, a [`Kubernetes Job`](https://kubernetes.io/docs/concepts/workloads/controllers/jobs-run-to-completion/) will run to populate the MySQL database with the orders data so that it can be served by the application. This is done for convenience as the orders data is static.

To check and wait for the deployment status, you can run the following command:

```bash
$ kubectl get deployments -w
NAME                  DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
orders-orders   	    1         1         1            1           10h
```

The `-w` flag is so that the command above not only retrieves the deployment but also listens for changes. If you a 1 under the `CURRENT` column, that means that the orders app deployment is ready.

To validate that everything works, follow the instructions on [Validate the Orders Microservice API](#validate-the-orders-microservice-api) and make sure to use the values for `${NODE_IP}` and `${PORT}` from the chart install output in the `ORDERS_HOST` and `ORDERS_PORT` fields respectively.

## Validate the Orders Microservice API

Now that we have the orders service up and running, let's go ahead and test that the API works properly.

### Setup

#### a. Setup Orders Service Hostname and Port

To make going through this document easier, we recommend you create environment variables for the orders service hostname/IP and port. To do so, run the following commands:

```bash
# If using Kubernetes, use the values from the helm install output
$ export ORDERS_HOST=${NODE_IP}
$ export ORDERS_PORT=${PORT}

# If using Docker or running locally
$ export ORDERS_HOST=localhost
$ export ORDERS_PORT=8084
```

Where:

* `ORDERS_HOST` is the hostname or IP address for the orders service.
  * If using `IBM Cloud Private`, use the IP address of one of the proxy nodes.
  * If using `IBM Cloud Kubernetes Service`, use the IP address of one of the worker nodes.
* `ORDERS_PORT` is the port for the orders service.
  * If using `IBM Cloud Private` or `IBM Cloud Kubernetes Service`, enter the value of the NodePort.

#### b. Create a temporary HS256 shared secret

As the APIs in this microservice as OAuth protected, the HS256 shared secret used to sign the JWT generated by the [Authorization Server](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) is needed to validate the access token provided by the caller.

To make things easier for you, we pasted below the 2048-bit secret that's included in the orders chart [here](chart/orders/values.yaml#L53), which you can export to your environment as follows:
```bash
$ export HS256_KEY="E6526VJkKYhyTFRFMC0pTECpHcZ7TGcq8pKsVVgz9KtESVpheEO284qKzfzg8HpWNBPeHOxNGlyudUHi6i8tFQJXC8PiI48RUpMh23vPDLGD35pCM0417gf58z5xlmRNii56fwRCmIhhV7hDsm3KO2jRv4EBVz7HrYbzFeqI45CaStkMYNipzSm2duuer7zRdMjEKIdqsby0JfpQpykHmC5L6hxkX0BT7XWqztTr6xHCwqst26O0g8r7bXSYjp4a"
```

However, if you must create your own 2048-bit secret, one can be generated using the following command:

```bash
cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 256 | head -n 1 | xargs echo -n
```

Note that if the [Authorization Server](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth) is also deployed, it must use the *same* HS256 shared secret.

#### c. Generate a JWT Token with `blue` Scope

To generate a JWT Token with an `blue` scope, which will let you create/get/delete orders, run the commands below:

```bash
# JWT Header
jwt1=$(echo -n '{"alg":"HS256","typ":"JWT"}' | openssl enc -base64);
# JWT Payload
jwt2=$(echo -n "{\"scope\":[\"blue\"],\"user_name\":\"admin\"}" | openssl enc -base64);
# JWT Signature: Header and Payload
jwt3=$(echo -n "${jwt1}.${jwt2}" | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# JWT Signature: Create signed hash with secret key
jwt4=$(echo -n "${jwt3}" | openssl dgst -binary -sha256 -hmac "${HS256_KEY}" | openssl enc -base64 | tr '+\/' '-_' | tr -d '=' | tr -d '\r\n');
# Complete JWT
jwt=$(echo -n "${jwt3}.${jwt4}");
```

Where:

* `blue` is the scope needed to create the order.
* `${HS256_KEY}` is the 2048-bit secret from the previous step.

### 1. Create an Order

Run the following to create an order for the `admin` user.  Be sure to use the JWT retrieved from the previous step in place of `${jwt}`.

```bash
$ curl -i -H "Content-Type: application/json" -H "Authorization: Bearer ${jwt}" -X POST -d '{"itemId":13401, "count":1}' "http://${ORDERS_HOST}:${ORDERS_PORT}/micro/orders"

HTTP/1.1 201 Created
Date: Wed, 29 Aug 2018 15:08:32 GMT
X-Application-Context: orders-microservice:8084
Location: http://localhost:8084/micro/orders/4028e381658639fb0165863a9b140000
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
```

If you get `HTTP/1.1 201 Created`, as shown above, then the command ran successfully.

### 2. Get all Orders

Run the following to retrieve all orders for the `admin` customerId.  Be sure to use the JWT retrieved from the previous step in place of `${jwt}`.

```bash
$ curl -H "Authorization: Bearer ${jwt}" "http://${ORDERS_HOST}:${ORDERS_PORT}/micro/orders"

[{"id":"4028e381658639fb0165863a9b140000","date":1535555312000,"itemId":13401,"customerId":"admin","count":1}]
```

If you get a JSON table with an `itemId` of `13401` and `customerId` of `admin`, then the command ran successfully.

## Deploy Orders Application on Docker

You can also run the Orders Application locally on Docker. Before we show you how to do so, you will need to have a running MySQL deployment running somewhere. 

### Deploy the MySQL Docker Container

The easiest way to get MySQL running is via a Docker container. To do so, run the following commands:

```bash
# Start a MySQL Container with a database user, a password, and create a new database
$ docker run --name ordersmysql \
    -e MYSQL_ROOT_PASSWORD=admin123 \
    -e MYSQL_USER=dbuser \
    -e MYSQL_PASSWORD=password \
    -e MYSQL_DATABASE=ordersdb \
    -p 3307:3307 \
    -d mysql:5.7.14

# Get the MySQL Container's IP Address
$ docker inspect ordersmysql | grep "IPAddress"
            "SecondaryIPAddresses": null,
            "IPAddress": "172.17.0.2",
                    "IPAddress": "172.17.0.2",
```

Make sure to select the IP Address in the `IPAddress` field. You will use this IP address when deploying the Orders container.

### Deploy the Orders Docker Container

To deploy the Orders container, run the following commands:

```bash
# Build the Docker Image
$ docker build -t orders .

# Start the Orders Container
$ docker run --name orders \
    -e MYSQL_HOST=${MYSQL_IP_ADDRESS} \
    -e MYSQL_PORT=3307 \
    -e MYSQL_USER=dbuser \
    -e MYSQL_PASSWORD=password \
    -e MYSQL_DATABASE=ordersdb \
    -e HS256_KEY=${HS256_KEY} \
    -p 8084:8084 \
    -d orders
```

Where `${MYSQL_IP_ADDRESS}` is the IP address of the MySQL container, which is only accessible from the Docker container network.

To validate that everything works, follow the instructions on [Validate the Orders Microservice API](#validate-the-orders-microservice-api).

## Run Orders Service application on localhost

In this section you will run the Spring Boot application on your local workstation. Before we show you how to do so, you will need to deploy a MySQL Docker container and populate it with data as shown in the [Deploy the MySQL Docker Container](#deploy-the-mysql-docker-container) section.

Once MySQL is ready, we can run the Spring Boot Orders application locally as follows:

1. Build the application:

    ```bash
    ./gradlew build -x test
    ```

2. Run the application on localhost:

    ```bash
    $ java -Deureka.client.fetchRegistry=false \
        -Deureka.client.registerWithEureka=false \
        -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/ordersdb \
        -Dspring.datasource.username=dbuser \
        -Dspring.datasource.password=password \
        -Dspring.datasource.port=3306 \
        -Djwt.sharedSecret=${HS256_KEY} \
        -jar build/libs/micro-orders-0.0.1.jar
    ```

To validate that everything works, follow the instructions on [Validate the Orders Microservice API](#validate-the-orders-microservice-api).

## Deploy Orders Application on Open Liberty

The Spring Boot applications can be deployed on WebSphere Liberty as well. In this case, the embedded server i.e. the application server packaged up in the JAR file will be Liberty. For instructions on how to deploy the Orders application optimized for Docker on Open Liberty, which is the open source foundation for WebSphere Liberty, follow the instructions [here](OpenLiberty.MD).

## Optional: Setup CI/CD Pipeline

If you would like to setup an automated Jenkins CI/CD Pipeline for this repository, we provided a sample [Jenkinsfile](Jenkinsfile), which uses the [Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/) syntax of the [Jenkins Kubernetes Plugin](https://github.com/jenkinsci/kubernetes-plugin) to automatically create and run Jenkis Pipelines from your Kubernetes environment. 

To learn how to use this sample pipeline, follow the guide below and enter the corresponding values for your environment and for this repository:

* https://github.com/ibm-cloud-architecture/refarch-cloudnative-devops-kubernetes

## Conclusion

You have successfully deployed and tested the Orders Microservice and a MySQL database both on a Kubernetes Cluster and in local Docker Containers.

To see the Orders app working in a more complex microservices use case, checkout our Microservice Reference Architecture Application [here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/spring).

## Contributing

If you would like to contribute to this repository, please fork it, submit a PR, and assign as reviewers any of the GitHub users listed here:

* https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/graphs/contributors

### GOTCHAs

1. We use [Travis CI](https://travis-ci.org/) for our CI/CD needs, so when you open a Pull Request you will trigger a build in Travis CI, which needs to pass before we consider merging the PR. We use Travis CI to test the following:
    * Create a MySQL databasea.
    * Building and running the Orders app against the MySQL database and run API tests.
    * Build and Deploy a Docker Container, using the same MySQL database.
    * Run API tests against the Docker Container.
    * Deploy a minikube cluster to test Helm charts.
    * Download Helm Chart dependencies and package the Helm chart.
    * Deploy the Helm Chart into Minikube.
    * Run API tests against the Helm Chart.

2. We use the Community Chart for MySQL as the dependency chart for the Orders Chart. If you would like to learn more about that chart and submit issues/PRs, please check out its repo here:
    * https://github.com/helm/charts/tree/master/stable/mysql

### Contributing a New Chart Package to Microservices Reference Architecture Helm Repository

To contribute a new chart version to the [Microservices Reference Architecture](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/spring) helm repository, follow its guide here:

* https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/spring#contributing-a-new-chart-to-the-helm-repositories