package attini.role.mapper;

import javax.enterprise.context.ApplicationScoped;

import attini.role.mapper.factories.SsmClientFactory;
import attini.role.mapper.services.DistributeSSORolesService;
import attini.role.mapper.services.IamFacade;
import attini.role.mapper.services.SsmFacade;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.ssm.SsmClient;


@ApplicationScoped
public class BeanConfig {

    @ApplicationScoped
    public DistributeSSORolesService distributeSSORolesService() {
        return new DistributeSSORolesService(ssmService());
    }

    @ApplicationScoped
    public IamFacade iamService() {
        return new IamFacade(IamClient.builder().region(Region.AWS_GLOBAL).httpClient(UrlConnectionHttpClient.create()).build());
    }

    @ApplicationScoped
    public SsmFacade ssmService() {
        return new SsmFacade(ssmClientFactory());
    }

    @ApplicationScoped
    public SsmClientFactory ssmClientFactory() {
        return new SsmClientFactory();
    }
}
