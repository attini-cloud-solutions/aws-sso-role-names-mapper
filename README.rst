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
|


Prerequisites
=============

- `AWS CLI <https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html>`_ (If you are using AWS SSO you need AWS CLI version 2)
- Configured `AWS CLI credentials <https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html>`_
- `Java JDK 11 <https://www.oracle.com/se/java/technologies/javase-jdk11-downloads.html>`_ (or later)
- For native compilation, see `this guide <https://quarkus.io/guides/building-native-image#configuring-graalvm>`_
- `Maven <https://maven.apache.org/install.html>`_ for installing project dependencies.

| 

Build
=============

1. Clone this repository into a folder of choice.
2. Run mvn package (add flag -Pnative for GraalVM native compilation)
   1. f
3. 





|
|
|
|
|
|
|
|
|
|
|
|
|
|
|
|
|
| Default Parameter Store path is */attini/aws-sso-role-names-mapper/*, 
| this can easily be changed by setting environment varible *ParameterStorePrefix*. 





- Hur man bygger
- Hur man deployar

Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip aws
cloudformation delete-stack --stack-name joel-test aws cloudformation deploy --template cf-template.yaml --stack-name
joel-test --capabilites CAPABILITY_IAM

mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json