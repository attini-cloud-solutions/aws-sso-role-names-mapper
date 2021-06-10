package attini.role.mapper;

import attini.role.mapper.domain.RoleName;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.http.SdkHttpClient;

import javax.inject.Named;
import java.util.Objects;

@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, String> {
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {



        // System.out.println("Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());

        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());

        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            System.err.println("Invalid event, please verify the cloudtrail filter");
            return "Failed";
        }

        String eventName = details.get("eventName").asText();

        // Get regions


        String permissionSetName = getPermissionSetName(details);
        String parameterName = getParameterName(details);


        String regions = getRegions();
        System.out.println(regions);

        String PLACEHOLDER = "eu-west-1";

        SsmClient client = SsmClient.builder().region(Region.EU_WEST_1).build();


        try {
            if (eventName == "CreateRole") {
                String RoleArn = details.get("responseElements").get("role").get("arn").asText();
                PutParameterRequest parameterRequest = PutParameterRequest.builder()
                        .dataType("String")
                        .name(parameterName)
                        .description("Role arn for AWS SSO PermissionSet " + permissionSetName)
                        .value("CreateRole")
                        .overwrite(true)
                        .tier("Standard")
                        .build();
                client.putParameter(parameterRequest);
                System.out.println("Saved: " + parameterName + " in region: " + PLACEHOLDER);
            } else if (eventName == "DeleteRole") {
                DeleteParameterRequest deleteParameterRequest = DeleteParameterRequest.builder().name(parameterName).build();
                client.deleteParameter(deleteParameterRequest);
                System.out.println("Deleted: " + parameterName + " in region: " + PLACEHOLDER);
            }
        }
        catch(Exception e) {
            if(eventName == "CreateRole") {
                System.out.println("Could not create the parameter in " + PLACEHOLDER);
            }
            else if (eventName == "DeleteRole") {
                System.out.println("Could not delete the parameter in " + PLACEHOLDER);
            }
        }



        // aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
        // aws cloudformation delete-stack --stack-name joel-test
        // aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM




        // System.out.println(details.get("requestParameters").get("roleName").asText());

        // System.out.println("Got event details " + event);


        // Kom ihåg: Hämta regioner från parameter store ist för ec2

        return new String("Success");
    }

    public String getPermissionSetName(JsonNode details) {
        String result = details.get("requestParameters").get("roleName").asText().trim().split("_")[0];
        System.out.println("PERMISSION SET NAME: " + result);
        return result;
    }

    public String getParameterName(JsonNode details) {
        String permissionSetName = getPermissionSetName(details);
        String result = "/SSORoleArns/" + permissionSetName;
        System.out.println("PARAMETER NAME: " + result);
        return result;
    }



    public String getRegions() {
        SdkHttpClient client = SdkHttpClient.newHttpClient();
        SsmClient ssmClient = SsmClient.builder().httpClient(client).build();

        // GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions/");
        // Dör alltid på denna rad, krashar för out of memory på jvm och kompilerar ej till native.
        // GetParametersByPathResponse response = ssmClient.getParametersByPath(requestBuilder.build());
        // return response.toString();
        return "sample-region";
    }
}
