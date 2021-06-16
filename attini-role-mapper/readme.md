

Fixa git repo mappen



* Hur man bygger
* Hur man deployar

Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip 
aws cloudformation delete-stack --stack-name joel-test
aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM



mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json