package attini.role.mapper;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

public class SsmService {
    /**
     * Get all regions from /aws/service/global-infrastructure/regions
     * Ignores CN/gov regions.
     * Currently requires region to exist in both Region.regions and in /aws/service/global-infrastructure/regions.
     */
    public List<Region> getAllRegions() {
        SsmClient ssmClient = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build();

        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions"); // /aws/service/global-infrastructure/services/ssm/regions
        GetParametersByPathIterable iterable = ssmClient.getParametersByPathPaginator(requestBuilder.build());

        ArrayList<Parameter> parameters = new ArrayList<Parameter>();

        for(GetParametersByPathResponse response : iterable) {
            parameters.addAll(response.parameters());
        }

        ArrayList<Region> regions = new ArrayList<Region>();

        for(Parameter region1 : parameters) {
            String region = region1.value();
            for(Region region2 : Region.regions()) {
                if(region.equals(region2.toString()) && !region.contains("-gov-") && !region.contains("cn-")) {
                    regions.add(region2);
                    break;
                }
            }
        }
        return regions;
    }
}
