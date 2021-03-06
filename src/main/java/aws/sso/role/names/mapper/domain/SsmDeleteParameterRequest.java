package aws.sso.role.names.mapper.domain;

import software.amazon.awssdk.regions.Region;

import static java.util.Objects.requireNonNull;

public class SsmDeleteParameterRequest {
    private final Region region;
    private final ParameterName parameterName;

    public SsmDeleteParameterRequest(Region region, ParameterName parameterName) {
        this.region = requireNonNull(region, "region");
        this.parameterName = requireNonNull(parameterName, "parameterName");
    }

    public static SsmDeleteParameterRequest create(Region region, ParameterName parameterName) {
        return new SsmDeleteParameterRequest(region, parameterName);
    }

    public Region getRegion() {
        return region;
    }

    public ParameterName getParameterName() {
        return parameterName;
    }
}
