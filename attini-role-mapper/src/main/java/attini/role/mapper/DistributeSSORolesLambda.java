package attini.role.mapper;

import attini.role.mapper.domain.*;
import attini.role.mapper.services.DistributeSSORolesService;
import attini.role.mapper.services.IamFacade;
import attini.role.mapper.services.SsmFacade;
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
import java.util.List;

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
    public DistributeSSORolesLambda(DistributeSSORolesService distributeSSORolesService, SsmFacade ssmService, IamFacade iamService) {
        this.distributeSSORolesService = distributeSSORolesService;
        this.ssmService = ssmService;
        this.iamService = iamService;
    }
    private final DistributeSSORolesService distributeSSORolesService;
    private final SsmFacade ssmService;
    private final IamFacade iamService;

    @Override
    public DistributeSSORolesResponse handleRequest(ScheduledEvent event, Context context) {
        LOGGER.info("Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());
        //TODO make sure only one resource is in json payload
        if (event.getResources().size() > 1 || event.getResources().isEmpty()){
            throw new IllegalArgumentException("Resource array in json payload must contain exactly one element.");
        }
        if (event.getResources().get(0).contains("-TriggerMonthly-")) {
            return distributeSSORolesService.monthlyCleanup();
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

//    public static void main(String[] args) {
//        SsmService ssmService = new SsmService(SsmClient.builder().httpClient(UrlConnectionHttpClient.create()));
//        IamService iamService = new IamService(IamClient.builder().httpClient(UrlConnectionHttpClient.create()));
//        DistributeSSORolesService distributeSSORolesService = new DistributeSSORolesService(iamService, ssmService);
//        DistributeSSORolesLambda lambda = new DistributeSSORolesLambda(distributeSSORolesService, ssmService, iamService);
//        ScheduledEvent event = new ScheduledEvent();
//        ArrayList<String> resources = new ArrayList<>();
//        resources.add("arn:aws:events:us-east-1:123456789012:rule/-TriggerMonthly-");
//        event.setResources(resources);
//
//        lambda.handleRequest(event, null);
//    }


    private DistributeSSORolesResponse handleEventTrigger(JsonNode details) {
        DistributeSSORolesResponse lambdaResponse = new DistributeSSORolesResponse();
        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());
        
        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            throw new InvalidArgsException("Invalid event, please verify the cloudtrail filter");
        }

        String eventName = details.get("requestParameters").get("eventName").asText();
        // TODO make sure roleName String split always works...
        PermissionSetName permissionSetName = PermissionSetName.create(details.get("requestParameters").get("roleName").asText());
        ParameterName parameterName = ParameterName.create(permissionSetName);
        Arn arn = Arn.create(details.get("requestResponse").get("role").get("arn").asText());
        List<Region> regions = ssmService.getAllRegions();

        if (eventName.equals("CreateRole")) {
            regions.stream().map(region -> SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn))
                    .forEach(request -> {
                        if (ssmService.putParameter(request)) {
                            LOGGER.info("Saved: " + parameterName.getName() + " in region: " + request.getRegion());
                            lambdaResponse.addRegionToCreatedParameter(parameterName, request.getRegion());
                        } else {
                            LOGGER.warn("Could not create the parameter in " + request.getRegion());
                        }
                    });
        }

        else if (eventName.equals("DeleteRole")) {
            regions.stream().map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                    .forEach(request -> {
                        if(ssmService.deleteParameter(request)) {
                            LOGGER.info("Deleted: " + parameterName.getName() + " in region: " + request.getRegion());
                            lambdaResponse.addRegionToDeletedParameter(parameterName, request.getRegion());
                        }
                        else {
                            LOGGER.warn("Could not delete the parameter in " + request.getRegion());
                        }
                    });
        }

        return lambdaResponse;
    }
}

