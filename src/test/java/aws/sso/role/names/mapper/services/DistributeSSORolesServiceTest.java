package aws.sso.role.names.mapper.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import aws.sso.role.names.mapper.domain.Arn;
import aws.sso.role.names.mapper.domain.CreateRoleEvent;
import aws.sso.role.names.mapper.domain.DeleteRoleEvent;
import aws.sso.role.names.mapper.domain.DistributeSSORolesResponse;
import aws.sso.role.names.mapper.domain.ParameterName;
import aws.sso.role.names.mapper.domain.ParameterStorePrefix;
import aws.sso.role.names.mapper.domain.PermissionSetName;
import aws.sso.role.names.mapper.domain.RoleName;
import aws.sso.role.names.mapper.domain.SsmDeleteParameterRequest;
import aws.sso.role.names.mapper.domain.SsmDeleteParametersRequest;
import aws.sso.role.names.mapper.domain.SsmPutParameterRequest;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import aws.sso.role.names.mapper.facades.SsmFacade;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.ssm.model.Parameter;

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
        when(environmentVariablesMock.getParameterStorePrefix()).thenReturn(ParameterStorePrefix.create("/test/"));
        distributeSSORolesService = new DistributeSSORolesService(ssmFacadeMock, environmentVariablesMock);
    }


    @Test
    void handleScheduledEvent_AllRolesHaveParameters() {
        DistributeSSORolesResponse expectedResponse = new DistributeSSORolesResponse();
        ParameterStorePrefix prefix = environmentVariablesMock.getParameterStorePrefix();
        ParameterName database = ParameterName.create(prefix,
                                                      PermissionSetName.create(
                                                              "AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad"));
        ParameterName billing = ParameterName.create(prefix,
                                                     PermissionSetName.create("AWSReservedSSO_Billing_c8106817c1780052"));
        ParameterName admin = ParameterName.create(prefix,
                                                   PermissionSetName.create(
                                                           "AWSReservedSSO_AdministratorAccess_f627296b4ac7ac6c"));
        ParameterName toBeDeleted = ParameterName.create(prefix,
                                                         PermissionSetName.create(
                                                                 "AWSReservedSSO_DeleteMePlease_f627296b4ac7ac6c"));

        Parameter databaseParameter = Parameter.builder()
                                               .name(database.getName())
                                               .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad")
                                               .build();
        Parameter billingParameter = Parameter.builder()
                                              .name(billing.getName())
                                              .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052")
                                              .build();
        Parameter adminParameter = Parameter.builder()
                                            .name(admin.getName())
                                            .value("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_AdministratorAccess_f627296b4ac7ac6c")
                                            .build();
        Parameter toBeDeletedParameter = Parameter.builder()
                                                  .name(toBeDeleted.getName())
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
        when(ssmFacadeMock.deleteParameters(any(SsmDeleteParametersRequest.class)))
                .thenReturn(Set.of(ParameterName.create(toBeDeleted.getName())));


        DistributeSSORolesResponse actualResponse = distributeSSORolesService.handleScheduledEvent(roles);

        assertEquals(expectedResponse.getParametersCreated(), actualResponse.getParametersCreated());
        assertEquals(expectedResponse.getParametersDeleted(), actualResponse.getParametersDeleted());
    }

    @Test
    void handleCreateRoleEvent_ValidCreateRoleEvent() {

        when(ssmFacadeMock.putParameter(any(SsmPutParameterRequest.class))).thenReturn(true);

        RoleName roleName = RoleName.create("AWSReservedSSO_test-latest3_58dcaf6a4cfad558");
        CreateRoleEvent createRoleEvent = CreateRoleEvent.create(
                roleName,
                Arn.create(
                        "arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_test-latest3_58dcaf6a4cfad558"),
                ParameterName.create(environmentVariablesMock.getParameterStorePrefix(),
                                     PermissionSetName.create(roleName.getName())));
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
    void handleDeleteRoleEvent_ValidDeleteRoleEvent() {

        when(ssmFacadeMock.deleteParameter(any(SsmDeleteParameterRequest.class))).thenReturn(true);

        RoleName roleName = RoleName.create("AWSReservedSSO_test-latest3_58dcaf6a4cfad558");
        DeleteRoleEvent deleteRoleEvent = DeleteRoleEvent.create(
                roleName,
                ParameterName.create(environmentVariablesMock.getParameterStorePrefix(),
                                     PermissionSetName.create(roleName.getName())));
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


    @Test
    void handleDeleteAllRolesEvent() {
        Parameter.Builder parameterBuilder = Parameter.builder().value("122");
        Set<Parameter> euWest1Params = Set.of(parameterBuilder.name(environmentVariablesMock.getParameterStorePrefix() + "west1-1")
                                                              .build(),
                                              parameterBuilder.name(environmentVariablesMock.getParameterStorePrefix()+"west1-2").build());
        Set<Parameter> euWest2Params = Set.of(parameterBuilder.name(environmentVariablesMock.getParameterStorePrefix()+"west2-1").build());
        when(ssmFacadeMock.getAllRegions()).thenReturn(Set.of(Region.EU_WEST_1, Region.EU_WEST_2));

        when(ssmFacadeMock.getParameters(Region.EU_WEST_1)).thenReturn(euWest1Params);
        when(ssmFacadeMock.getParameters(Region.EU_WEST_2)).thenReturn(euWest2Params);
        when(ssmFacadeMock.deleteParameters(SsmDeleteParametersRequest.create(euWest1Params, Region.EU_WEST_1)))
                .thenReturn(euWest1Params.stream().map(parameter -> ParameterName.create(parameter.name())).collect(
                        Collectors.toSet()));
        when(ssmFacadeMock.deleteParameters(SsmDeleteParametersRequest.create(euWest2Params, Region.EU_WEST_2)))
                .thenReturn(euWest2Params.stream().map(parameter -> ParameterName.create(parameter.name())).collect(
                        Collectors.toSet()));

        DistributeSSORolesResponse distributeSSORolesResponse = distributeSSORolesService.handleDeleteAllRolesEvent();
        assertEquals(2, distributeSSORolesResponse.getParametersDeleted().get(Region.EU_WEST_1).size());
        assertEquals(1, distributeSSORolesResponse.getParametersDeleted().get(Region.EU_WEST_2).size());

    }

}