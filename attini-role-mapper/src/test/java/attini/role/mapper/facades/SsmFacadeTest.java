package attini.role.mapper.facades;

import attini.role.mapper.domain.*;
import attini.role.mapper.factories.SsmClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.regions.Region.*;

@ExtendWith(MockitoExtension.class)
public class SsmFacadeTest {

    SsmFacade ssmFacade;

    @Mock
    SsmClientFactory ssmClientFactoryMock;

    @Mock
    SsmClient ssmClientMock;

    @Mock
    GetParametersByPathIterable getParametersByPathIterableMock;

    private static Parameter toParam(String value) {
        return Parameter.builder().value(value).build();
    }

    @BeforeEach
    public void setup() {
        ssmFacade = new SsmFacade(ssmClientFactoryMock);
    }

    @Test
    public void getAllRegions_ValidAPIResponse() {
        when(ssmClientFactoryMock.createGlobalSsmClient()).thenReturn(ssmClientMock);
        when(ssmClientMock.getParametersByPathPaginator(any(GetParametersByPathRequest.class)))
                .thenReturn(getParametersByPathIterableMock);

        GetParametersByPathResponse.Builder responseBuilder = GetParametersByPathResponse.builder();

        Stream<GetParametersByPathResponse> responseStream = Stream.of(
                responseBuilder.parameters(toParam("eu-west-1"),
                        toParam("eu-north-1")).build(),
                responseBuilder.parameters(toParam("us-east-1")).build());

        when(getParametersByPathIterableMock.stream()).thenReturn(responseStream);

        Set<Region> regions = ssmFacade.getAllRegions();

        assertEquals(3, regions.size());
        assertTrue(regions.containsAll(List.of(EU_WEST_1, US_EAST_1, EU_NORTH_1)));
        assertFalse(regions.contains(EU_WEST_2));
    }

    @Test
    public void getParameters_SsmError_ShouldReturnEmptySet() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.getParametersByPathPaginator(any(GetParametersByPathRequest.class)))
                .thenReturn(getParametersByPathIterableMock);
        when(getParametersByPathIterableMock.stream()).thenThrow(SsmException.builder().build());

        // Irrelevant what Region is used.
        assertTrue(ssmFacade.getParameters(Region.AP_NORTHEAST_3).isEmpty());
    }

    @Test
    public void deleteParameters_SsmError_ShouldReturnFalse() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.deleteParameters(any(DeleteParametersRequest.class))).thenThrow(SsmException.class);

        // Irrelevant what Region or Set is used.
        assertFalse(ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(Region.EU_WEST_3, new HashSet<>())));
    }

    @Test
    public void deleteParameters_SsmError_ShouldReturnTrue() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.deleteParameters(any(DeleteParametersRequest.class))).thenReturn(DeleteParametersResponse.builder().build());

        // Irrelevant what Region or Set is used.
        assertTrue(ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(Region.EU_WEST_3, new HashSet<>())));
    }

    @Test
    public void putParameters_SsmError_ShouldReturnFalse() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.putParameter(any(PutParameterRequest.class))).thenThrow(SsmException.class);

        // Irrelevant what arguments are used.
        assertFalse(ssmFacade.putParameter(SsmPutParameterRequest.create(
                Region.EU_NORTH_1,
                ParameterName.create("/attini/aws-sso-role-names-mapper/AdministratorAccess"),
                PermissionSetName.create("AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad"),
                Arn.create("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052"))));
    }

    @Test
    public void putParameters_SsmError_ShouldReturnTrue() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.putParameter(any(PutParameterRequest.class))).thenReturn(PutParameterResponse.builder().build());

        // Irrelevant what arguments are used.
        assertTrue(ssmFacade.putParameter(SsmPutParameterRequest.create(
                Region.EU_NORTH_1,
                ParameterName.create("/attini/aws-sso-role-names-mapper/AdministratorAccess"),
                PermissionSetName.create("AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad"),
                Arn.create("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052"))));
    }
}
