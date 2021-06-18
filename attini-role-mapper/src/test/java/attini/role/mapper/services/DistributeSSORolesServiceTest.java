package attini.role.mapper.services;

import attini.role.mapper.domain.*;
import attini.role.mapper.facades.EnvironmentVariables;
import attini.role.mapper.facades.SsmFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributeSSORolesServiceTest {

    private DistributeSSORolesService distributeSSORolesService;

    @Mock
    private SsmFacade ssmFacadeMock;

    @Mock
    private EnvironmentVariables environmentVariablesMock;

    private Set<Region> regions;
    @BeforeEach
    void setUp() {
        regions = new HashSet<>();
        regions.add(Region.EU_WEST_1);
        regions.add(Region.US_EAST_1);
        regions.add(Region.EU_NORTH_1);
        when(ssmFacadeMock.getAllRegions()).thenReturn(regions);
        when(environmentVariablesMock.getParameterStorePrefix()).thenReturn("/test/");
        distributeSSORolesService = new DistributeSSORolesService(ssmFacadeMock, environmentVariablesMock);
    }


    @Test
    void handleMonthlyEvent_AllRolesHaveParameters_ShouldPass() {
        DistributeSSORolesResponse expectedResponse = new DistributeSSORolesResponse();
        String prefix = environmentVariablesMock.getParameterStorePrefix();
        ParameterName database = ParameterName.create(prefix, PermissionSetName.create("AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad"));
        ParameterName billing = ParameterName.create(prefix, PermissionSetName.create("AWSReservedSSO_Billing_c8106817c1780052"));
        ParameterName admin = ParameterName.create(prefix, PermissionSetName.create("AWSReservedSSO_AdministratorAccess_f627296b4ac7ac6c"));
        ParameterName toBeDeleted = ParameterName.create(prefix, PermissionSetName.create("AWSReservedSSO_DeleteMePlease_f627296b4ac7ac6c"));

        Parameter databaseParameter = Parameter.builder()
                .name(database.toString())
                .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad")
                .build();
        Parameter billingParameter = Parameter.builder()
                .name(billing.toString())
                .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052")
                .build();
        Parameter adminParameter = Parameter.builder()
                .name(admin.toString())
                .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_AdministratorAccess_f627296b4ac7ac6c")
                .build();
        Parameter toBeDeletedParameter = Parameter.builder()
                .name(toBeDeleted.toString())
                .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_DeleteMePlease_f627296b4ac7ac6c")
                .build();

        Set<Parameter> parameters = new HashSet<>();
        parameters.add(databaseParameter);
        parameters.add(billingParameter);
        parameters.add(adminParameter);
        parameters.add(toBeDeletedParameter);

        expectedResponse.addCreatedParameter(database, Region.EU_WEST_1);
        expectedResponse.addCreatedParameter(database, Region.US_EAST_1);
        expectedResponse.addCreatedParameter(database, Region.EU_NORTH_1);
        expectedResponse.addCreatedParameter(billing, Region.EU_WEST_1);
        expectedResponse.addCreatedParameter(billing, Region.US_EAST_1);
        expectedResponse.addCreatedParameter(billing, Region.EU_NORTH_1);
        expectedResponse.addCreatedParameter(admin, Region.EU_WEST_1);
        expectedResponse.addCreatedParameter(admin, Region.US_EAST_1);
        expectedResponse.addCreatedParameter(admin, Region.EU_NORTH_1);

        expectedResponse.addDeletedParameter(toBeDeleted, Region.EU_WEST_1);
        expectedResponse.addDeletedParameter(toBeDeleted, Region.US_EAST_1);
        expectedResponse.addDeletedParameter(toBeDeleted, Region.EU_NORTH_1);

        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder()
                .roleName("AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad")
                .arn("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad")
                .build());
        roles.add(Role.builder()
                .roleName("AWSReservedSSO_Billing_c8106817c1780052")
                .arn("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052")
                .build());
        roles.add(Role.builder()
                .roleName("AWSReservedSSO_AdministratorAccess_f627296b4ac7ac6c")
                .arn("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_AdministratorAccess_f627296b4ac7ac6c")
                .build());

        when(ssmFacadeMock.getAllRegions()).thenReturn(this.regions);
        when(ssmFacadeMock.getParameters(any(Region.class))).thenReturn(parameters);
        when(ssmFacadeMock.putParameter(any(SsmPutParameterRequest.class))).thenReturn(true);
        when(ssmFacadeMock.deleteParameters(any(SsmDeleteParametersRequest.class))).thenReturn(true);


        DistributeSSORolesResponse actualResponse = distributeSSORolesService.handleMonthlyEvent(roles);

        assertEquals(expectedResponse.getParametersCreated(), actualResponse.getParametersCreated());
        assertEquals(expectedResponse.getParametersDeleted(), actualResponse.getParametersDeleted());
    }

    @Test
    void handleCreateRoleEvent_ValidCreateRoleEvent_ShouldPass() {

        when(ssmFacadeMock.putParameter(any(SsmPutParameterRequest.class))).thenReturn(true);

        RoleName roleName = RoleName.create("AWSReservedSSO_test-latest3_58dcaf6a4cfad558");
        CreateRoleEvent createRoleEvent = CreateRoleEvent.create(
                roleName,
                Arn.create("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_test-latest3_58dcaf6a4cfad558"),
                ParameterName.create(environmentVariablesMock.getParameterStorePrefix(), PermissionSetName.create(roleName.toString())));
        DistributeSSORolesResponse expectedResponse = new DistributeSSORolesResponse();

        expectedResponse.addCreatedParameter(createRoleEvent.getParameterName(), this.regions);
        DistributeSSORolesResponse actualResponse = distributeSSORolesService.handleCreateRoleEvent(createRoleEvent);
        assertEquals(expectedResponse.getParametersCreated().get(Region.EU_WEST_1),
                actualResponse.getParametersCreated().get(Region.EU_WEST_1));
        assertEquals(expectedResponse.getParametersCreated().get(Region.US_EAST_1),
                actualResponse.getParametersCreated().get(Region.US_EAST_1));
        assertEquals(expectedResponse.getParametersCreated().get(Region.EU_NORTH_1),
                actualResponse.getParametersCreated().get(Region.EU_NORTH_1));

    }


    @Test
    void handleDeleteRoleEvent_ValidDeleteRoleEvent_ShouldPass() {

        when(ssmFacadeMock.deleteParameter(any(SsmDeleteParameterRequest.class))).thenReturn(true);

        RoleName roleName = RoleName.create("AWSReservedSSO_test-latest3_58dcaf6a4cfad558");
        DeleteRoleEvent deleteRoleEvent = DeleteRoleEvent.create(
                roleName,
                ParameterName.create(environmentVariablesMock.getParameterStorePrefix(),
                        PermissionSetName.create(roleName.toString())));
        DistributeSSORolesResponse expectedResponse = new DistributeSSORolesResponse();

        expectedResponse.addDeletedParameter(deleteRoleEvent.getParameterName(), this.regions);
        DistributeSSORolesResponse actualResponse = distributeSSORolesService.handleDeleteRoleEvent(deleteRoleEvent);
        assertEquals(expectedResponse.getParametersDeleted().get(Region.EU_WEST_1),
                actualResponse.getParametersDeleted().get(Region.EU_WEST_1));
        assertEquals(expectedResponse.getParametersDeleted().get(Region.US_EAST_1),
                actualResponse.getParametersDeleted().get(Region.US_EAST_1));
        assertEquals(expectedResponse.getParametersDeleted().get(Region.EU_NORTH_1),
                actualResponse.getParametersDeleted().get(Region.EU_NORTH_1));

    }

}