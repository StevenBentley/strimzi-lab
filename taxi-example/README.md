# Taxi Dataset Demo

A sample application using the NYC taxi dataset, also used in the DEBS 2015 Grand Challenge.

The deployment has the following application structure:

<p align=center>
  <img src="doc/assets/taxi-implementation.png" alt="Appliction Structure">
</p>

For a more detailed discussion of the implementation details please [follow the link here](doc/README.md).

## Building

To rebuild the docker containers with any changes, set the `USER` environment variable to the username of the docker hub account the images should be pushed to, and ensure `sorteddata.csv` ([source](<http://www.debs2015.org/call-grand-challenge.html>)) is located in [taxi-producer/src/main/resources](taxi-producer/src/main/resources). Simply run:

```bash
make all
```

## Deployment

There are two separate application deployments, one using KafkaConnect, and the other using a separate docker image to feed in the data. For each a YAML has been provided for both OpenShift and Kubermetes. See the relevant sections below for deployment. The instructions here assume that you have setup your cluster appropriately, following the [strimzi quickstart documentation](<https://strimzi.io/quickstarts/>).

### Using Kafka Connector

#### Deploy the demo on OpenShift

TODO

#### Deploy the demo on Kubernetes

TODO

### Using Simple Source

#### Deploy the demo on OpenShift

To deploy the application, adjust the image names in [deployment/oc.yaml](deployment/oc.yaml) appropriately, or leave unchanged for the default images.

```bash
oc create -f deployment/oc.yaml
```

Removing the application is as simple as:

```bash
oc delete all -l app=taxi-example
```

Once completed delete all topics:

```bash
oc delete cm -l strimzi.io/kind=topic
```

#### Deploy the demo on Kubernetes

To deploy the application, adjust the image names in [deployment/k8s.yaml](deployment/k8s.yaml) appropriately, or leave unchanged for the default images.

```bash
kubectl apply -f deployment/k8s.yaml
```

You can find a link to the deployed route by running:

```
minikube service list
```

Removing all components:

```bash
kubectl delete all -l app=taxi-example -n kafka
```

Deleting all topics:

```bash
kubectl delete kafkatopic --all -n kafka
```
