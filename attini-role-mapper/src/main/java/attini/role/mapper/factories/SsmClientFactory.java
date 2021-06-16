package attini.role.mapper.factories;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

public class SsmClientFactory {
    public SsmClient createSsmClient(Region region) {
        return SsmClient.builder().region(region).httpClient(UrlConnectionHttpClient.create()).build();
    }
    public SsmClient createGlobalSsmClient() {
        return SsmClient.builder().region(Region.AWS_GLOBAL).httpClient(UrlConnectionHttpClient.create()).build();
    }
}
