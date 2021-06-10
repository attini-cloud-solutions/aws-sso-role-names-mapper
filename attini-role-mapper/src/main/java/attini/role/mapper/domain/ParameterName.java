package attini.role.mapper.domain;

import java.util.Objects;

public class ParameterName {
    private final String name;

    private ParameterName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static ParameterName create(PermissionSetName permissionSetName) {
        return new ParameterName("/SSORoleArns/" + permissionSetName);
    }

    public String getName() {
        return name;
    }
}
