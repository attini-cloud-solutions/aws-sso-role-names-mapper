package aws.sso.role.names.mapper.domain;

import aws.sso.role.names.mapper.domain.exceptions.ParameterStorePrefixInvalidException;

import java.util.Objects;

public class ParameterStorePrefix {
    private final String prefix;

    private ParameterStorePrefix(String value) {
        if (Objects.isNull(value)) {
            throw new ParameterStorePrefixInvalidException("Environment variable \"ParameterStorePrefix\" must be set");
        }
        if (!value.startsWith("/") || !value.endsWith("/")) {
            throw new ParameterStorePrefixInvalidException("Environment variable \"ParameterStorePrefix\" must start and end with a \"/\" e.g. \"/ParameterPrefix/\"");
        }
        this.prefix = value;
    }

    public static ParameterStorePrefix create(String value) {
        return new ParameterStorePrefix(value);
    }

    public String getPrefix() {
        return prefix;
    }
}
