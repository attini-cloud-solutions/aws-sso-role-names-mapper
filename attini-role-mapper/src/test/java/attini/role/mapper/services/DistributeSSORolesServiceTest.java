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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributeSSORolesServiceTest {

    private DistributeSSORolesService distributeSSORolesService;

    private IamFacade iamFacade;

    @Mock
    private SsmFacade ssmFacade;

    @BeforeEach
    void setUp() {
        distributeSSORolesService = new DistributeSSORolesService(iamFacade, ssmFacade);
    }


    @Disabled
    @Test
    void monthlyCleanup_AllRolesHaveParameters_ShouldPass() {
        DistributeSSORolesResponse expectedResponse = new DistributeSSORolesResponse();
        ParameterName database = ParameterName.create("/attini/aws-sso-role-names-mapper/DatabaseAdministrator");
        ParameterName billing = ParameterName.create("/attini/aws-sso-role-names-mapper/Billing");
        ParameterName admin = ParameterName.create("/attini/aws-sso-role-names-mapper/AdministratorAccess");
        ParameterName toBeDeleted = ParameterName.create("/attini/aws-sso-role-names-mapper/ToBeDeleted");

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
        Set<Region> regions = new HashSet<>();
        regions.add(Region.EU_WEST_1);
        regions.add(Region.US_EAST_1);
        regions.add(Region.EU_NORTH_1);

        when(ssmFacade.getAllRegions()).thenReturn(regions);

        DistributeSSORolesResponse actualResponse = distributeSSORolesService.monthlyCleanup(roles);

        assertEquals(expectedResponse, actualResponse);
    }
}