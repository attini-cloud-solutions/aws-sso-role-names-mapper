package attini.role.mapper.factories;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

public class SsmClientFactory {
    public SsmClient createSsmClient(Region region) {
        return SsmClient.builder().region(region).httpClient(UrlConnectionHttpClient.create()).build();
    }

    //TODO kommer denna hämta globalt eller kommer den ta default regionen från nuvarande profil?
    public SsmClient createGlobalSsmClient() {
        return SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }
}
