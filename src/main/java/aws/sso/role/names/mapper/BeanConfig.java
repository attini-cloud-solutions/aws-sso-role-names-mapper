package aws.sso.role.names.mapper;

import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import aws.sso.role.names.mapper.facades.IamFacade;
import aws.sso.role.names.mapper.facades.SsmFacade;
import aws.sso.role.names.mapper.factories.SsmClientFactory;
import aws.sso.role.names.mapper.services.DistributeSSORolesService;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class BeanConfig {

    @ApplicationScoped
    public DistributeSSORolesService distributeSSORolesService() {
        return new DistributeSSORolesService(ssmFacade(), environmentVariables());
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
    public SsmFacade ssmFacade() {
        return new SsmFacade(ssmClientFactory(), environmentVariables());
    }

    @ApplicationScoped
    public SsmClientFactory ssmClientFactory() {
        return new SsmClientFactory();
    }
}
