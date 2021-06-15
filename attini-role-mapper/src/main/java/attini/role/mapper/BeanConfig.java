package attini.role.mapper;

import javax.enterprise.context.ApplicationScoped;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.ssm.SsmClient;


@ApplicationScoped
public class BeanConfig {
    
    @ApplicationScoped
    public IamService IamService() {
        return new IamService(IamClient.builder().httpClient(UrlConnectionHttpClient.create()));
    }

    @ApplicationScoped
    public SsmService SsmService() {
        return new SsmService(SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build());
    };
}
