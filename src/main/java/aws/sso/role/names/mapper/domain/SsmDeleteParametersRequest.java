package aws.sso.role.names.mapper.domain;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class SsmDeleteParametersRequest {
    private final Region region;
    private final Set<ParameterName> parameterNames;

    public SsmDeleteParametersRequest(Region region, Set<ParameterName> parameterNames) {
        this.region = requireNonNull(region, "region");
        this.parameterNames = requireNonNull(parameterNames, "parameterNames");
    }

    public static SsmDeleteParametersRequest create(Set<Parameter> parameters, Region region) {
        return new SsmDeleteParametersRequest(region, parameters.stream().map(parameter -> ParameterName.create(parameter.name())).collect(Collectors.toSet()));
    }

    public static SsmDeleteParametersRequest create(Region region, Set<ParameterName> parameterNames) {
        return new SsmDeleteParametersRequest(region, parameterNames);
    }

    public Region getRegion() {
        return region;
    }

    public Set<ParameterName> getParameterNames() {
        return parameterNames;
    }
}
