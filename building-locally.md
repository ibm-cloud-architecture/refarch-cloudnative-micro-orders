### Building the app

To build the application, we used Maven. Maven is a project management tool that is based on the Project Object Model (POM). 
Typically, people use Maven for project builds, dependencies, and documentation. Maven simplifies the project build. 
In this task, you use Maven to build the project.

1. Clone this respository and navigate into it

   ```
   $ git clone https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders.git
   $ cd refarch-cloudnative-micro-orders/
   ```

2. Checkout the MicroProfile branch.

   ```
   $ git checkout microprofile
   ```

3. Use Maven to build and install the project.

   ```
   $ mvn install
   ```
   Optionally, you may use `maven clean install` to ensure a clean working directory before you build.
   
   If this runs successfully, you will see a similar message to the one below. 
   
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

### Setting up MYSQL on Minikube or ICP

#### Pre-requisites

1. Set Up MYSQL on Minikube

    To set up MYSQL in a Kubernetes-based environment locally, we will use Minikube (which is meant to be a small development environment), 
    and a couple of tools:

    - [Kubectl](https://kubernetes.io/docs/user-guide/kubectl-overview/) (Kubernetes CLI) - 
    Follow the instructions [here](https://kubernetes.io/docs/tasks/tools/install-kubectl/) to install it onto your platform.
    - [Helm](https://github.com/kubernetes/helm) (Kubernetes package manager) - 
    Follow the instructions [here](https://github.com/kubernetes/helm/blob/master/docs/install.md) to install it onto your platform.
    - [Minikube](https://kubernetes.io/docs/getting-started-guides/minikube/) - 
    To create a single node virtual cluster on your workstation. 
    Follow the instructions [here](https://kubernetes.io/docs/tasks/tools/install-minikube/) to get Minikube installed onto your workstation.

    We recommend read [Running Kubernetes Locally via Minikube](https://kubernetes.io/docs/getting-started-guides/minikube/) additionally
    for more knowledge to familiarize yourself with with Minikube, Kubernetes, and Helm.

2. Set Up MYSQL on IBM Cloud Private

    To set up MYSQL on IBM Cloud Private, you will need to have:
    - [Helm](https://github.com/kubernetes/helm) (Kubernetes package manager) - Follow the instructions 
    [here](https://github.com/kubernetes/helm/blob/master/docs/install.md) to install it on your platform.
    If you're using IBM Cloud Private version 2.1.0.2 or newer, we recommend you follow 
    these [instructions](https://www.ibm.com/support/knowledgecenter/SSBS6K_2.1.0.2/app_center/create_helm_cli.html) to install helm.
    - An [IBM Cloud Private Cluster](https://www.ibm.com/cloud/private).
    Create a Kubernetes cluster in an on-premise data center. The community edition (IBM Cloud private-ce) is free.
    Follow the instructions [here](https://www.ibm.com/support/knowledgecenter/en/SSBS6K_2.1.0.2/installing/install_containers_CE.html) 
    to install IBM Cloud private-ce.

#### Run Minikube and start the container using Helm Charts

1. Start up minikube. Run the command below:

    ```
    $ minikube start
    ```

    You will see similar output to this:

    ```
    Setting up certs...
    Connecting to cluster...
    Setting up kubeconfig...
    Starting cluster components...
    Kubectl is now configured to use the cluster.
    ```
    
    Note, if running the application, it is recommended to provision 8GB over the default 2GB amount:
    ```
    $ minikube start --memory 8192
    ```

2. To install Tiller, which is a server side component of Helm, we must first initialize helm using:
    ```
    $ helm init
    ```
    
    You will see simialr output to this:
    ```
    $HELM_HOME has been configured at /Users/user@ibm.com/.helm.
    
    Tiller (the helm server side component) has been installed into your Kubernetes Cluster.
    Happy Helming!
    ```
    
3. Check if your tiller is available with:
    ```
    kubectl get deployment tiller-deploy --namespace kube-system
    ``` 
     
    You can verify its status with the output message.
    
    ```
    NAME            DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
    tiller-deploy   1         1         1            1           1m
    ```

4. Verify your helm before proceeding like below:
    ```
    helm version
    ```

    If your helm server version is below 2.5.0, please run command below:

    ```
    helm init --upgrade --tiller-image gcr.io/kubernetes-helm/tiller:v2.5.0
    ```

    Make sure your versions by testing the versions.
    You will see similar output.

    ```
    Client: &version.Version{SemVer:"v2.4.2", GitCommit:"82d8e9498d96535cc6787a6a9194a76161d29b4c", GitTreeState:"clean"}
    Server: &version.Version{SemVer:"v2.5.0", GitCommit:"012cb0ac1a1b2f888144ef5a67b8dab6c2d45be6", GitTreeState:"clean"}
    ```

5. Set the docker environment and build the docker image:

    Before we build the docker image, we must first set the docker environment so that it builds to Minikube's Docker.
    
    Run the following command:
    ```
    $ eval $(minikube docker-env)
    ```

    Now we can run docker build.

    ```
    cd mysql
    docker build -t ordersdb:v1.0.0 .
    ```

    A success will output a similar message:
    
    ```
    Successfully built 27e132d3c908
    Successfully tagged ordersdb:v1.0.0
    ```
    Then run `cd ..`

6. Run the helm chart as below:

    ```
    helm install --name=ordersdb chart/ordersdb
    ```
    
7. Make sure your deployment is ready. To verify run this command and you should see the availability:
    
    ```
    kubectl get deployments
    ```
    
    Yow will see a similar message:
    ```
    NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
    bluecompute-ordersdb                        1         1         1            1           5m
    ```

#### Set Up MYSQL on IBM Cloud Private

1. Your [IBM Cloud Private Cluster](https://www.ibm.com/cloud/private) should be up and running.

2. Log in to the IBM Cloud Private. 

    <p align="center">
        <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/icp_dashboard.png">
    </p>

3. Go to `admin > Configure Client`.

    <p align="center">
        <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/client_config.png">
    </p>

4. Grab the kubectl configuration commands.

    <p align="center">
        <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/kube_cmds.png">
    </p>

5. Run those commands in your terminal.

6. If successful, you should see something like below.
    ```
    Switched to context "xxx-cluster.icp-context".
    ```
    
7. Run the below command:
    ```
    $ helm init --client-only
    ```

    You will see a similar message to below:
    ```
    $HELM_HOME has been configured at /Users/user@ibm.com/.helm.
    Not installing Tiller due to 'client-only' flag having been set
    Happy Helming!
    ```

8. Verify the helm version:

    ```
    $ helm version --tls
    ```

    You will see a similar message to below:
    ```
    Client: &version.Version{SemVer:"v2.7.2+icp", GitCommit:"d41a5c2da480efc555ddca57d3972bcad3351801", GitTreeState:"dirty"}
    Server: &version.Version{SemVer:"v2.7.2+icp", GitCommit:"d41a5c2da480efc555ddca57d3972bcad3351801", GitTreeState:"dirty"}
    ```

9. Set the docker environment and build the docker image:

    Before we build the docker image, we must first set the docker environment so that it builds to Minikube's Docker.
    
    Run the following command:
    ```
    $ eval $(minikube docker-env)
    ```

    Now we can run docker build.

    ```
    cd mysql
    docker build -t ordersdb:v1.0.0 .
    ```

    A success will output a similar message:
    
    ```
    Successfully built 27e132d3c908
    Successfully tagged ordersdb:v1.0.0
    ```
    Then run `cd ..`

10. Run the helm chart as below:
    ```
    helm install --name=ordersdb chart/ordersdb --tls
    ```

11. Make sure your deployment is ready. To verify run this command and you should see the availability:
        
    ```
    kubectl get deployments
    ```
    
    Yow will see a similar message:
    ```
    NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
    bluecompute-ordersdb                        1         1         1            1           5m
    ```
           
    **NOTE**: If you are using a version of ICP older than 2.1.0.2, you don't need to add the --tls at the end of the helm command.


### Setting up RabbitMQ

The charts for RabbitMQ are included in the helm charts for Orders Service. Launching the helm charts for Orders Service also launches RabbitMQ.

Once the Orders service is deployed as [here](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/tree/microprofile#running-the-app-and-stopping-it), you can see the below.

`kubectl get deployments`

```
NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
rabbitmq-deployment                         1         1         1            1           9m
```

`kubectl get services`

```
NAME                       TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                          AGE
rabbitmq-service           NodePort    10.101.72.56     <none>        5672:30086/TCP,15672:31942/TCP   38m
```

### Running the app and stopping it

#### Pre-requisites

To run the Orders microservice, please complete the [Building the app](#building-the-app) section before proceeding to any of the following steps.

Also make sure [Auth](https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth/tree/microprofile) service is running and Keystore is set.

1. Locally in Minikube

To run the Orders application locally on your laptop on a Kubernetes-based environment such as Minikube (which is meant to be a small development environment) we first need to get few tools installed:

- [Kubectl](https://kubernetes.io/docs/user-guide/kubectl-overview/) (Kubernetes CLI) - Follow the instructions [here](https://kubernetes.io/docs/tasks/tools/install-kubectl/) to install it on your platform.
- [Helm](https://github.com/kubernetes/helm) (Kubernetes package manager) - Follow the instructions [here](https://github.com/kubernetes/helm/blob/master/docs/install.md) to install it on your platform.

Finally, we must create a Kubernetes Cluster. As already said before, we are going to use Minikube:

- [Minikube](https://kubernetes.io/docs/getting-started-guides/minikube/) - Create a single node virtual cluster on your workstation. Follow the instructions [here](https://kubernetes.io/docs/tasks/tools/install-minikube/) to get Minikube installed on your workstation.

We not only recommend to complete the three Minikube installation steps on the link above but also read the [Running Kubernetes Locally via Minikube](https://kubernetes.io/docs/getting-started-guides/minikube/) page for getting more familiar with Minikube.

2. Remotely in ICP

[IBM Cloud Private Cluster](https://www.ibm.com/cloud/private)

Create a Kubernetes cluster in an on-premise datacenter. The community edition (IBM Cloud private-ce) is free of charge.
Follow the instructions [here](https://www.ibm.com/support/knowledgecenter/en/SSBS6K_2.1.0.2/installing/install_containers_CE.html) to install IBM Cloud private-ce.

[Helm](https://github.com/kubernetes/helm) (Kubernetes package manager)

Follow the instructions [here](https://github.com/kubernetes/helm/blob/master/docs/install.md) to install it on your platform.
If using IBM Cloud Private version 2.1.0.2 or newer, we recommend you follow these [instructions](https://www.ibm.com/support/knowledgecenter/SSBS6K_2.1.0.2/app_center/create_helm_cli.html) to install helm.

### Locally in Minikube

#### Setting up your environment

1. Start your minikube. Run the below command.

`minikube start`

You will see output similar to this.

```
Setting up certs...
Connecting to cluster...
Setting up kubeconfig...
Starting cluster components...
Kubectl is now configured to use the cluster.
```
2. To install Tiller which is a server side component of Helm, initialize helm. Run the below command.

`helm init`

If it is successful, you will see the below output.

```
$HELM_HOME has been configured at /Users/user@ibm.com/.helm.

Tiller (the helm server side component) has been installed into your Kubernetes Cluster.
Happy Helming!
```
3. Check if your tiller is available. Run the below command.

`kubectl get deployment tiller-deploy --namespace kube-system`

If it available, you can see the availability as below.

```
NAME            DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
tiller-deploy   1         1         1            1           1m
```

4. Verify your helm before proceeding like below.

`helm version`

You will see the below output.

```
Client: &version.Version{SemVer:"v2.4.2", GitCommit:"82d8e9498d96535cc6787a6a9194a76161d29b4c", GitTreeState:"clean"}
Server: &version.Version{SemVer:"v2.5.0", GitCommit:"012cb0ac1a1b2f888144ef5a67b8dab6c2d45be6", GitTreeState:"clean"}
```

#### Running the application on Minikube

1. Build the docker image.

Before building the docker image, set the docker environment.

- Run the below command.

`minikube docker-env`

You will see the output similar to this.

```
export DOCKER_TLS_VERIFY="1"
export DOCKER_HOST="tcp://192.168.99.100:2376"
export DOCKER_CERT_PATH="/Users/user@ibm.com/.minikube/certs"
export DOCKER_API_VERSION="1.23"
# Run this command to configure your shell:
# eval $(minikube docker-env)
```
- For configuring your shell, run the below command.

`eval $(minikube docker-env)`

- Now run the docker build.

`docker build -t orders-mp:v1.0.0 .`

If it is a success, you will see the below output.

```
Successfully built 36d1cf24d7ad
Successfully tagged orders-mp:v1.0.0
```
2. Run the helm chart as below.

Before running the helm chart in minikube, access [values.yaml](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-orders/blob/microprofile/chart/orders/values.yaml) and replace the repository with the below.

`repository: orders`

Then run the helm chart 

`helm install --name=orders chart/orders`

You will see message like below.

```
==> v1beta1/Deployment
NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
orders-deployment                           1         1         1            0           1s
```
Please wait till your deployment is ready. To verify run the below command and you should see the availability.

`kubectl get deployments`

You will see something like below.

```
==> v1beta1/Deployment
NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
orders-deployment                           1         1         1            1           9m
```
### Remotely in ICP

[IBM Cloud Private](https://www.ibm.com/cloud/private)

IBM Private Cloud has all the advantages of public cloud but is dedicated to single organization. You can have your own security requirements and customize the environment as well. Basically it has tight security and gives you more control along with scalability and easy to deploy options. You can run it externally or behind the firewall of your organization.

Basically this is an on-premise platform.

Includes docker container manager
Kubernetes based container orchestrator
Graphical user interface
You can find the detailed installation instructions for IBM Cloud Private [here](https://www.ibm.com/support/knowledgecenter/en/SSBS6K_2.1.0.2/installing/install_containers_CE.html)

#### Pushing the image to Private Registry

1. Now run the docker build.

`docker build -t orders-mp:v1.0.0 .`

If it is a success, you will see the below output.

```
Successfully built 36d1cf24d7ad
Successfully tagged orders-mp:v1.0.0
```

2. Tag the image to your private registry.

`docker tag orders:v1.0.0 <Your ICP registry>/orders:v1.0.0`

3. Push the image to your private registry.

`docker push <Your ICP registry>/orders:v1.0.0`

You should see something like below.

```
v1.0.0: digest: sha256:14f0015461aa38c78f7174011f1046fc806841856a226ca7d5d220e6a13cb4cf size: 3873
```
#### Running the application on ICP

1. Your [IBM Cloud Private Cluster](https://www.ibm.com/cloud/private) should be up and running.

2. Log in to the IBM Cloud Private. 

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/icp_dashboard.png">
</p>

3. Go to `admin > Configure Client`.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/client_config.png">
</p>

4. Grab the kubectl configuration commands.

<p align="center">
    <img src="https://github.com/ibm-cloud-architecture/refarch-cloudnative-kubernetes/blob/microprofile/static/imgs/kube_cmds.png">
</p>

5. Run those commands in your terminal.

6. If successful, you should see something like below.

```
Switched to context "xxx-cluster.icp-context".
```
7. Run the below command.

`helm init --client-only`

You will see the below

```
$HELM_HOME has been configured at /Users/user@ibm.com/.helm.
Not installing Tiller due to 'client-only' flag having been set
Happy Helming!
```

8. Verify the helm version

`helm version --tls`

You will see something like below.

```
Client: &version.Version{SemVer:"v2.7.2+icp", GitCommit:"d41a5c2da480efc555ddca57d3972bcad3351801", GitTreeState:"dirty"}
Server: &version.Version{SemVer:"v2.7.2+icp", GitCommit:"d41a5c2da480efc555ddca57d3972bcad3351801", GitTreeState:"dirty"}
```
9. Before running the helm chart in minikube, access [values.yaml](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-inventory/blob/microprofile/inventory/chart/inventory/values.yaml) and replace the repository with the your IBM Cloud Private .

`repository: <Your IBM Cloud Private Docker registry>`

Then run the helm chart 

`helm install --name=orders chart/orders --tls`

You will see message like below.

```
==> v1beta1/Deployment
NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
orders-deployment                           1         1         1            0           1s
```
Please wait till your deployment is ready. To verify run the below command and you should see the availability.

`kubectl get deployments`

You will see something like below.

```
==> v1beta1/Deployment
NAME                                        DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
orders-deployment                           1         1         1            1           1m     
```
**NOTE**: If you are using a version of ICP older than 2.1.0.2, you don't need to add the --tls at the end of the helm command.
