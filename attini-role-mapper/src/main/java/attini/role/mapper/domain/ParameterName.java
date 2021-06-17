package attini.role.mapper.domain;

import java.util.Objects;

public class ParameterName implements Comparable<ParameterName> {
    private final String name;

    private ParameterName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }


    //TODO "/attini/aws-sso-role-names-mapper/" vara en system-variabel (som sätts o cloudformation)så att en användare kan döpa den till vad dom vill
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
