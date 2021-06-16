package attini.role.mapper.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import attini.role.mapper.domain.*;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

import javax.inject.Inject;

public class SsmFacade {

    private final static Logger LOGGER = Logger.getLogger(SsmFacade.class);

    // TODO: Factory
    private final SsmClientBuilder ssmClientBuilder;

    @Inject
    public SsmFacade(SsmClientBuilder ssmClient) {
        this.ssmClientBuilder = ssmClient;
    }

    // TODO: Titta över algorithmer, forEach addAll inte bra, mutera helst inte.

    /**
     * Ignores CN/gov regions.
     *
     * @return all regions from /aws/service/global-infrastructure/regions.
     */
    public List<Region> getAllRegions() { // TODO should return Set<>
        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions"); // /aws/service/global-infrastructure/services/ssm/regions
        GetParametersByPathIterable iterable = ssmClientBuilder.build().getParametersByPathPaginator(requestBuilder.build());

        return iterable
                .stream()
                .map(GetParametersByPathResponse::parameters)
                .flatMap(List::stream)
                .filter(parameter -> !parameter.value().contains("-gov-") && !parameter.value().contains("cn-"))
                .map(Parameter::value)
                .map(Region::of)
                .collect(Collectors.toList());
    }

    /**
     * @param region
     * @return Set of parameters from path /attini/aws-sso-role-names-mapper
     * Empty if exception thrown or no parameters found.
     */
    public Set<Parameter> getParameters(Region region) {
        Set<Parameter> parameters = new HashSet<>();
        try {
            SsmClient ssmClient = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region).build();
            GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/attini/aws-sso-role-names-mapper");
            GetParametersByPathIterable iterable = ssmClient.getParametersByPathPaginator(requestBuilder.build());

            parameters = iterable.stream()
                    .map(GetParametersByPathResponse::parameters)
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());

        } catch (SsmException e) {
            LOGGER.warn("Could not get parameters from region: " + region, e);
        }
        return parameters;

    }


    /**
     * @param ssmDeleteParametersRequest
     * @return true if successfully deleted parameters in region, false otherwise.
     */
    public boolean deleteParameters(SsmDeleteParametersRequest ssmDeleteParametersRequest) {
        try {
            SsmClient client = ssmClientBuilder.region(ssmDeleteParametersRequest.getRegion()).build();
            DeleteParametersRequest deleteParametersRequest = DeleteParametersRequest
                    .builder()
                    .names(ssmDeleteParametersRequest.getParameterNames()
                            .stream().map(ParameterName::getName)
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
     * @param ssmDeleteParameterRequest
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean deleteParameter(SsmDeleteParameterRequest ssmDeleteParameterRequest) {
        try {
            SsmClient client = ssmClientBuilder.region(ssmDeleteParameterRequest.getRegion()).build();
            client.deleteParameter(getDeleteParameterRequest(ssmDeleteParameterRequest.getParameterName()));
            LOGGER.info("Deleted: " + ssmDeleteParameterRequest.getParameterName().getName() + " in region: " + ssmDeleteParameterRequest.getRegion());
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not delete parameter " + ssmDeleteParameterRequest.getParameterName().getName() + " in region: " + ssmDeleteParameterRequest.getRegion(), e);
            return false;
        }
    }

    /**
     * @param ssmPutParameterRequest
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean putParameter(SsmPutParameterRequest ssmPutParameterRequest) {
        try {
            PutParameterRequest putParameterRequest = getCreateParameterRequest(ssmPutParameterRequest.getParameterName(), ssmPutParameterRequest.getPermissionSetName(), ssmPutParameterRequest.getArn());
            // TODO: Använd clienten
            SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(ssmPutParameterRequest.getRegion()).build();
            client.putParameter(putParameterRequest);
            LOGGER.info("Saved: " + ssmPutParameterRequest.getParameterName().getName() + " in region: " + ssmPutParameterRequest.getRegion());
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not create parameter: " + ssmPutParameterRequest.getParameterName().getName() + " in region: " + ssmPutParameterRequest.getRegion(), e);
            return false;
        }
    }

    private static DeleteParameterRequest getDeleteParameterRequest(ParameterName parameterName) {
        return DeleteParameterRequest.builder().name(parameterName.getName()).build();
    }

    private static PutParameterRequest getCreateParameterRequest(ParameterName parameterName, PermissionSetName permissionSetName, Arn arn) {
        return PutParameterRequest.builder()
                .type(ParameterType.STRING)
                .name(parameterName.getName())
                .description("Role arn for AWS SSO PermissionSet " + permissionSetName.getName())
                .value(arn.getArn())
                .overwrite(true)
                .tier(ParameterTier.STANDARD)
                .build();
    }
}
