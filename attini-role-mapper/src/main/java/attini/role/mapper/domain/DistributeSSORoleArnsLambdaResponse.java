package attini.role.mapper.domain;

import software.amazon.awssdk.regions.Region;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class DistributeSSORoleArnsLambdaResponse {

    // Olagligt? (förlåt oscar)
    private final LinkedHashMap<ParameterName, LinkedHashSet<Region>> parametersCreated = new LinkedHashMap<>();
    private final LinkedHashMap<ParameterName, LinkedHashSet<Region>> parametersDeleted = new LinkedHashMap<>();


//    {
//        "created": {"param1": ['region2'], "param2": []}
//        "deleted": {}
//    }

    public void addRegionToCreatedParameter(ParameterName parameterName, Region region) {
        parametersCreated.computeIfAbsent(parameterName, p -> new LinkedHashSet<>()).add(region);
    }
    public void addRegionToDeletedParameter(ParameterName parameterName, Region region) {
        parametersDeleted.computeIfAbsent(parameterName, p -> new LinkedHashSet<>()).add(region);
    }
}