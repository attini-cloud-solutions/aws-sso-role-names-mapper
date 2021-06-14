package attini.role.mapper;

import attini.role.mapper.domain.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

// aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
// aws cloudformation delete-stack --stack-name joel-test
// aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM


@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, String> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORoleArnsLambda.class);

    @Inject
    SsmService ssmService;

    // TODO: Skriv logik för TriggerMonthly (ny lambda?).
    // TODO: Skapa ett objekt vi kan returnera.
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        LOGGER.log(Logger.Level.INFO, "Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());

        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());

        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            LOGGER.log(Logger.Level.ERROR,"Invalid event, please verify the cloudtrail filter");
            return "Failed";
        }

        String eventName = details.get("eventName").asText();

        PermissionSetName permissionSetName = PermissionSetName.create(details.get("requestParameters").get("roleName").asText().trim().split("_")[0]);
        ParameterName parameterName = ParameterName.create(permissionSetName);
        List<Region> regions = ssmService.getAllRegions();

        if (eventName.equals("CreateRole")) {
            for(Region region : regions) {
                try {

                    SsmPutParameterRequest ssmPutParameterRequest = SsmPutParameterRequest.create(region, parameterName, permissionSetName);
                    ssmService.putParameter(ssmPutParameterRequest);
                    LOGGER.log(Logger.Level.INFO, "Saved: " + parameterName + " in region: " + region);
                }
                catch (SsmException e) {
                    // TODO: Move Try/Catch logic to service class.
                    // SsmException is internal to SsmService, should be in SsmService.
                    LOGGER.warn("Could not create the parameter in " + region, e);
                }
            }
        }
        else if (eventName.equals("DeleteRole")) {
            for(Region region : regions) {
                try {
                    SsmDeleteParameterRequest ssmDeleteParameterRequest = SsmDeleteParameterRequest.create(region, parameterName);
                    ssmService.deleteParameter(ssmDeleteParameterRequest);
                    LOGGER.log(Logger.Level.INFO,"Deleted: " + parameterName + " in region: " + region);
                }
                catch (SsmException e) {
                    LOGGER.warn("Could not delete the parameter in " + region, e);
                }
            }
        }
        return "Success";
    }
}
