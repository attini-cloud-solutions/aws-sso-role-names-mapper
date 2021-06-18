package attini.role.mapper;

import attini.role.mapper.facades.EnvironmentVariables;
import attini.role.mapper.facades.IamFacade;
import attini.role.mapper.facades.SsmFacade;
import attini.role.mapper.factories.SsmClientFactory;
import attini.role.mapper.services.DistributeSSORolesService;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class BeanConfig {

    @ApplicationScoped
    public DistributeSSORolesService distributeSSORolesService() {
        return new DistributeSSORolesService(ssmService(), environmentVariables());
    }

    @ApplicationScoped
    public EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }

    @ApplicationScoped
    public IamFacade iamFacade() {
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
