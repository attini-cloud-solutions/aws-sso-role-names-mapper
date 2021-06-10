package attini.role.mapper;

import javax.enterprise.context.ApplicationScoped;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;


@ApplicationScoped
public class BeanConfig {
    


    @ApplicationScoped
    public SsmService SsmService() {
        return new SsmService(SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build());
    };
}
