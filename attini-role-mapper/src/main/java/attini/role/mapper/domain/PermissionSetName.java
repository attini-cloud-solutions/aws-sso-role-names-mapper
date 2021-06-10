package attini.role.mapper.domain;

import java.util.Objects;

public class PermissionSetName {
    private final String name;

    private PermissionSetName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static PermissionSetName create(String value) {
        return new PermissionSetName(value);
    }

    public String getName() {
        return name;
    }
}
