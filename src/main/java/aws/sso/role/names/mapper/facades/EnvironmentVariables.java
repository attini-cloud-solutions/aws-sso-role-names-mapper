package aws.sso.role.names.mapper.facades;

import aws.sso.role.names.mapper.domain.ParameterStorePrefix;
import aws.sso.role.names.mapper.domain.exceptions.ParameterStorePrefixInvalidException;

import java.util.Objects;

public class EnvironmentVariables {
    public ParameterStorePrefix getParameterStorePrefix() {
        return ParameterStorePrefix.create(System.getenv("ParameterStorePrefix"));
    }
}
