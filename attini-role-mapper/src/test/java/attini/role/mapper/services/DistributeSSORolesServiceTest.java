package attini.role.mapper.services;

import attini.role.mapper.domain.DistributeSSORolesResponse;
import attini.role.mapper.domain.ParameterName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Role;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributeSSORolesServiceTest {

    private DistributeSSORolesService distributeSSORolesService;
    @Mock
    private IamService iamService;

    @Mock
    private SsmService ssmService;

    @BeforeEach
    void setUp() {
        distributeSSORolesService = new DistributeSSORolesService(iamService, ssmService);
    }


    @Disabled
    @Test
    void monthlyCleanup_AllRolesHaveParameters_ShouldPass() {
        DistributeSSORolesResponse expectedResponse = new DistributeSSORolesResponse();
        ParameterName database = ParameterName.create("/attini/aws-sso-role-names-mapper/DatabaseAdministrator");
        ParameterName billing = ParameterName.create("/attini/aws-sso-role-names-mapper/Billing");
        ParameterName admin = ParameterName.create("/attini/aws-sso-role-names-mapper/AdministratorAccess");
        ParameterName toBeDeleted = ParameterName.create("/attini/aws-sso-role-names-mapper/ToBeDeleted");

        expectedResponse.addRegionToCreatedParameter(database, Region.EU_WEST_1);
        expectedResponse.addRegionToCreatedParameter(database, Region.US_EAST_1);
        expectedResponse.addRegionToCreatedParameter(database, Region.EU_NORTH_1);
        expectedResponse.addRegionToCreatedParameter(billing, Region.EU_WEST_1);
        expectedResponse.addRegionToCreatedParameter(billing, Region.US_EAST_1);
        expectedResponse.addRegionToCreatedParameter(billing, Region.EU_NORTH_1);
        expectedResponse.addRegionToCreatedParameter(admin, Region.EU_WEST_1);
        expectedResponse.addRegionToCreatedParameter(admin, Region.US_EAST_1);
        expectedResponse.addRegionToCreatedParameter(admin, Region.EU_NORTH_1);
        expectedResponse.addRegionToDeletedParameter(toBeDeleted, Region.EU_WEST_1);
        expectedResponse.addRegionToDeletedParameter(toBeDeleted, Region.US_EAST_1);
        expectedResponse.addRegionToDeletedParameter(toBeDeleted, Region.EU_NORTH_1);

        List<Role> roles = new ArrayList<>();
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
        List<Region> regions = new ArrayList<>();
        regions.add(Region.EU_WEST_1);
        regions.add(Region.US_EAST_1);
        regions.add(Region.EU_NORTH_1);

        when(iamService.listAllRoles()).thenReturn(roles);
        when(ssmService.getAllRegions()).thenReturn(regions);

        DistributeSSORolesResponse actualResponse = distributeSSORolesService.monthlyCleanup();

        assertEquals(expectedResponse, actualResponse);
    }
}