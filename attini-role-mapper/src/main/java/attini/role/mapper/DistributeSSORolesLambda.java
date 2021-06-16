package attini.role.mapper;

import attini.role.mapper.domain.*;
import attini.role.mapper.services.DistributeSSORolesService;
import attini.role.mapper.facades.IamFacade;
import attini.role.mapper.facades.SsmFacade;
import com.amazonaws.services.acmpca.model.InvalidArgsException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

// aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
// aws cloudformation delete-stack --stack-name joel-test
// aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM
// mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json
//
@Named("DistributeSSORolesLambda")
public class DistributeSSORolesLambda implements RequestHandler<ScheduledEvent, DistributeSSORolesResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesLambda.class);

    @Inject
    public DistributeSSORolesLambda(DistributeSSORolesService distributeSSORolesService, SsmFacade ssmFacade, IamFacade iamFacade) {
        this.distributeSSORolesService = distributeSSORolesService;
        this.ssmFacade = ssmFacade;
        this.iamFacade = iamFacade;
    }
    private final DistributeSSORolesService distributeSSORolesService;
    private final SsmFacade ssmFacade;
    private final IamFacade iamFacade;

    @Override
    public DistributeSSORolesResponse handleRequest(ScheduledEvent event, Context context) {
        LOGGER.info("Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());
        if (event.getResources().size() > 1 || event.getResources().isEmpty()){
            throw new IllegalArgumentException("Resource array in json payload must contain exactly one element.");
        }
        if (event.getResources().get(0).contains("-TriggerMonthly-")) {
            return distributeSSORolesService.monthlyCleanup(iamFacade.listAllRoles());
        }
        else {
            try {
                return handleEventTrigger(details);
            }
            catch (Exception e) {
                LOGGER.info(e.getMessage());
                return null;
            }
        }
    }

    private DistributeSSORolesResponse handleEventTrigger(JsonNode details) {
        DistributeSSORolesResponse lambdaResponse = new DistributeSSORolesResponse();
        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());
        
        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            throw new InvalidArgsException("Invalid event, please verify the cloudtrail filter");
        }

        String eventName = details.get("requestParameters").get("eventName").asText();
        PermissionSetName permissionSetName = PermissionSetName.create(details.get("requestParameters").get("roleName").asText());
        ParameterName parameterName = ParameterName.create(permissionSetName);
        Arn arn = Arn.create(details.get("requestResponse").get("role").get("arn").asText());
        Set<Region> regions = ssmFacade.getAllRegions();

        if (eventName.equals("CreateRole")) {
            regions.stream().map(region -> SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn))
                    .forEach(request -> {
                        if (ssmFacade.putParameter(request)) {
                            LOGGER.info("Saved: " + parameterName.getName() + " in region: " + request.getRegion());
                            lambdaResponse.addCreatedParameter(parameterName, request.getRegion());
                        } else {
                            LOGGER.warn("Could not create the parameter in " + request.getRegion());
                        }
                    });
        }

        else if (eventName.equals("DeleteRole")) {
            regions.stream().map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                    .forEach(request -> {
                        if(ssmFacade.deleteParameter(request)) {
                            LOGGER.info("Deleted: " + parameterName.getName() + " in region: " + request.getRegion());
                            lambdaResponse.addDeletedParameter(parameterName, request.getRegion());
                        }
                        else {
                            LOGGER.warn("Could not delete the parameter in " + request.getRegion());
                        }
                    });
        }

        return lambdaResponse;
    }
}

