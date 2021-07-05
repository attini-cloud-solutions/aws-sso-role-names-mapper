package aws.sso.role.names.mapper.services;

import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;

import aws.sso.role.names.mapper.domain.Arn;
import aws.sso.role.names.mapper.domain.CreateRoleEvent;
import aws.sso.role.names.mapper.domain.DeleteRoleEvent;
import aws.sso.role.names.mapper.domain.DistributeSSORolesResponse;
import aws.sso.role.names.mapper.domain.ParameterName;
import aws.sso.role.names.mapper.domain.PermissionSetName;
import aws.sso.role.names.mapper.domain.SsmDeleteParameterRequest;
import aws.sso.role.names.mapper.domain.SsmDeleteParametersRequest;
import aws.sso.role.names.mapper.domain.SsmPutParameterRequest;
import aws.sso.role.names.mapper.domain.exceptions.CouldNotGetParametersException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import aws.sso.role.names.mapper.facades.SsmFacade;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

public class DistributeSSORolesService {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesService.class);
    private final SsmFacade ssmFacade;
    private final EnvironmentVariables environmentVariables;

    public DistributeSSORolesService(SsmFacade ssmFacade, EnvironmentVariables environmentVariables) {
        this.ssmFacade = Objects.requireNonNull(ssmFacade, "ssmFacade");
        this.environmentVariables = Objects.requireNonNull(environmentVariables, "environmentVariables");
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

    public DistributeSSORolesResponse handleDeleteAllRolesEvent() {
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();

        Set<Region> regions = ssmFacade.getAllRegions();


        for (Region region : regions) {
            try {
                Set<ParameterName> parameterNames = ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(
                        ssmFacade.getParameters(
                                region),
                        region));
                if (!parameterNames.isEmpty()) {
                    distributeSSORolesResponse.addDeletedParameters(parameterNames, region);
                }
            } catch (CouldNotGetParametersException e) {
                LOGGER.warn("Could not get parameters from region: " + region + ", check if region is configured correctly.",
                            e);
            }
        }

        return distributeSSORolesResponse;
    }

    public DistributeSSORolesResponse handleSyncRolesEvent(Set<Role> roles) {

        LOGGER.info("AWS SSO Roles in this account: " + roles.toString());

        Set<Region> regions = ssmFacade.getAllRegions();
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();

        for (Region region : regions) {
            try {
                Set<Parameter> parameters = ssmFacade.getParameters(region);
                if (parameters.isEmpty()) {
                    LOGGER.info("No parameters found in region: " + region);
                } else {
                    distributeSSORolesResponse.addDeletedParameters(deleteParametersWithoutRole(roles,
                                                                                                region,
                                                                                                parameters), region);
                }
                distributeSSORolesResponse.addCreatedParameters(createParametersForAllSSORoles(roles, region), region);
            } catch (CouldNotGetParametersException e) {
                LOGGER.warn("Could not get parameters from region: " + region + ", check if region is configured correctly.",
                            e);
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
                      .collect(toSet());
    }

    private Set<Region> deleteParameter(ParameterName parameterName) {
        Set<Region> regions = ssmFacade.getAllRegions();
        return regions.parallelStream()
                      .map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                      .filter(ssmFacade::deleteParameter)
                      .map(SsmDeleteParameterRequest::getRegion)
                      .collect(toSet());
    }

    private Set<ParameterName> createParametersForAllSSORoles(Set<Role> roles, Region region) {
        return roles.parallelStream()
                    .map(role -> buildSsmPutParameterRequest(role, region))
                    .filter(ssmFacade::putParameter)
                    .map(SsmPutParameterRequest::getParameterName)
                    .collect(toSet());
    }

    private Set<ParameterName> deleteParametersWithoutRole(Set<Role> roles, Region region, Set<Parameter> parameters) {
        Set<Parameter> parametersWithoutRole = getParametersWithoutRole(roles, parameters);
        if (!parametersWithoutRole.isEmpty()) {
            SsmDeleteParametersRequest ssmDeleteParametersRequest = SsmDeleteParametersRequest.create(
                    parametersWithoutRole,
                    region);
            return ssmFacade.deleteParameters(ssmDeleteParametersRequest);
        }
        return Collections.emptySet();
    }

    private static Set<Parameter> getParametersWithoutRole(Set<Role> roles, Set<Parameter> parameters) {
        return parameters.stream()
                         .filter(parameter -> parameterHasNoRole(roles, parameter))
                         .collect(toSet());
    }

    private static boolean parameterHasNoRole(Set<Role> iamRoles, Parameter parameter) {
        return iamRoles.stream()
                       .map(Role::arn)
                       .noneMatch(arn -> arn.equals(parameter.value()));
    }

    private SsmPutParameterRequest buildSsmPutParameterRequest(Role role, Region region) {
        PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
        ParameterName parameterName = ParameterName.create(environmentVariables.getParameterStorePrefix(),
                                                           permissionSetName);
        return SsmPutParameterRequest.create(region, parameterName, permissionSetName, Arn.create(role.arn()));
    }
}
