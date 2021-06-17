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
    private final Set<Region> regions;

    @Inject //TODO ni behöver ingen annotation här då ni skapar den i er BeanConfig
    public DistributeSSORolesService(SsmFacade ssmFacade) {
        this.ssmFacade = ssmFacade;
        this.regions = ssmFacade.getAllRegions(); //TODO kalla inte på en service i konstruktorn här.
        // Det gör servicen stateful och kan inte återanvändas säkert ifall regioner skulle ha förändrats
    }

    public DistributeSSORolesResponse handleCreateRoleEvent(CreateRoleEvent createRoleEvent) {
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();
        distributeSSORolesResponse.addCreatedParameter(createRoleEvent.getParameterName(),
                createParameter(
                        createRoleEvent.getPermissionSetName(),
                        createRoleEvent.getParameterName(),
                        createRoleEvent.getArn()));
        return distributeSSORolesResponse;
    }

    public DistributeSSORolesResponse handleDeleteRoleEvent(DeleteRoleEvent deleteRoleEvent) {
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();
        distributeSSORolesResponse.addDeletedParameter(deleteRoleEvent.getParameterName(),
                deleteParameter(deleteRoleEvent.getParameterName()));
        return distributeSSORolesResponse;
    }

    public DistributeSSORolesResponse handleMonthlyEvent(Set<Role> roles) {

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
        return regions.stream()
                .map(region -> SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn))
                .filter(ssmFacade::putParameter)
                .map(SsmPutParameterRequest::getRegion)
                .collect(Collectors.toSet());
    }

    private Set<Region> deleteParameter(ParameterName parameterName) {
        return regions.stream()
                .map(region -> SsmDeleteParameterRequest.create(region, parameterName))
                .filter(ssmFacade::deleteParameter)
                .map(SsmDeleteParameterRequest::getRegion)
                .collect(Collectors.toSet());
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


    //TODO kan vara static den inte använder sig av nån data från det instansierade objektet
    private Set<Parameter> getParametersWithoutRole(Set<Role> roles, Set<Parameter> parameters) {
        return parameters.stream()
                .filter(parameter -> parameterHasNoRole(roles, parameter))
                .collect(Collectors.toSet());
    }

    //TODO kan vara static den inte använder sig av nån data från det instansierade objektet
    private SsmPutParameterRequest buildSsmPutParameterRequest(Role role, Region region) {
        Arn arn = Arn.create(role.arn());
        PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
        ParameterName parameterName = ParameterName.create(permissionSetName);
        return SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn);
    }

    //TODO kan vara static den inte använder sig av nån data från det instansierade objektet
    private boolean parameterHasNoRole(Set<Role> iamRoles, Parameter parameter) {
        return iamRoles.stream()
                .map(Role::arn)
                .noneMatch(arn -> arn.equals(parameter.value()));
    }
}
