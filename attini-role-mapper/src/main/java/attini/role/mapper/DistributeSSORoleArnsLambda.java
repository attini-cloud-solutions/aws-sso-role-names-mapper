package attini.role.mapper;

import attini.role.mapper.domain.RoleName;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, String> {

    @Inject
    SsmService ssmService;

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        System.out.println("Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());



        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());

        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            System.err.println("Invalid event, please verify the cloudtrail filter");
            return "Failed";
        }

        String eventName = details.get("eventName").asText();

        String permissionSetName = getPermissionSetName(details);
        String parameterName = getParameterName(details);

        List<Region> regions = ssmService.getAllRegions();

        if (eventName == "CreateRole") {
            for(Region region : regions) {
                try {
                    SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region).build();
                    PutParameterRequest parameterRequest = PutParameterRequest.builder()
                            .dataType("String")
                            .name(parameterName)
                            .description("Role arn for AWS SSO PermissionSet " + permissionSetName)
                            .value("CreateRole")
                            .overwrite(true)
                            .tier("Standard")
                            .build();
                    client.putParameter(parameterRequest);
                    System.out.println("Saved: " + parameterName + " in region: " + region);
                }
                catch (Exception e) {
                    System.err.println("Could not create the parameter in " + region);
                }
            }
        }
        else if (eventName == "DeleteRole") {
            for(Region region : regions) {
                try {
                    SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region).build();
                    DeleteParameterRequest deleteParameterRequest = DeleteParameterRequest.builder().name(parameterName).build();
                    client.deleteParameter(deleteParameterRequest);
                    System.out.println("Deleted: " + parameterName + " in region: " + region);
                }
                catch (Exception e) {
                    System.err.println("Could not delete the parameter in " + region);
                }
            }
        }

        // aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
        // aws cloudformation delete-stack --stack-name joel-test
        // aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM

        return new String("Success");
    }

    private String getPermissionSetName(JsonNode details) {
        String result = details.get("requestParameters").get("roleName").asText().trim().split("_")[0];
        return result;
    }

    private String getParameterName(JsonNode details) {
        String permissionSetName = getPermissionSetName(details);
        String result = "/SSORoleArns/" + permissionSetName;
        return result;
    }
}
