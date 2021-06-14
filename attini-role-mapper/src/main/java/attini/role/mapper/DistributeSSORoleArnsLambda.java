package attini.role.mapper;

import attini.role.mapper.domain.*;
import com.amazonaws.services.acmpca.model.InvalidArgsException;
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
import java.util.LinkedHashSet;
import java.util.List;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

// aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
// aws cloudformation delete-stack --stack-name joel-test
// aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM


@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, DistributeSSORoleArnsLambdaResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORoleArnsLambda.class);

    @Inject
    SsmService ssmService;

    // TODO: Skriv logik för TriggerMonthly.
    @Override
    public DistributeSSORoleArnsLambdaResponse handleRequest(ScheduledEvent event, Context context) {
        LOGGER.info("Got event " + event);
        DistributeSSORoleArnsLambdaResponse lambdaResponse = new DistributeSSORoleArnsLambdaResponse();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());

        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());

        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            throw new InvalidArgsException("Invalid event, please verify the cloudtrail filter");
        }

        String eventName = details.get("requestParameters").get("eventName").asText();

        PermissionSetName permissionSetName = PermissionSetName.create(details.get("requestParameters").get("roleName").asText().trim().split("_")[0]);
        ParameterName parameterName = ParameterName.create(permissionSetName);
        List<Region> regions = ssmService.getAllRegions();

        LinkedHashSet<Region> successfulCreateRegions = new LinkedHashSet<Region>();
        LinkedHashSet<Region> successfulDeleteRegions = new LinkedHashSet<Region>();
        if (eventName.equals("CreateRole")) {
            regions.stream().map(region -> SsmPutParameterRequest.create(region, parameterName, permissionSetName))
                    .forEach(request -> {
                        if (ssmService.putParameter(request)) {
                            LOGGER.info("Saved: " + parameterName + " in region: " + request.getRegion());
                            successfulCreateRegions.add(request.getRegion());
                        } else {
                            LOGGER.warn("Could not create the parameter in " + request.getRegion());
                        }
                    });
            lambdaResponse.parametersCreated.put(parameterName, successfulCreateRegions);
        }

        else if (eventName.equals("DeleteRole")) {
            regions.stream().map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                    .forEach(request -> {
                        if(ssmService.deleteParameter(request)) {
                            LOGGER.info("Deleted: " + parameterName + " in region: " + request.getRegion());
                            successfulDeleteRegions.add(request.getRegion());
                        }
                        else {
                            LOGGER.warn("Could not delete the parameter in " + request.getRegion());
                        }
                    });
            lambdaResponse.parametersDeleted.put(parameterName, successfulDeleteRegions);
        }

        return lambdaResponse;
    }
}
