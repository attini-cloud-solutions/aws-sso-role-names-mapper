package aws.sso.role.names.mapper.domain;

import software.amazon.awssdk.regions.Region;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DistributeSSORolesResponse {

    private final HashMap<Region, TreeSet<String>> parametersCreated = new HashMap<>();
    private final HashMap<Region, TreeSet<String>> parametersDeleted = new HashMap<>();

    public HashMap<Region, TreeSet<String>> getParametersCreated() {
        return parametersCreated;
    }

    public HashMap<Region, TreeSet<String>> getParametersDeleted() {
        return parametersDeleted;
    }


    public void addCreatedParameter(ParameterName parameterName, Region region) {
        parametersCreated.computeIfAbsent(region, p -> new TreeSet<>()).add(parameterName.getName());
    }

    public void addDeletedParameter(ParameterName parameterName, Region region) {
        parametersDeleted.computeIfAbsent(region, p -> new TreeSet<>()).add(parameterName.getName());
    }

    public void addCreatedParameter(ParameterName parameterName, Set<Region> regions) {
        regions.forEach(region -> parametersCreated.computeIfAbsent(region, p -> new TreeSet<>()).add(parameterName.getName()));
    }

    public void addDeletedParameter(ParameterName parameterName, Set<Region> regions) {
        regions.forEach(region -> parametersDeleted.computeIfAbsent(region, p -> new TreeSet<>()).add(parameterName.getName()));
    }

    public void addCreatedParameters(Set<ParameterName> parameterNames, Region region) {
        parametersCreated.putIfAbsent(region, new TreeSet<>(parameterNames.stream()
                .map(ParameterName::getName)
                .collect(Collectors.toSet())));
    }

    public void addDeletedParameters(Set<ParameterName> parameterNames, Region region) {
        parametersDeleted.putIfAbsent(region, new TreeSet<>(parameterNames.stream()
                .map(ParameterName::getName)
                .collect(Collectors.toSet())));
    }


}