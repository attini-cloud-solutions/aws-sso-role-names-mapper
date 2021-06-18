package attini.role.mapper.domain;

import software.amazon.awssdk.regions.Region;

import static java.util.Objects.requireNonNull;

public class SsmPutParameterRequest {
    private final Region region;
    private final ParameterName parameterName;
    private final PermissionSetName permissionSetName;
    // The value of a parameter
    private final Arn iamRoleArn;

    public SsmPutParameterRequest(Region region, ParameterName parameterName, PermissionSetName permissionSetName, Arn arn) {
        this.region = requireNonNull(region);
        this.parameterName = requireNonNull(parameterName);
        this.permissionSetName = requireNonNull(permissionSetName);
        this.iamRoleArn = requireNonNull(arn);
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

    public Arn getIamRoleArn() {
        return iamRoleArn;
    }


    public static SsmPutParameterRequest create(Region region, ParameterName parameterName, PermissionSetName permissionSetName, Arn arn) {
        return new SsmPutParameterRequest(region, parameterName, permissionSetName, arn);
    }

}