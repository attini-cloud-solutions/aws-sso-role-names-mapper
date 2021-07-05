package aws.sso.role.names.mapper.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.Parameter;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SsmDeleteParametersRequest that = (SsmDeleteParametersRequest) o;
        return Objects.equals(region, that.region) && Objects.equals(parameterNames,
                                                                     that.parameterNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, parameterNames);
    }
}
