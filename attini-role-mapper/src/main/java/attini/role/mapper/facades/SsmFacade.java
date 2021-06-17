package attini.role.mapper.facades;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import attini.role.mapper.domain.*;
import attini.role.mapper.factories.SsmClientFactory;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

import javax.inject.Inject;

public class SsmFacade {

    private final static Logger LOGGER = Logger.getLogger(SsmFacade.class);

    private final SsmClientFactory ssmClientFactory;

    @Inject
    public SsmFacade(SsmClientFactory ssmClientFactory) {
        this.ssmClientFactory = ssmClientFactory;
    }

    /**
     * Ignores CN/gov regions.
     * @return all regions from /aws/service/global-infrastructure/regions.
     */
    public Set<Region> getAllRegions() {
        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions"); // /aws/service/global-infrastructure/services/ssm/regions
        GetParametersByPathIterable iterable = ssmClientFactory
                .createGlobalSsmClient()
                .getParametersByPathPaginator(requestBuilder.build());
        return iterable
                .stream()
                .map(GetParametersByPathResponse::parameters)
                .flatMap(List::stream)
                .filter(parameter -> !parameter.value().contains("-gov-") && !parameter.value().contains("cn-"))
                .map(Parameter::value)
                .map(Region::of)
                .collect(Collectors.toSet());
    }

    /**
     * @return Set of parameters from path /attini/aws-sso-role-names-mapper
     * Empty if exception thrown or no parameters found.
     */
    public Set<Parameter> getParameters(Region region) {
        try {
            SsmClient ssmClient = ssmClientFactory.createSsmClient(region);
            GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/attini/aws-sso-role-names-mapper");
            GetParametersByPathIterable iterable = ssmClient.getParametersByPathPaginator(requestBuilder.build());

            return iterable.stream()
                    .map(GetParametersByPathResponse::parameters)
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());

        } catch (SsmException e) {
            LOGGER.warn("Could not get parameters from region: " + region, e);
            return Collections.emptySet();
        }
    }


    /**
     * @return true if successfully deleted parameters in region, false otherwise.
     */
    public boolean deleteParameters(SsmDeleteParametersRequest ssmDeleteParametersRequest) {
        try {
            SsmClient client = ssmClientFactory.createSsmClient(ssmDeleteParametersRequest.getRegion());
            DeleteParametersRequest deleteParametersRequest = DeleteParametersRequest
                    .builder()
                    .names(ssmDeleteParametersRequest.getParameterNames()
                            .stream().map(ParameterName::toString)
                            .collect(Collectors.toList()))
                    .build();
            client.deleteParameters(deleteParametersRequest);
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not delete parameters in region: " + ssmDeleteParametersRequest.getRegion(), e);
            return false;
        }
    }


    /**
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean deleteParameter(SsmDeleteParameterRequest ssmDeleteParameterRequest) {
        try {
            SsmClient client = ssmClientFactory.createSsmClient(ssmDeleteParameterRequest.getRegion());
            client.deleteParameter(getDeleteParameterRequest(ssmDeleteParameterRequest.getParameterName()));
            LOGGER.info("Deleted: " + ssmDeleteParameterRequest.getParameterName().toString() + " in region: " + ssmDeleteParameterRequest.getRegion());
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not delete parameter " + ssmDeleteParameterRequest.getParameterName().toString() + " in region: " + ssmDeleteParameterRequest.getRegion(), e);
            return false;
        }
    }

    /**
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean putParameter(SsmPutParameterRequest ssmPutParameterRequest) {
        try {
            PutParameterRequest putParameterRequest = getCreateParameterRequest(ssmPutParameterRequest.getParameterName(), ssmPutParameterRequest.getPermissionSetName(), ssmPutParameterRequest.getArn());
            SsmClient client = ssmClientFactory.createSsmClient(ssmPutParameterRequest.getRegion());
            client.putParameter(putParameterRequest);
            LOGGER.info("Saved: " + ssmPutParameterRequest.getParameterName().toString() + " in region: " + ssmPutParameterRequest.getRegion());
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not create parameter: " + ssmPutParameterRequest.getParameterName().toString() + " in region: " + ssmPutParameterRequest.getRegion(), e);
            return false;
        }
    }

    private static DeleteParameterRequest getDeleteParameterRequest(ParameterName parameterName) {
        return DeleteParameterRequest.builder().name(parameterName.toString()).build();
    }

    private static PutParameterRequest getCreateParameterRequest(ParameterName parameterName, PermissionSetName permissionSetName, Arn arn) {
        return PutParameterRequest.builder()
                .type(ParameterType.STRING)
                .name(parameterName.toString())
                .description("Role arn for AWS SSO PermissionSet " + permissionSetName.toString())
                .value(arn.toString())
                .overwrite(true)
                .tier(ParameterTier.STANDARD)
                .build();
    }
}
