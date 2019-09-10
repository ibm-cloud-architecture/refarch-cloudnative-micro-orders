###### refarch-cloudnative-micro-orders

## Microprofile based Microservice Apps Integration with MySQL Database Server

This repository contains the **MicroProfile** implementation of the **Orders Service** which is a part of the 
'IBM Cloud Native Reference Architecture' suite, available at https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes

<p align="center">
  <a href="https://microprofile.io/">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-wfd/blob/microprofile/static/imgs/microprofile_small.png" width="300" height="100">
  </a>
</p>

* [Introduction](#introduction)
* [How it works](#how-it-works)
* [API Endpoints](#api-endpoints)
* [Implementation](#implementation)
    * [Microprofile](#microprofile)
* [Features and App details](#features)
* [Deploying the Bluecompute App](#deploying-the-bluecompute-app)
    + [IBM Cloud Private](#ibm-cloud-private)
    + [Minikube](#minikube)
* [Run Orders Service locally](#run-orders-service-locally)
* [References](#references)

### Introduction

This project is built to demonstrate how to build Orders Microservices applications using Microprofile. 
This application provides basic operations of saving and querying orders from a relational database as part of the Orders function of BlueCompute.

- Based on [MicroProfile](https://microprofile.io/).
- Persist order data to a MySQL database.
- Transmit stock to Inventory using RabbitMQ
- OAuth 2.0 protected APIs
- Deployment options for Minikube environment and ICP.

### How it works

The Orders MicroService serves 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes. Though it is a part of a bigger application, 
the Orders service is itself an application that persists the data of orders to a MYSQL database.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/orders_microservice.png">
</p>

### API Endpoints

The Orders MicroService REST APIs are protected by OpenID Connect. These APIs identifies and validates the caller using mp-jwt tokens.

```
GET     /orders/rest/orders 
```

- Returns all orders. The caller of this API must pass a valid OAuth token. 
The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim. 
A JSON object array is returned consisting of only orders created by the customer ID

```
GET     /orders/rest/orders/{id}  
```

- Return order by ID. The caller of this API must pass a valid OAuth token. 
The OAuth token is a JWT with the customer ID of the caller encoded in the `user_name` claim. 
If the `id` of the order is owned by the customer passed in the `IBM-App-User` header, 
it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

```
POST   /orders/rest/orders
```

- Create an order. The caller of this API must pass a valid OAuth token. 
The OAuth token is a JWT with the customer ID of the caller encoded in the user_name claim. 
The Order object must be passed as JSON object in the request body with the following format:

```
{
  "itemId": <item id>,
  "count": <number of items in order>,
}
```

On success, `HTTP 201` is returned with the ID of the created order in the `Location` response header.

### Implementation

#### [MicroProfile](https://microprofile.io/)

MicroProfile is an open platform that optimizes the Enterprise Java for microservices architecture. In this application, 
we use [**MicroProfile 1.3**](https://github.com/eclipse/microprofile-bom). This includes:

- MicroProfile 1.0 ([JAX-RS 2.0](https://jcp.org/en/jsr/detail?id=339), [CDI 1.2](https://jcp.org/en/jsr/detail?id=346), 
and [JSON-P 1.0](https://jcp.org/en/jsr/detail?id=353))
- MicroProfile 1.2 (MicroProfile 1.0,
[MicroProfile Fault Tolerance 1.0](https://github.com/eclipse/microprofile-fault-tolerance), 
[MicroProfile Health Check 1.0](https://github.com/eclipse/microprofile-health),  
[MicroProfile JWT Authentication 1.0](https://github.com/eclipse/microprofile-jwt-auth).)
- MicroProfile 1.3 (
[MicroProfile Open Tracing 1.0](https://github.com/eclipse/microprofile-opentracing),
[MicroProfile Open API 1.0](https://github.com/eclipse/microprofile-open-api),
[MicroProfile Metrics 1.1](https://github.com/eclipse/microprofile-metrics),
[MicroProfile Config 1.2](https://github.com/eclipse/microprofile-config))

You can make use of this feature by including this dependency in Maven.

```
<dependency>
    <groupId>org.eclipse.microprofile</groupId>
    <artifactId>microprofile</artifactId>
    <version>1.3</version>
    <type>pom</type>
    <scope>provided</scope>
</dependency>
```

You should also include a feature in [server.xml](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/blob/microprofile/src/main/liberty/config/server.xml).

```
<server description="Sample Liberty server">

  <featureManager>
      <feature>microprofile-1.3</feature>
  </featureManager>

  <httpEndpoint httpPort="${default.http.port}" httpsPort="${default.https.port}"
      id="defaultHttpEndpoint" host="*" />

</server>
```
### Features

1. Java SE 8 - Used Java Programming language

2. [CDI 1.2](https://jcp.org/en/jsr/detail?id=346) - Used CDI for typesafe dependency injection

3. [JAX-RS 2.0.1](https://jcp.org/en/jsr/detail?id=339) - 
JAX-RS is used for providing both standard client and server APIs for RESTful communication by the MicroProfile applications.

4. [Eclipse MicroProfile Config](https://github.com/eclipse/microprofile-config) - 
Configuration data comes from different sources like system properties, 
system environment variables, *.properties etc. These values may change dynamically. 
This feature enables us to pick up configured values immediately after they got changed.

    The config values are sorted according to their ordinal. We can override the less important values from outside. 
    The config sources three locations by default, and the list below shows their rank in priority from most to least:

    - System.getProperties()
    - System.getenv()
    - all META-INF/microprofile-config.properties files on the ClassPath.

    In our sample application, we obtained the configuration programmatically.

5. [MicroProfile JWT Authentication](https://github.com/eclipse/microprofile-jwt-auth) - 
MicroProfile JWT Authentication for token based authentication. 
It uses OpenIDConnect based JSON Web Tokens (JWT) for role based access control to our REST endpoints. 
This allows the system to verify, authorize and authenticate the user based the security token given.

6. [MicroProfile Health Check](https://github.com/eclipse/microprofile-open-api) - For MicroProfile implementations, 
this feature helps us determine the status of the service as well as its availability. 
This helps us to identify if the service is healthy or not. If the service is down, 
we can investigate the reasons behind its termination or shutdown. 

    In our sample application, we injected this `/health` endpoint in our liveness probes.

7. [MicroProfile OpenAPI](https://github.com/eclipse/microprofile-open-api) - 
This feature helps us to expose the API documentation for the RESTful services. 
It allows the developers to produce OpenAPI v3 documents for their JAX-RS applications.

    In our sample application we used @OpenAPIDefinition, @Info, @Contact, @License, @APIResponses, 
    @APIResponse, @Content, @Schema, @Operation and @Parameter annotations.

8. [MicroProfile OpenTracing](https://github.com/eclipse/microprofile-opentracing) - 
Enables and allows for custom tracing of JAX-RS and non-JAX-RS methods. It helps us 
to analyze the transaction flow so that the we can easily debug the problematic services and fix them.

    In our sample application, we used [Zipkin](https://zipkin.io/) as our distributed tracing system. We used @Traced 
    and an ActiveSpan object to retrieve messages.
    
9. [MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics) - 
Used to help monitor essential system parameters and the performance of well-known endpoints.
    
    In our sample application, we use @Timed, @Counted, @Metered.

## Deploying the Bluecompute App

To build and run the entire BlueCompute demo application, each MicroService must be spun up together. This is due to how we
set up our Helm charts structure and how we dynamically produce our endpoints and URLs.  

Further instructions are provided 
[here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/microprofile).

### IBM Cloud Private

To deploy it on IBM Cloud Private, please follow the instructions provided 
[here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/microprofile#remotely-on-ibm-cloud-private).

### Minikube

To deploy it on Minikube, please follow the instructions provided 
[here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/tree/microprofile#locally-in-minikube).

## Run Orders Service locally

To deploy the Orders app locally using Maven or Helm and test the individual service, please follow the instructions provided 
[here](etc/building-locally.md).


## References

1. [MicroProfile](https://microprofile.io/)
2. [MicroProfile Config on Liberty](https://www.ibm.com/support/knowledgecenter/en/SSAW57_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_microprofile_appconfig.html)
3. [MicroProfile Fault Tolerance on Liberty](https://www.ibm.com/support/knowledgecenter/en/was_beta_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_microprofile_fault_tolerance.html)
4. [MicroProfile Health Checks on Liberty](https://www.ibm.com/support/knowledgecenter/en/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_microprofile_healthcheck.html)
5. [MicroProfile Rest Client on Liberty](https://www.ibm.com/support/knowledgecenter/en/was_beta_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/twlp_mp_restclient.html)
4. [MicroProfile OpenAPI on Liberty](https://www.ibm.com/support/knowledgecenter/en/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/twlp_mpopenapi.html)
5. [MicroProfile Metrics on Liberty](https://www.ibm.com/support/knowledgecenter/en/SSEQTP_liberty/com.ibm.websphere.wlp.doc/ae/cwlp_mp_metrics_api.html)
6. [MicroProfile OpenTracing on Liberty](https://www.ibm.com/support/knowledgecenter/SSEQTP_liberty/com.ibm.websphere.liberty.autogen.base.doc/ae/rwlp_feature_mpOpenTracing-1.0.html)
