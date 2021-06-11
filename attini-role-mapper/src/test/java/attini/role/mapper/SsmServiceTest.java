
package attini.role.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.regions.Region.EU_NORTH_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_1;
import static software.amazon.awssdk.regions.Region.EU_WEST_2;
import static software.amazon.awssdk.regions.Region.US_EAST_1;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

@ExtendWith(MockitoExtension.class)
public class SsmServiceTest {

    @Inject
    SsmService ssmService;

    @Mock
    SsmClient ssmClient;

    @Mock
    GetParametersByPathIterable getParametersByPathIterable;

    @BeforeEach
    public void setup() {
        ssmService = new SsmService(ssmClient);
    }

    @Test
    public void getAllRegionsTest() {
        when(ssmClient.getParametersByPathPaginator(any(GetParametersByPathRequest.class)))
                .thenReturn(getParametersByPathIterable);

        GetParametersByPathResponse.Builder responseBuilder = GetParametersByPathResponse.builder();

        Stream<GetParametersByPathResponse> responseStream = Stream.of(
                responseBuilder.parameters(toParam("eu-west-1"),
                                           toParam("eu-north-1")).build(), //då det kan komma olika många regioner i varje paginering är det värt att ha med det i sitt test
                responseBuilder.parameters(toParam("us-east-1")).build());

        when(getParametersByPathIterable.stream()).thenReturn(responseStream);

        List<Region> regions = ssmService.getAllRegions();

        assertNotEquals(new ArrayList<Region>(), regions); // onödigt, behöver inte gämföra med en tom lista, om man vill kan man kolla att listan inte är tom men bättre att kolla att den har rätt size, se nedan
        assertEquals(3,regions.size());
        assertTrue(regions.containsAll(List.of(EU_WEST_1, US_EAST_1, EU_NORTH_1)));
        assertFalse(regions.contains(EU_WEST_2));

    }

    //Lägg till fler test med felhantering

    private static Parameter toParam(String value) {
        return Parameter.builder().value(value).build();
    }
}
