package attini.role.mapper.domain;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class SsmDeleteParametersRequest {
    private final Region region;
    private final List<ParameterName> parameterNames;

    public SsmDeleteParametersRequest(Region region, List<ParameterName> parameterNames) {
        this.region = requireNonNull(region);
        this.parameterNames = requireNonNull(parameterNames);
    }


    public Region getRegion() {
        return region;
    }

    public List<ParameterName> getParameterNames() {
        return parameterNames;
    }

    public static SsmDeleteParametersRequest create(List<Parameter> parameters, Region region) {
        return new SsmDeleteParametersRequest(region, parameters.stream().map(parameter -> ParameterName.create(parameter.name())).collect(Collectors.toList()));
    }

    public static SsmDeleteParametersRequest create(Region region, List<ParameterName> parameterNames) {
        return new SsmDeleteParametersRequest(region, parameterNames);
    }


}
