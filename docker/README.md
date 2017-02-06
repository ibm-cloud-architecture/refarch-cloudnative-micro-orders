# Deploy Sample - Orders Microservice 

## Orders Microservice Container

The sample liberty server can be used to deploy the Orders Microservice into a local docker container or a docker container running on BlueMix.  The samples here assume that a VPN Gateway service has been configured in BlueMix that is used to create a tunnel to an on-premise MySQL server hosting an orders database.

### Create Docker Image 
- Clone git repository to your local machine. 
```
# cd <your-working-dir> 
# git clone https://github.com/ibm-solution-engineering/hybrid.git 
```

- Login to the Bluemix container service. 
```
# cf ic login 
```
 
- Find the private namespace of your Bluemix registry. 
```
# cf ic namespace get 
```
 
- Build the docker image

  This builds the image locally, but tags it with the private bluemix namespace, and gives it the desired image name <image name>.  Pass the credentials for the MySQL user on the commandline, which will update server.xml.  Most of the arguments will have defaults and do not need to be specified.

```
# cd hybrid/onprem-connectivity/orders-microservice-container
# docker build --build-arg db_ip=<private server IP> --build-arg db_user=<mysql db user> --build-arg db_passwd=<mysql db password> --build-arg db_name=<db name> --build-arg db_port=<db port> -t registry.ng.bluemix.net/<namespace>/<image_name> . 
```

 
- Once the image has been built, verify that the image is in the local docker repository: 
```
# docker images | grep <image_name> 
```

### Start Docker Container on local Docker Host

To verify that the container is working correctly, use the following command to start the docker container from the images just created in the local docker host.  This starts the container with 1GB memory and exposes port 9080 on the public IP of the docker host.

```
# docker run –p <public ip>:9080:9080 –m=1024M --name <container name> -d registry.ng.bluemix.net/<namespace>/<image_name> 
```

Once the container is running, verify that it's running using the following command:
```
# docker ps
```

Connect to the orders web app at the url, http://<public ip>:9080
 
### Publish Docker Image to Bluemix 
Push the container into bluemix using the following command:
```
# docker push registry.ng.bluemix.net/<namespace>/<image_name> 
```
 
To ensure that the container made it into Bluemix, use the following command to verify: 
```
# cf ic images | grep <image_name> 
```
 
### Start Docker Container in Bluemix 
Run the container in Bluemix using the following command, passing the desired container name <container name>.  This command exposes the ports defined in the liberty container (9080) with memory size 1024M, and executes the container in the background (-d): 
```
# cf ic run –P –m 1024 --name <container name> -d registry.ng.bluemix.net/<namespace>/<image_name> 
```

Verify that the container is running using the following command:
```
# cf ic ps | grep <container name>
```

Once the container is started, in the Bluemix portal, assign a public IP address to the container.  The web application should be accessible at the URL http://<Public IP>:9080. 


# Deploy Orders Microservice as a Cloud Foundry application

The web app can also be packaged as a Cloud Foundry application to be used in conjunction with the Secure Gateway service on BlueMix.

## Set up Secure Gateway

- Create Secure Gateway service instance in your Bluemix Space, this service will be used to establish connectivity between the Orders Microservice in Bluemix and Database server in SoftLayer. 
```
# cf create-service SecureGateway securegatewayplan My-SecureGateway 
```
 
- Log into Bluemix Dashboard. Click on your user avatar icon located at the top right corner of the dashboard and set the Region, Organization, and Space used for this deployment. From list of services double-click on My-SecureGateway service to launch the Secure Gateway Dashboard. 
 
- In the Secure Gateway Dashboard, click Add Gateway. The Add Gateway page is displayed. Enter Connect SoftLayer Devices in Gateway Name input field. 
 
  For this sample application clear the checkbox for Require security token to connect clients. If this option is left selected, it means that you will need to enter a security token each time you start the Secure Gateway client. 
 
  For this sample application clear the checkbox for Token Expiration:. If selected, this option lets you set the token expiration time. When the security token expires, it is not automatically regenerated. You need to regenerate the token to receive a new one. 
 
- Click ADD GATEWAY to add the gateway. The gateway named Connect SoftLayer Devices should now be displayed on the Secure Gateway Dashboard. 
 
