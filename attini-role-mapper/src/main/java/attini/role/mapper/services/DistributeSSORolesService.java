package attini.role.mapper.services;

import attini.role.mapper.domain.*;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DistributeSSORolesService {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesService.class);
    private final IamFacade iamService;
    private final SsmFacade ssmFacade;

    @Inject
    public DistributeSSORolesService(IamFacade iamService, SsmFacade ssmService) {
        this.iamService = iamService;
        this.ssmFacade = ssmService;
    }

    public DistributeSSORolesResponse monthlyCleanup(Set<Role> roles) {

        // TODO: Ta in listorna istället
        // List<Role> roles = iamService.listAllRoles();
        Set<Region> regions = ssmFacade.getAllRegions();

        // TODO populate this response
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();

        for(Region region : regions) {
            Set<Parameter> parameters = ssmFacade.getParameters(region);
            if (parameters.isEmpty()) {
                LOGGER.info("No parameters found in region: " + region + ", check if region is configured correctly.");
            }
            else {
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

    private  Set<ParameterName> deleteParametersWithoutRole(Set<Role> roles, Region region, Set<Parameter> parameters) {
        Set<Parameter> parametersWithoutRole = getParametersWithoutRole(roles, parameters);
        SsmDeleteParametersRequest ssmDeleteParametersRequest = SsmDeleteParametersRequest.create(parametersWithoutRole, region);
        ssmFacade.deleteParameters(ssmDeleteParametersRequest);
        return parametersWithoutRole.stream()
                .map(parameter -> ParameterName.create(parameter.name()))
                .collect(Collectors.toSet());
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
