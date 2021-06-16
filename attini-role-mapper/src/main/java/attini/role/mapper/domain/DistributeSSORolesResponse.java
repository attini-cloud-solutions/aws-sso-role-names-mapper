package attini.role.mapper.domain;

import com.sun.source.tree.Tree;
import org.jboss.logging.annotations.Param;
import software.amazon.awssdk.regions.Region;

import java.util.*;

public class DistributeSSORolesResponse {

    private final HashMap<Region, TreeSet<ParameterName>> parametersCreated = new HashMap<>();
    private final HashMap<Region, TreeSet<ParameterName>> parametersDeleted = new HashMap<>();


//    {
//        "created": {"param1": ['region2'], "param2": []}
//        "deleted": {}
//    }

    public void addCreatedParameter(ParameterName parameterName, Region region) {
        parametersCreated.computeIfAbsent(region, p -> new TreeSet<>()).add(parameterName);
    }

    public void addDeletedParameter(ParameterName parameterName, Region region) {
        parametersDeleted.computeIfAbsent(region, p -> new TreeSet<>()).add(parameterName);
    }

    public void addCreatedParameters(Set<ParameterName> parameterNames, Region region) {
        parametersCreated.putIfAbsent(region, new TreeSet<>(parameterNames));
    }

    public void addDeletedParameters(Set<ParameterName> parameterNames, Region region) {
        parametersDeleted.putIfAbsent(region, new TreeSet<>(parameterNames));
    }
}