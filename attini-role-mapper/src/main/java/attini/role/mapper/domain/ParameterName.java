package attini.role.mapper.domain;

import java.util.Objects;

public class ParameterName implements Comparable<ParameterName> {
    private final String name;

    private ParameterName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static ParameterName create(PermissionSetName permissionSetName) {
        return new ParameterName("/attini/aws-sso-role-names-mapper/" + permissionSetName.toString());
    }

    public static ParameterName create(String value) {
        return new ParameterName(value);
    }
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(ParameterName name1) {
        return this.name.compareTo(name1.toString());
    }
}
