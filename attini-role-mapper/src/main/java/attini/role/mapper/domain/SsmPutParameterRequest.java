package attini.role.mapper.domain;

import software.amazon.awssdk.regions.Region;

import static java.util.Objects.requireNonNull;

public class SsmPutParameterRequest {
    private final Region region;
    private final ParameterName parameterName;
    private final PermissionSetName permissionSetName;

    public SsmPutParameterRequest(Region region, ParameterName parameterName, PermissionSetName permissionSetName) {
        this.region = requireNonNull(region);
        this.parameterName = requireNonNull(parameterName);
        this.permissionSetName = requireNonNull(permissionSetName);
    }

    public Region getRegion() {
        return region;
    }

    public ParameterName getParameterName() {
        return parameterName;
    }

    public PermissionSetName getPermissionSetName() {
        return permissionSetName;
    }

    public static SsmPutParameterRequest create(Region region, ParameterName parameterName, PermissionSetName permissionSetName) {
        return new SsmPutParameterRequest(region, parameterName, permissionSetName);
    }

}