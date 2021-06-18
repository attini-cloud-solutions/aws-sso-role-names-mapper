package attini.role.mapper.facades;

public class EnvironmentVariables {
    public String getParameterStorePrefix() {
        // TODO: Kasta exception om inte satt
        return System.getenv("ParameterStorePrefix");
    }
}
