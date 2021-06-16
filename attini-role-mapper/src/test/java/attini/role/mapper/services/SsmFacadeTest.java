
package attini.role.mapper.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.regions.Region.EU_NORTH_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_2;
import static software.amazon.awssdk.regions.Region.US_EAST_1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import attini.role.mapper.domain.ParameterName;
import attini.role.mapper.domain.SsmDeleteParametersRequest;
import attini.role.mapper.facades.SsmFacade;
import attini.role.mapper.factories.SsmClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.DeleteParametersRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;
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
    GetParametersByPathIterable getParametersByPathIterableMock;

    @BeforeEach
    public void setup() {
        ssmFacade = new SsmFacade(ssmClientFactoryMock);
    }

    @Test
    public void getAllRegions_ValidAPIResponse_ShouldPass() {
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

        assertEquals(3,regions.size());
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
        when(ssmClientMock.deleteParameters(any(DeleteParametersRequest.class))).thenThrow(SsmException.builder().build());

        // Irrelevant what Region or Set is used.
        assertFalse(ssmFacade.deleteParameters(SsmDeleteParametersRequest.create(Region.EU_WEST_3, new HashSet<>())));
    }

    private static Parameter toParam(String value) {
        return Parameter.builder().value(value).build();
    }
}