- On the Secure Gateway Dashboard double-click on Connect SoftLayer Devices gateway. Notice this gateway is in Disconnected state. Click on icon to Copy Gateway ID and paste it to a text file. This ID will be used later by the Secure Gateway client to connect to this gateway service. 
 
- Click on Add Destination to create connection to the MySQL database server in SoftLayer. Add Destination wizard will launch, click on Advanced Setup. On the Next screen enter following values and then click ADD DESTINATION. 

Name | Value
--- | ---
Select Radio-button  | On-Premises Destination
Destination Name     | SoftLayer MySQL Server                      
Resource Hostname    | Private IP Address of MySQL Database server 
Resource Port        | 3306                                        
Protocol             | TCP                                         

  SoftLayer MySQL Server is now added as a Destination with state Enabled. Click on the Settings (gear-wheel icon) to display the connection details. Note down Cloud Host : Port values and close the details dialog. 
 
- Create Bluemix service (mysql-OrdersDBService) to connect to MySQL database server running in SoftLayer. Use following values. 
```
# cf create-user-provided-service mysql-OrdersDBService -p "hostname, port, user, password, jdbcUrl, uri" 
```

Name | Value
--- | ---
hostname | Cloud Host name from step 9 
port | Port number from step 9
user | orders_dbuser 
password | Pass4OrdersUs3r 
jdbcUrl  | jdbc:mysql://<hostname>:<port>/orders 
uri | mysql://<hostname>:<port>/orders

## Setup Secure Gateway Client in SoftLayer 
The Secure Gateway client can be installed in Windows, RedHat, SuSE, Ubuntu, and OS X, and also be run on IBM DataPower, and within Docker. This document will use a Docker Container. 
 
- Log into SoftLayer Portal. Place Order for an Hourly/Monthly VSI running CoreOS stable. This will be used as Docker host to run Secure Gateway client container. Select CoreOS Stable for Operating System, default selections can be accepted for all the other options. 
 
- After the CoreOS VSI is successfully provisioned, note down the private ip-address and password for user-id core. 
 
- Connect to SoftLayer VPN, and ssh to CoreOS VSI as core. Run the Secure Gateway client container using following commands. 

- Pull the secure gateway client image from IBM 
```
# sudo docker pull ibmcom/secure-gateway-client 
```
 
- Run the secure gateway docker container 
```
# sudo docker run -itd ibmcom/secure-gateway-client <gateway-id> --service -A "acl allow :3306" 
```
 
  Make sure that you substitute <gateway-id> above with the Gateway ID value from Step 7 in the previous section. 
 
- Go to Bluemix Dashboard. From list of services double-click on My-SecureGateway service to launch the Secure Gateway Dashboard. 
 
- On the Secure Gateway Dashboard double-click on Connect SoftLayer Devices gateway, it should now be in Connected state. 
 
## Deploy Orders Microservice to Bluemix connecting over Secure Gateway 

- Clone repository to your local machine. 
```
# cd <your-working-dir> 
# git clone https://github.com/ibm-solution-engineering/hybrid.git 
```
 
- Deploy the Orders Microservice application as a pre-packaged Liberty server, giving it a desired name <app-name>: 
```
# cd hybrid/onprem-connectivity/orders-microservice-container 
# zip –r hybrid-ordersapi-ng.zip wlp/ 
# cf push -d <bluemix-domain> <app-name> -p hybrid-ordersapi-ng.zip --no-start 
```
 
- Go to Bluemix Dashboard.. From the list of _Cloud Foundry Applications_ locate the app with <app-name>, it should be in Stopped state.  
 
- Bind MySQL database service to Orders Microservice application. 
```
# cf bind-service <app-name> mysql-OrdersDBService  
```
 
- Start Orders Microservice. 
```
# cf start <app-name> 
```
 
- Get the URL to access the application and confirm the application is now running. 
```
# cf app <app-name> | grep 'state\|url' 
```
 
- Copy the application URL and launch it in a web browser, if the web page loads successfully then the Orders Microservice deployed successfully.  
 
- Click on the GET uri to see any existing orders in the __orders__ database hosted in SoftLayer. To create a new order send a POST request using cURL command with JSON string containing the order information. 
 
