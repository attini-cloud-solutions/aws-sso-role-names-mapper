package aws.sso.role.names.mapper.domain;

import java.util.Objects;

public class RoleName {
    private final String name;

    private RoleName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static RoleName create(String value) {
        return new RoleName(value);
    }

    public String getName() {
        return name;
    }
}