package aws.sso.role.names.mapper.domain;

import java.util.Objects;

public class ParameterName implements Comparable<ParameterName> {
    private final String name;

    private ParameterName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static ParameterName create(ParameterStorePrefix prefix, PermissionSetName permissionSetName) {
        return new ParameterName(prefix.getPrefix() + permissionSetName.getName());
    }

    public static ParameterName create(String value) {
        return new ParameterName(value);
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ParameterName name1) {
        return this.name.compareTo(name1.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterName that = (ParameterName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
