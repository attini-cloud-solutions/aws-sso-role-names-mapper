package aws.sso.role.names.mapper.facades;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.regions.Region.EU_NORTH_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_2;
import static software.amazon.awssdk.regions.Region.EU_WEST_3;
import static software.amazon.awssdk.regions.Region.US_EAST_1;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import aws.sso.role.names.mapper.domain.Arn;
import aws.sso.role.names.mapper.domain.ParameterName;
import aws.sso.role.names.mapper.domain.ParameterStorePrefix;
import aws.sso.role.names.mapper.domain.PermissionSetName;
import aws.sso.role.names.mapper.domain.SsmDeleteParametersRequest;
import aws.sso.role.names.mapper.domain.SsmPutParameterRequest;
import aws.sso.role.names.mapper.domain.exceptions.CouldNotGetParametersException;
import aws.sso.role.names.mapper.factories.SsmClientFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteParametersRequest;
import software.amazon.awssdk.services.ssm.model.DeleteParametersResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

@ExtendWith(MockitoExtension.class)
public class SsmFacadeTest {

    SsmFacade ssmFacade;

    @Mock
    SsmClientFactory ssmClientFactoryMock;

    @Mock
    SsmClient ssmClientMock;

    @Mock
    EnvironmentVariables environmentVariablesMock;

    @Mock
    GetParametersByPathIterable getParametersByPathIterableMock;

    private static Parameter toParam(String value) {
        return Parameter.builder().value(value).build();
    }

    public SsmFacadeTest() {
        environmentVariablesMock = mock(EnvironmentVariables.class);
    }

    @BeforeEach
    void setup() {
        ssmFacade = new SsmFacade(ssmClientFactoryMock, environmentVariablesMock);
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
    public void getParameters_SsmError_ShouldThrowCouldNotGetParametersException() {
        when(environmentVariablesMock.getParameterStorePrefix()).thenReturn(ParameterStorePrefix.create("/test/"));
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.getParametersByPathPaginator(any(GetParametersByPathRequest.class)))
                .thenReturn(getParametersByPathIterableMock);
        when(getParametersByPathIterableMock.stream()).thenThrow(SsmException.builder().build());

        // Irrelevant what Region is used.
        assertThrows(CouldNotGetParametersException.class,()-> ssmFacade.getParameters(Region.AP_NORTHEAST_3));
    }

    @Test
    public void deleteParameters_SsmError_ShouldReturnEmptySet() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.deleteParameters(any(DeleteParametersRequest.class))).thenThrow(SsmException.class);

        // Irrelevant what Region or Set is used.
        assertTrue(ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(Region.EU_WEST_3, emptySet()))
                            .isEmpty());
    }

    @Test
    public void deleteParameters_emptyResponse_ShouldReturnEmptySet() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.deleteParameters(any(DeleteParametersRequest.class))).thenReturn(DeleteParametersResponse.builder()
                                                                                                                    .build());

        // Irrelevant what Region or Set is used.
        assertTrue(ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(EU_WEST_3, emptySet())).isEmpty());
    }

    @Test
    public void deleteParameters_EmptyRequest_ShouldReturnEmptySet() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.deleteParameters(DeleteParametersRequest.builder().names(emptySet()).build()))
                .thenThrow(SsmException.class);
        assertTrue(ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(EU_WEST_3, emptySet())).isEmpty());
    }

    @Test
    public void deleteParameters_ValidRequest_ShouldReturnDeletedParameter() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        Set<String> names = Set.of("name1", "name2", "name3");
        when(ssmClientMock.deleteParameters(any(DeleteParametersRequest.class)))
                .thenReturn(DeleteParametersResponse.builder().deletedParameters(names).build());

        Set<ParameterName> params = names.stream().map(ParameterName::create).collect(toSet());
        Set<ParameterName> deletedParameters = ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(EU_WEST_3,
                                                                                                            params));
        assertFalse(deletedParameters.isEmpty());
        assertEquals(params, deletedParameters);
    }

    @Test
    public void putParameter_SsmError_ShouldReturnFalse() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.putParameter(any(PutParameterRequest.class))).thenThrow(SsmException.class);

        // Irrelevant what arguments are used.
        assertFalse(ssmFacade.putParameter(SsmPutParameterRequest.create(
                Region.EU_NORTH_1,
                ParameterName.create("/attini/aws-sso-role-names-mapper/AdministratorAccess"),
                PermissionSetName.create("AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad"),
                Arn.create(
                        "arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052"))));
    }

    @Test
    public void putParameter_SsmError_ShouldReturnTrue() {
        when(ssmClientFactoryMock.createSsmClient(any(Region.class))).thenReturn(ssmClientMock);
        when(ssmClientMock.putParameter(any(PutParameterRequest.class))).thenReturn(PutParameterResponse.builder()
                                                                                                        .build());

        // Irrelevant what arguments are used.
        assertTrue(ssmFacade.putParameter(SsmPutParameterRequest.create(
                Region.EU_NORTH_1,
                ParameterName.create("/attini/aws-sso-role-names-mapper/AdministratorAccess"),
                PermissionSetName.create("AWSReservedSSO_DatabaseAdministrator_e90c045f34e6a0ad"),
                Arn.create(
                        "arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_Billing_c8106817c1780052"))));
    }
}
