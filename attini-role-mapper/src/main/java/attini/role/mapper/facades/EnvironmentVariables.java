package attini.role.mapper.facades;

import attini.role.mapper.domain.exceptions.ParameterStorePrefixMissingException;

import java.util.Objects;

public class EnvironmentVariables {
    public String getParameterStorePrefix() {
        String prefix = System.getenv("ParameterStorePrefix");
        if (Objects.isNull(prefix)) {
            throw new ParameterStorePrefixMissingException("Environment variable \"ParameterStorePrefix\" must be set");
        }
        return prefix;
    }
}
