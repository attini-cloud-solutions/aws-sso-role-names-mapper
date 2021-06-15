package attini.role.mapper;

import javax.enterprise.context.ApplicationScoped;

import attini.role.mapper.services.DistributeSSORolesService;
import attini.role.mapper.services.IamService;
import attini.role.mapper.services.SsmService;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.ssm.SsmClient;


@ApplicationScoped
public class BeanConfig {

    @ApplicationScoped
    public DistributeSSORolesService distributeSSORolesService() {
        return new DistributeSSORolesService(iamService(), ssmService());
    }

    @ApplicationScoped
    public IamService iamService() {
        return new IamService(IamClient.builder().httpClient(UrlConnectionHttpClient.create()));
    }

    @ApplicationScoped
    public SsmService ssmService() {
        return new SsmService(SsmClient.builder().httpClient(UrlConnectionHttpClient.create()));
    };
}
