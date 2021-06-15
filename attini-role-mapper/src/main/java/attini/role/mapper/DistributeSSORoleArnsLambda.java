package attini.role.mapper;

import attini.role.mapper.domain.*;
import com.amazonaws.services.acmpca.model.InvalidArgsException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.Parameter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

// aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
// aws cloudformation delete-stack --stack-name joel-test
// aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM
// mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json
//
@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, DistributeSSORoleArnsLambdaResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORoleArnsLambda.class);

    //@Inject
    SsmService ssmService;

    // TODO: Skriv logik för TriggerMonthly.
    @Override
    public DistributeSSORoleArnsLambdaResponse handleRequest(ScheduledEvent event, Context context) {
        LOGGER.info("Got event " + event);

        // TESTING
        ssmService = new SsmService(SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build());


        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());
        //TODO make sure only one resource is in json payload
        if (event.getResources().size() > 1 || event.getResources().isEmpty()){
            throw new IllegalArgumentException("Resource array in json payload must contain exactly one element.");
        }
        if (event.getResources().get(0).contains("-TriggerMonthly-")) {
            return handleMonthlyTrigger();
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

    public static void main(String[] args) {
        DistributeSSORoleArnsLambda lambda = new DistributeSSORoleArnsLambda();
        ScheduledEvent event = new ScheduledEvent();
        ArrayList resources = new ArrayList<String>();
        resources.add("arn:aws:events:us-east-1:123456789012:rule/-TriggerMonthly-");
        event.setResources(resources);
        lambda.handleRequest(event, null);
    }

    private DistributeSSORoleArnsLambdaResponse handleMonthlyTrigger() {

        DistributeSSORoleArnsLambdaResponse lambdaResponse = new DistributeSSORoleArnsLambdaResponse();

        LinkedHashSet<Region> successfulCreateRegions = new LinkedHashSet<>();
        LinkedHashSet<Region> successfulDeleteRegions = new LinkedHashSet<>();

        IamClient iamClient = IamClient.builder().region(Region.AWS_GLOBAL).httpClient(UrlConnectionHttpClient.create()).build();
        ListRolesIterable listRolesResponses = iamClient.listRolesPaginator(ListRolesRequest.builder().pathPrefix("/aws-reserved/sso.amazonaws.com/").build());
        List<Region> regions = ssmService.getAllRegions();
        // Is in IAM not in parameters.
        ArrayList<Role> iamRoles = new ArrayList<>();
        listRolesResponses.roles().stream().forEach(role -> iamRoles.add(role));
        // TODO convert to functional?
        for(Region region : regions) {
            HashSet<Parameter> parameters = ssmService.getParameters(region);
            if (parameters.size() == 0) {
                LOGGER.info("No parameters found in region: " + region + ", check if region is configured correctly.");
                continue;
            }
            for (Role role : iamRoles) {
                Arn arn = Arn.create(role.arn());
                PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
                ParameterName parameterName = ParameterName.create(permissionSetName);
                SsmPutParameterRequest putParameterRequest = SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn);
                if (ssmService.putParameter(putParameterRequest)) {
                    LOGGER.info("Saved: " + parameterName.getName() + " in region: " + region);
                    //successfulCreateRegions.add(region);
                }
                else {
                    LOGGER.warn("Could not create parameter: " + parameterName.getName() + " in region: " + region);
                }
            }
            for (Parameter parameter : parameters){
                if (iamRoles.stream().map(role -> role.arn()).collect(Collectors.toList()).contains(parameter.value()) == false){
                    ParameterName parameterName = ParameterName.create(parameter.name());
                    SsmDeleteParameterRequest deleteParameterRequest = SsmDeleteParameterRequest.create(region, parameterName);
                    if (ssmService.deleteParameter(deleteParameterRequest)){
                        LOGGER.info("Deleted: " + parameterName.getName() + " in region: " + region);
                        successfulDeleteRegions.add(region);
                    }
                    else {
                        LOGGER.warn("Could not delete parameter " + parameterName.getName() + " in region: " + region);
                    }
                }
            }
        }

        // Is in parameters and shall be destroyed.



        return lambdaResponse;
    }

    private DistributeSSORoleArnsLambdaResponse handleEventTrigger(JsonNode details) {
        DistributeSSORoleArnsLambdaResponse lambdaResponse = new DistributeSSORoleArnsLambdaResponse();
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

        LinkedHashSet<Region> successfulCreateRegions = new LinkedHashSet<>();
        LinkedHashSet<Region> successfulDeleteRegions = new LinkedHashSet<>();
        if (eventName.equals("CreateRole")) {
            regions.stream().map(region -> SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn))
                    .forEach(request -> {
                        if (ssmService.putParameter(request)) {
                            LOGGER.info("Saved: " + parameterName.getName() + " in region: " + request.getRegion());
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
                            LOGGER.info("Deleted: " + parameterName.getName() + " in region: " + request.getRegion());
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

