package attini.role.mapper.services;

import attini.role.mapper.domain.*;
import attini.role.mapper.facades.SsmFacade;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DistributeSSORolesService {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesService.class);
    private final SsmFacade ssmFacade;

    @Inject
    public DistributeSSORolesService(SsmFacade ssmService) {
        this.ssmFacade = ssmService;
    }

    private DistributeSSORolesResponse handleEventTrigger(JsonNode details) {
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();
        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());

        if (!roleName.getName().startsWith("AWSReservedSSO")) {
            throw new IllegalArgumentException("Invalid event, please verify the cloudtrail filter");
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
        } else if (eventName.equals("DeleteRole")) {
            // TODO: Fixa det här
            distributeSSORolesResponse.addDeletedParameter(parameterName, regions);
        }

        return lambdaResponse;
    }

    private Set<Region> deleteParameter(ParameterName parameterName, Set<Region> regions) {
        return regions.stream()
                .map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                .filter(ssmFacade::deleteParameter)
                .map(SsmDeleteParameterRequest::getRegion)
                .collect(Collectors.toSet());

    }


    public DistributeSSORolesResponse monthlyCleanup(Set<Role> roles) {

        Set<Region> regions = ssmFacade.getAllRegions();

        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();

        for (Region region : regions) {
            Set<Parameter> parameters = ssmFacade.getParameters(region);
            if (parameters.isEmpty()) {
                LOGGER.info("No parameters found in region: " + region + ", check if region is configured correctly.");
            } else {
                distributeSSORolesResponse.addCreatedParameters(createParametersForAllSSORoles(roles, region), region);
                distributeSSORolesResponse.addDeletedParameters(deleteParametersWithoutRole(roles, region, parameters), region);
            }
        }

        return distributeSSORolesResponse;
    }

    private Set<ParameterName> createParametersForAllSSORoles(Set<Role> roles, Region region) {
        return roles.stream()
                .map(role -> buildSsmPutParameterRequest(role, region))
                .filter(ssmFacade::putParameter)
                .map(SsmPutParameterRequest::getParameterName)
                .collect(Collectors.toSet());
    }

    private Set<ParameterName> deleteParametersWithoutRole(Set<Role> roles, Region region, Set<Parameter> parameters) {
        Set<Parameter> parametersWithoutRole = getParametersWithoutRole(roles, parameters);
        SsmDeleteParametersRequest ssmDeleteParametersRequest = SsmDeleteParametersRequest.create(parametersWithoutRole, region);

        if (ssmFacade.deleteParameters(ssmDeleteParametersRequest)) {
            return parametersWithoutRole.stream()
                    .map(parameter -> ParameterName.create(parameter.name()))
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private Set<Parameter> getParametersWithoutRole(Set<Role> roles, Set<Parameter> parameters) {
        return parameters.stream()
                .filter(parameter -> parameterHasNoRole(roles, parameter))
                .collect(Collectors.toSet());
    }

    private SsmPutParameterRequest buildSsmPutParameterRequest(Role role, Region region) {
        Arn arn = Arn.create(role.arn());
        PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
        ParameterName parameterName = ParameterName.create(permissionSetName);
        return SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn);
    }

    private boolean parameterHasNoRole(Set<Role> iamRoles, Parameter parameter) {
        return iamRoles.stream()
                .map(Role::arn)
                .noneMatch(arn -> arn.equals(parameter.value()));
    }
}
