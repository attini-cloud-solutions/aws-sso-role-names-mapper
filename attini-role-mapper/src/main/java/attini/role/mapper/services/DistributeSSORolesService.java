package attini.role.mapper.services;

import attini.role.mapper.domain.*;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DistributeSSORolesService {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesService.class);
    private final IamService iamService;
    private final SsmService ssmService;

    @Inject
    public DistributeSSORolesService(IamService iamService, SsmService ssmService) {
        this.iamService = iamService;
        this.ssmService = ssmService;
    }

    public DistributeSSORolesResponse monthlyCleanup() {

        List<Role> roles = iamService.listAllRoles();
        List<Region> regions = ssmService.getAllRegions();

        // TODO populate this response
        DistributeSSORolesResponse distributeSSORolesResponse = new DistributeSSORolesResponse();

        for(Region region : regions) {
            HashSet<Parameter> parameters = ssmService.getParameters(region);
            if (parameters.isEmpty()) {
                LOGGER.info("No parameters found in region: " + region + ", check if region is configured correctly.");
            }
            else {
                createParametersForAllSSORoles(roles, region);
                deleteParametersWithoutRole(roles, region, parameters);
            }
        }

        return distributeSSORolesResponse;
    }
    // TODO should return DistributeResponse (pass it in, then call addCreateParam...?)
    private void createParametersForAllSSORoles(List<Role> roles, Region region) {
        roles.stream().map(role -> buildSsmPutParameterRequest(role, region))
                .forEach(ssmService::putParameter);

    }

    // TODO should return DistributeResponse
    private void deleteParametersWithoutRole(List<Role> roles, Region region, HashSet<Parameter> parameters) {
        List<Parameter> parametersWithoutRole = getParametersWithoutRole(roles, region, parameters);
        SsmDeleteParametersRequest ssmDeleteParametersRequest = SsmDeleteParametersRequest.create(parametersWithoutRole, region);
        ssmService.deleteParameters(ssmDeleteParametersRequest);
    }

    public List<Parameter> getParametersWithoutRole(List<Role> roles, Region region, HashSet<Parameter> parameters) {
        return parameters.stream()
                .filter(parameter -> parameterHasNoRole(roles, parameter))
                .collect(Collectors.toList());
    }

    private SsmPutParameterRequest buildSsmPutParameterRequest(Role role, Region region) {
        Arn arn = Arn.create(role.arn());
        PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
        ParameterName parameterName = ParameterName.create(permissionSetName);
        return SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn);
    }

    private boolean parameterHasNoRole(List<Role> iamRoles, Parameter parameter) {
        return iamRoles.stream()
                .map(Role::arn)
                .noneMatch(arn -> arn.equals(parameter.value()));
    }

    public void getRoles(List<Role> iamRoles, Region region) {
        for (Role role : iamRoles) {
            Arn arn = Arn.create(role.arn());
            PermissionSetName permissionSetName = PermissionSetName.create(role.roleName());
            ParameterName parameterName = ParameterName.create(permissionSetName);
            SsmPutParameterRequest putParameterRequest = SsmPutParameterRequest.create(region, parameterName, permissionSetName, arn);

        }
    }
}
