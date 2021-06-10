package attini.role.mapper;

import attini.role.mapper.domain.ParameterName;
import attini.role.mapper.domain.PermissionSetName;
import attini.role.mapper.domain.RoleName;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
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

        PermissionSetName permissionSetName = PermissionSetName.create(details.get("requestParameters").get("roleName").asText().trim().split("_")[0]);
        ParameterName parameterName = ParameterName.create(permissionSetName);
        List<Region> regions = ssmService.getAllRegions();

        if (eventName == "CreateRole") {
            for(Region region : regions) {
                try {
                    PutParameterRequest putParameterRequest = ssmService.getCreateParameterRequest(region, parameterName, permissionSetName);
                    PutParameterResponse putParameterResponse = ssmService.putParameter(region, putParameterRequest);
                    System.out.println("Saved: " + parameterName + " in region: " + region);
                }
                catch (SsmException e) {
                    //TODO clean up ssmexception message.
                    System.err.println("Could not create the parameter in " + region);
                    System.err.println("AWS error details: " + e.awsErrorDetails());
                }
            }
        }
        else if (eventName == "DeleteRole") {
            for(Region region : regions) {
                try {
                    DeleteParameterRequest deleteParameterRequest = ssmService.getDeleteParameterRequest(parameterName);
                    DeleteParameterResponse deleteParameterResponse = ssmService.deleteParameter(region, deleteParameterRequest);
                    System.out.println("Deleted: " + parameterName + " in region: " + region);
                }
                catch (SsmException e) {
                    //TODO clean up ssmexception message.
                    System.err.println("Could not delete the parameter in " + region);
                    System.err.println("AWS error details: " + e.awsErrorDetails());
                }
            }
        }

        // aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
        // aws cloudformation delete-stack --stack-name joel-test
        // aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM

        return new String("Success");
    }
}
