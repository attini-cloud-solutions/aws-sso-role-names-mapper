package aws.sso.role.names.mapper.facades;

import aws.sso.role.names.mapper.domain.exceptions.ParameterStorePrefixInvalidException;

import java.util.Objects;

public class EnvironmentVariables {
    public String getParameterStorePrefix() {
        String prefix = System.getenv("ParameterStorePrefix");
        if (Objects.isNull(prefix)) {
            throw new ParameterStorePrefixInvalidException("Environment variable \"ParameterStorePrefix\" must be set");
        }
        if (!prefix.startsWith("/") || !prefix.endsWith("/")) {
            throw new ParameterStorePrefixInvalidException("Environment variable \"ParameterStorePrefix\" must start and end with a \"/\" e.g. \"/ParameterPrefix/\"");
        }
        return prefix;
    }
}
