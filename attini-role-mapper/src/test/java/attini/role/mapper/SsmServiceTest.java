
package attini.role.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import software.amazon.awssdk.services.ssm.model.*;
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

    /**
     *
     */
    @Test
    public void getAllRegionsTest() {
        Mockito.when(ssmClient.getParametersByPathPaginator(Mockito.any(GetParametersByPathRequest.class)))
            .thenReturn(getParametersByPathIterable);
        GetParametersByPathResponse.Builder responseBuilder = GetParametersByPathResponse.builder();
        //responseBuilder.....
        //responseBuilder.parameters();

        Parameter.Builder paramBuilder = Parameter.builder();

        Stream<GetParametersByPathResponse> s = Stream.of(
                (responseBuilder.parameters(paramBuilder.value("eu-west-1").build()).build()),
                (responseBuilder.parameters(paramBuilder.value("us-east-1").build()).build()));

        Mockito.when(getParametersByPathIterable.stream()).thenReturn(s);


        List<Region> regions = ssmService.getAllRegions();
        assertNotEquals(new ArrayList<Region>(), regions);
        assertTrue(regions.contains(Region.EU_WEST_1) && regions.contains(Region.US_EAST_1));
        assertFalse(regions.contains(Region.EU_WEST_2));

    }
}
