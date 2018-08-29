# refarch-cloudnative-micro-orders: Spring Boot Microservice with MySQL Database

## Introduction
This chart will deploy a Spring Boot Application with a MySQL database onto a Kubernetes Cluster.

![Application Architecture](https://raw.githubusercontent.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/spring/static/orders.png?raw=true)

Here is an overview of the project's features:
- Leverage [`Spring Boot`](https://projects.spring.io/spring-boot/) framework to build a Microservices application.
- Uses [`Spring Data JPA`](http://projects.spring.io/spring-data-jpa/) to persist data to MySQL database.
- Uses [`MySQL`](https://www.mysql.com/) as the orders database.
- OAuth 2.0 protected APIs using Spring Security framework.
- Uses [`Docker`](https://docs.docker.com/) to package application binary and its dependencies.
- Uses [`Helm`](https://helm.sh/) to package application and MySQL deployment configuration and deploy to a [`Kubernetes`](https://kubernetes.io/) cluster. 
- When retrieving orders using the OAuth 2.0 protected APIs, return only orders belonging to the user identity encoded in the `user_name` claim in the JWT payload. 
  - See the [Authentication microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth/tree/spring) for more details on how identity is propagated.

## Chart Source
The source for the `Orders` chart can be found at:
* https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/tree/spring/chart/orders

The source for the `MySQL` chart can be found at:
* https://github.com/helm/charts/tree/master/stable/mysqlcouchdb

## REST APIs
The Orders Microservice REST API is OAuth 2.0 protected and identifies and validates the caller using signed JWT tokens.  

- `GET /micro/orders`
  - Returns all orders.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim.  A JSON object array is returned consisting of only orders created by the orders ID.

- `GET /micro/orders/{id}`
  - Return order by ID.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim.  If the `id` of the order is owned by the orders passed in the `IBM-App-User` header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

- `POST /micro/orders`
  - Create an order.  The caller of this API must pass a valid OAuth token.  The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim.  The Order object must be passed as JSON object in the request body with the following format:
    ```json
    {
      "itemId": "item_id",
      "count": "number_of_items_in_order",
    }
    ```

    On success, `HTTP 201` is returned with the ID of the created order in the `Location` response header.

## Deploy Orders Application to Kubernetes Cluster from CLI
To deploy the Orders Chart and its MySQL dependency Chart to a Kubernetes cluster using Helm CLI, follow the instructions below:
```bash
# Clone orders repository:
$ git clone -b spring --single-branch https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders.git

# Go to Chart Directory
$ cd refarch-cloudnative-micro-orders/chart/orders

# Download MySQL Dependency Chart
$ helm dependency update

# Deploy Orders and MySQL to Kubernetes cluster
$ helm upgrade --install orders --set service.type=NodePort .
```