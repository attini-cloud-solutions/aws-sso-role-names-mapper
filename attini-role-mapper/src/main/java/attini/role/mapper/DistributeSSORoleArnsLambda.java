package attini.role.mapper;

import attini.role.mapper.domain.*;
import com.amazonaws.services.acmpca.model.InvalidArgsException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;
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
// sam local invoke -t target/sam.jvm.yaml -e payload.json

@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, DistributeSSORoleArnsLambdaResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORoleArnsLambda.class);

    @Inject
    SsmService ssmService;

    // TODO: Skriv logik för TriggerMonthly.
    @Override
    public DistributeSSORoleArnsLambdaResponse handleRequest(ScheduledEvent event, Context context) {
        LOGGER.info("Got event " + event);

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
            return handleEventTrigger(details);
        }
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
        listRolesResponses.stream().map(response -> iamRoles.addAll(response.roles()));

        for(Region region : regions) {
            HashSet<Parameter> parameters = ssmService.getParameters(region);
            for (Role role : iamRoles) {
                if (parameters.stream().map(parameter -> parameter.value()).noneMatch(arn -> arn.equals(role.arn()))) {
                    PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
                    ParameterName parameterName = ParameterName.create(permissionSetName);
                    regions.stream().map(reg -> SsmPutParameterRequest.create(reg, parameterName, permissionSetName))
                            .forEach(request -> {
                                if (ssmService.putParameter(request)) {
                                    LOGGER.info("Saved: " + parameterName + " in region: " + request.getRegion());
                                    successfulCreateRegions.add(request.getRegion());
                                } else {
                                    LOGGER.warn("Could not create the parameter in " + request.getRegion());
                                }
                            });
                }
            }
        }

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
        PermissionSetName permissionSetName = PermissionSetName.create(details.get("requestParameters").get("roleName").asText().trim().split("_")[1]);
        ParameterName parameterName = ParameterName.create(permissionSetName);
        List<Region> regions = ssmService.getAllRegions();

        LinkedHashSet<Region> successfulCreateRegions = new LinkedHashSet<>();
        LinkedHashSet<Region> successfulDeleteRegions = new LinkedHashSet<>();
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

