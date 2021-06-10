
package attini.role.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import software.amazon.awssdk.regions.Region;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@QuarkusTest
public class SsmServiceTest {
    
    @Inject
    SsmService ssmService;

    // @BeforeEach
    // public void setup() {
    //     ssmService = new SsmService();
    // }

    /**
     * Not very useful...
     */
    @Test
    public void getAllRegionsTest() {
        List<Region> regions = ssmService.getAllRegions();
        assertNotEquals(new ArrayList<Region>(), regions);
        assertTrue(regions.contains(Region.US_EAST_1));
    }
}
