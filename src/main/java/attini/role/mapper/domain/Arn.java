package attini.role.mapper.domain;

import java.util.Objects;

public class Arn {
    private final String arn;

    private Arn(String value) {
        this.arn = Objects.requireNonNull(value, "arn");
    }

    public static Arn create(String value) {
        return new Arn(value);
    }

    @Override
    public String toString() {
        return arn;
    }
}
