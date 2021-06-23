package aws.sso.role.names.mapper.domain;

import java.util.Objects;

public class PermissionSetName {
    private final String name;

    private PermissionSetName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static PermissionSetName create(String value) {
        try {
            return new PermissionSetName(value.trim().split("_")[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error splitting value on '_' into PermissionSetName.", e);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
