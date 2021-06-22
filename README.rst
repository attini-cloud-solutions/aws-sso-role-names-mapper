*************************
AWS-SSO-ROLE-NAMES-MAPPER
*************************
Description
===========
The Problem
-----------
Generated IAM roles by SSO, have long and hard to remember names like:
"*AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad*"

The Solution
------------
| This project creates and maps IAM role names created by SSO to simple names in AWS Parameter store.
|
| IAM Role Name: "*AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad*"
| Name stored in Parameter Store: "*DatabaseAdministrator*"

Prerequisites
=============

- `AWS CLI <https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html>`_ (If you are using AWS SSO you need AWS CLI version 2)
- Configured `AWS CLI credentials <https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html>`_
- `Java JDK 11 <https://www.oracle.com/se/java/technologies/javase-jdk11-downloads.html>`_ (or later)
- For native compilation, see `this guide <https://quarkus.io/guides/building-native-image#configuring-graalvm>`_
- `Maven <https://maven.apache.org/install.html>`_ for installing project dependencies.


Build
=============

1. Clone this repository into a folder of choice
2. Run :code:`mvn clean package`
   
   - [:code:`-Pnative`] For native compilation
   - [:code:`-Dquarkus.native.container-build=true`] to build inside a container (Requires `Docker <https://docs.docker.com/get-docker/>`_)
3. For local testing, run :code:`mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json`

Deploy
=============
1. Clone this repository into a folder of choice
2. Create a bucket to store zipped code (:code:`aws s3 mb s3://mybucket --region us-east-1`)
3. Set stackName, parameterStorePrefix, s3Bucket and s3BucketKey in pom.xml (Region should be us-east-1)
4. Run :code:`mvn deploy`
   
   - [:code:`-Pnative`] For native compilation
   - [:code:`-Dquarkus.native.container-build=true`] to build inside a container (Requires `Docker <https://docs.docker.com/get-docker/>`_)

Cloudformation Stack
====================
.. image:: AWSSSORoleNamesMapper.png
   :width: 400
