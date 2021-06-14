package attini.role.mapper.domain;

import software.amazon.awssdk.regions.Region;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class DistributeSSORoleArnsLambdaResponse {

    // Olagligt? (förlåt oscar)
    public LinkedHashMap<ParameterName, LinkedHashSet<Region>> parametersCreated = new LinkedHashMap<>();
    public LinkedHashMap<ParameterName, LinkedHashSet<Region>> parametersDeleted = new LinkedHashMap<>();
}
