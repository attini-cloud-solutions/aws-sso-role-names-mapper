package aws.sso.role.names.mapper.facades;

import aws.sso.role.names.mapper.domain.ParameterStorePrefix;

public class EnvironmentVariables {
    public ParameterStorePrefix getParameterStorePrefix() {
        return ParameterStorePrefix.create(System.getenv("PARAMETER_STORE_PREFIX"));
    }
}
