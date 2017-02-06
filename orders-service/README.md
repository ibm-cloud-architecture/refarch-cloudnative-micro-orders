## Deploy Sample - Orders Microservice Cloud Foundry Application
Instructions to deploy Orders Microservice sample hybrid application in your Bluemix Org that will connect to a MySQL Database server in SoftLayer using the Secure Gateway service.

Use values from table below for _bluemix-api-endpoint_ and _bluemix-domain_.

| Region | bluemix-api-endpoint | bluemix-domain |
| --- | --- | --- |
| US South | api\.ng\.bluemix\.net | api\.ng\.bluemix\.net | mybluemix\.net |
| London  | api\.eu-gb\.bluemix\.net | eu-gb\.mybluemix\.net |
| Sydney  | api\.au-syd\.bluemix\.net | au-syd\.mybluemix\.net |

#### Pre-Requisites
* Bluemix Account to deploy the Microservice. [Sign-up here](https://new-console.ng.bluemix.net/registration/?Target=https%3A%2F%2Fnew-console.ng.bluemix.net%2Flogin%3Fstate%3D%2Fhome%2Fonboard).
* You should have a SoftLayer Account. Two Virtual Server Instances (VSI) will be ordered using this account.
* Active SoftLayer user with VPN access enabled in your SoftLayer Account.
* Bluemix CLI Cloud Foundry (`cf`) tool should be installed. [Click for instructions](https://new-console.ng.bluemix.net/docs/cli/index.html#cli).

#### Setup MySQL Database Server in SoftLayer
1. Log into [SoftLayer Portal](https://control.softlayer.com).

2. Place SoftLayer Order for an [Hourly](https://www.softlayer.com/Store/orderHourlyComputingInstance/1640,1644,2202)/[Monthly](https://www.softlayer.com/Store/orderComputingInstance/1640,1644,2202) VSI using following options. Default selections can be made for other options.
  - _Operating System:_ __CentOS 7.x - Minimal Install (64 bit)__
  - _Uplink Port Speeds:_ __Private Only__
  - _Database Software:_ __MySQL for Linux__

3. After the CentOS VSI VSI is successfully provisioned, note down the private ip-address and password for user-id __root__.

4. Connect to SoftLayer VPN, and ssh to the VSI as __root__.

5. Connect to MySQL (MariaDB) server CLI by typing `mysql` at Linux command prompt. Instructions here are limited to creating a database and a user to access the database, refer to MySQL documentation to setup a secure production grade MySQL database server.

6. Create __orders__ database and __orders_dbuser__ to use the database.
    ```
    MariaDB [(none)]> CREATE DATABASE orders;
    MariaDB [(none)]> GRANT ALL PRIVILEGES ON orders.* TO 'orders_dbuser'@'%' IDENTIFIED BY 'Pass4OrdersUs3r';
    MariaDB [(none)]> FLUSH PRIVILEGES;
    MariaDB [(none)]> quit;
    ```

7. Note down the database server's private IP address, database name (__orders__), database user (__orders\_dbuser__) and password (__Pass4OrdersUs3r__). These will be used to create a Bluemix user provided service to connect to this database.

#### Connect to Bluemix Org using CLI
1. Log in to your Bluemix account.
    ```
    cf login -a <bluemix-api-endpoint> -u <your-bluemix-user-id>
    ```

2. Set target to use your Bluemix Org and Space.
    ```
    cf target -o <your-bluemix-org> -s <your-bluemix-space>
    ```

#### Setup Secure Gateway Service Instance in Bluemix
1. Create Secure Gateway service instance in your Bluemix Space, this service will be used to establish connectivity between the Orders Microservice in Bluemix and Database server in SoftLayer.
    ```
    cf create-service SecureGateway securegatewayplan My-SecureGateway
    ```
    
2. Log into [Bluemix Dashboard](https://new-console.ng.bluemix.net/#all-items). Click on your user avatar icon located at the top right corner of the dashboard and set the _Region_, _Organization_, and _Space_ used for this deployment. From list of services double-click on __My\-SecureGateway__ service to launch the _Secure Gateway Dashboard_.

3. In the _Secure Gateway Dashboard_, click _Add Gateway_. The Add Gateway page is displayed. Enter __Connect SoftLayer Devices__ in _Gateway Name_ input field.

4. For this sample application clear the checkbox for __Require security token to connect clients__. If selected, this means that you will need to enter a security token each time you start the Secure Gateway client.

5. For this sample application clear the checkbox for __Token Expiration:__. This option lets you set the token expiration time. When the security token expires, it is not automatically regenerated. You need to regenerate the token to receive a new one.

6. Click __ADD GATEWAY__ to add the gateway. It should now be displayed on the _Secure Gateway Dashboard_.

7. On the _Secure Gateway Dashboard_ double-click on __Connect SoftLayer Devices__ gateway. Notice this gateway is in __Disconnected__ state. Click on icon to _Copy Gateway ID_ and paste it to a text file. This string will be used later by the Secure Gateway client to connect to this gateway.

8. Click on __Add Destination__ to create connection to the MySQL database server in SoftLayer. _Add Destination_ wizard will launch, click on __Advanced Setup__. On the _Next_ screen enter following values and click __ADD DESTINATION__.
  - Select __On-Premises Destination__
  - _Destination Name:_ __SoftLayer MySQL Server__
  - _Resource Hostname:_ __Private IP Address of MySQL Database server__
  - _Resource Port:_ __3306__
  - _Protocol_: __TCP__

9. __SoftLayer MySQL Server__ is now added as a Destination with state _Enabled_. Click on the _Settings (gear-wheel icon)_ to display the connection details. Note down __Cloud Host : Port__ values and close the details dialog.

9. Create Bluemix service (__mysql-OrdersDBService__) to connect to MySQL database server running in SoftLayer. Use following values;
    ```
    cf create-user-provided-service mysql-OrdersDBService -p "hostname, port, user, password, jdbcUrl, uri"
    ```
  - _hostname:_ __Cloud Host name from step 9__
  - _port:_ __Port number from step 9__
  - _user:_ __orders_dbuser__
  - _password:_ __Pass4OrdersUs3r__
  - _jdbcUrl:_ __jdbc:mysql://\<Value of Cloud Host:Port from step 9\>/orders__
  - _uri:_ __mysql://\<Value of Cloud Host:Port from step 9\>/orders__


#### Setup SecureGateway Client Container in SoftLayer
1. Log into [SoftLayer Portal](https://control.softlayer.com).

2. Place SoftLayer Order for an [Hourly](https://www.softlayer.com/Store/orderHourlyComputingInstance/1640,1644,2202)/[Monthly](https://www.softlayer.com/Store/orderComputingInstance/1640,1644,2202) VSI running CoreOS stable. This will be used as Docker host to run Secure Gateway client container. Select __CoreOS Stable__ for __Operating System__, default selections can be accepted for all the other options.

2. After the CoreOS VSI is successfully provisioned, note down the private ip-address and password for user-id __core__.

3. Connect to SoftLayer VPN, and ssh to CoreOS VSI as __core__.

4. Pull the Secure Gateway Client image from IBM.
    ```
    sudo docker pull ibmcom/secure-gateway-client
    ```

5. Run the Secure Gateway container to connect to the Secure Gateway service instance in Bluemix. Also, allow incoming connections to port 3306 (MySQL Database Server port). Use the copied Gateway-ID of __Connect SoftLayer Devices__ gateway in Bluemix.
    ```
    sudo docker run -itd ibmcom/secure-gateway-client <Gateway-ID> --service -A "acl allow :3306"
    ```
6. Go to [Bluemix Dashboard](https://new-console.ng.bluemix.net/#all-items). From list of services double-click on __My\-SecureGateway__ service to launch the _Secure Gateway Dashboard_.

7. On the _Secure Gateway Dashboard_ double-click on __Connect SoftLayer Devices__ gateway, it should now be in __Connected__ state.

#### Deploy Orders Microservice in Bluemix
1. Clone repository to your local machine.
    ```
    cd <your-working-dir>
    git clone https://github.com/ibm-cloud-architecture/hybrid.git
    ```

2. Deploy the Orders Microservice application.
    ```
    cd hybrid/onprem-connectivity/orders-microservice-app
    cf push -d <bluemix-domain> --no-start
    ```
3. Go to [Bluemix Dashboard](https://new-console.ng.bluemix.net/#all-items). From the list of _Cloud Foundry Applications_ locate __Hybrid-Orders-Microservice__, it should be in _Stopped_ state.

4. Bind MySQL database service to Orders Microservice application.
    ```
    cf bind-service mysql-OrdersDBService Hybrid-Orders-Microservice
    ```

6. Start Orders Microservice.
    ```
    cf start Hybrid-Orders-Microservice
    ```

7. Get the URL to access the application and confirm the application is now running.
    ```
    cf app Hybrid-Orders-Microservice | grep 'state\|url'
    ```

8. Copy the application URL and launch it in a web browser, if the web page loads successfully then the Orders Microservice application deployed successfully. Click on the GET uri to see any existing orders in the __orders__ database hosted in SoftLayer. To create new orders send a POST request using cURL command with a JSON string containing sample order information.

This completes the deployment of a Hybrid Microservice sample application running in Bluemix connected a MySQL database server running in SoftLayer.
