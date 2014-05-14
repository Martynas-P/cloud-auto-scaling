# Cloud Auto-Scaling

## About
This my Final Year Project for the BSc Computer Science course at University of Leeds.

This application deploys an infrastructure of a web application to the cloud. It was built
to work with OpenNebula 3.8

It instantiates one VM with HTTP Load Balancer running and another VM with Application container
listening for any incoming requests. In the original project I used Tomcat 7 and Apache 2.2.
It also collects CPU and Memory utilisation using collectd.

The auto-scaling program monitors web application's response time and allocates or deallocates VMs
if thresholds are exceeded.

The auto-scaling program also uses a trained artificial neural network to predict the traffic
and adjust the cluster's size accordingly.
