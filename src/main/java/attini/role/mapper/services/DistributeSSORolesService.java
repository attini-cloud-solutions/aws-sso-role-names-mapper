package attini.role.mapper.services;

import attini.role.mapper.domain.*;
import attini.role.mapper.facades.EnvironmentVariables;
import attini.role.mapper.facades.SsmFacade;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DistributeSSORolesService {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesService.class);
    private final SsmFacade ssmFacade;
    private final EnvironmentVariables environmentVariables;

    public DistributeSSORolesService(SsmFacade ssmFacade, EnvironmentVariables environmentVariables) {
        this.ssmFacade = Objects.requireNonNull(ssmFacade, "ssmFacade");
        this.environmentVariables = Objects.requireNonNull(environmentVariables, "environmentVariables");
    }

    private static Set<Parameter> getParametersWithoutRole(Set<Role> roles, Set<Parameter> parameters) {
        return parameters.stream()
                .filter(parameter -> parameterHasNoRole(roles, parameter))
                .collect(Collectors.toSet());
    }

    private static boolean parameterHasNoRole(Set<Role> iamRoles, Parameter parameter) {
        return iamRoles.stream()
                .map(Role::arn)
                .noneMatch(arn -> arn.equals(parameter.value()));
    }

    public DistributeSSORolesResponse handleCreateRoleEvent(CreateRoleEvent createRoleEvent) {
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();
        distributeSSORolesResponse.addCreatedParameter(createRoleEvent.getParameterName(),
                createParameter(
                        createRoleEvent.getPermissionSetName(),
                        createRoleEvent.getParameterName(),
                        createRoleEvent.getIamRoleName()));
        return distributeSSORolesResponse;
    }

    public DistributeSSORolesResponse handleDeleteRoleEvent(DeleteRoleEvent deleteRoleEvent) {
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();
        distributeSSORolesResponse.addDeletedParameter(deleteRoleEvent.getParameterName(),
                deleteParameter(deleteRoleEvent.getParameterName()));
        return distributeSSORolesResponse;
    }

    public DistributeSSORolesResponse handleScheduledEvent(Set<Role> roles) {

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

    private Set<Region> createParameter(PermissionSetName permissionSetName, ParameterName parameterName, Arn arn) {
        Set<Region> regions = ssmFacade.getAllRegions();
        return regions.parallelStream()
                .map(region -> SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn))
                .filter(ssmFacade::putParameter)
                .map(SsmPutParameterRequest::getRegion)
                .collect(Collectors.toSet());
    }

    private Set<Region> deleteParameter(ParameterName parameterName) {
        Set<Region> regions = ssmFacade.getAllRegions();
        return regions.parallelStream()
                .map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                .filter(ssmFacade::deleteParameter)
                .map(SsmDeleteParameterRequest::getRegion)
                .collect(Collectors.toSet());
    }

    private Set<ParameterName> createParametersForAllSSORoles(Set<Role> roles, Region region) {
        return roles.parallelStream()
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

    private SsmPutParameterRequest buildSsmPutParameterRequest(Role role, Region region) {
        Arn arn = Arn.create(role.arn());
        PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
        ParameterName parameterName = ParameterName.create(environmentVariables.getParameterStorePrefix(), permissionSetName);
        return SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn);
    }
}
