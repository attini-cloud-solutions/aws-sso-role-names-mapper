package attini.role.mapper.domain;

import java.util.Objects;

public class RoleName {
    private final String name;

    private RoleName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static RoleName create(String value) {
        return new RoleName(value);
    }

    @Override
    public String toString() {
        return name;
    }
}