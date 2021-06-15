package attini.role.mapper.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

public class SsmService {

    private final static Logger LOGGER = Logger.getLogger(SsmService.class);

    private final SsmClientBuilder ssmClientBuilder;

    @Inject
    public SsmService(SsmClientBuilder ssmClient) {
        this.ssmClientBuilder = ssmClient;
    }


    /**
     * Ignores CN/gov regions.
     * @return all regions from /aws/service/global-infrastructure/regions.
     */
    public List<Region> getAllRegions() { // TODO should return Set<>
        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions"); // /aws/service/global-infrastructure/services/ssm/regions
        GetParametersByPathIterable iterable = ssmClientBuilder.build().getParametersByPathPaginator(requestBuilder.build());

        ArrayList<Parameter> parameters = new ArrayList<>();

        iterable.stream().forEach(page -> {
            parameters.addAll(page.parameters());
        });

        ArrayList<Region> regions = new ArrayList<>();

        parameters.stream().filter(parameter -> !parameter.value().contains("-gov-") && !parameter.value().contains("cn-")).forEach(param -> {
            regions.add(Region.of(param.value()));
        });

        return regions;
    }

    /**
     *
     * @param region
     * @return Set of parameters from path /attini/aws-sso-role-names-mapper
     */
    public HashSet<Parameter> getParameters(Region region) {
        HashSet<Parameter> parameters = new HashSet<>();
        try {
            SsmClient ssmClient = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region).build();
            GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/attini/aws-sso-role-names-mapper");
            GetParametersByPathIterable iterable = ssmClient.getParametersByPathPaginator(requestBuilder.build());
            for (GetParametersByPathResponse response : iterable) {
                parameters.addAll(response.parameters());
            }
            iterable.stream().forEach(page -> parameters.addAll(page.parameters()));
        }
        catch (Exception e) {
            // TODO handle these exception better, one for "security token invalid".
            // log here

        }
        return parameters;

    }


    /**
     *
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
        }
        catch (Exception e) {
            return false;
        }
    }


    /**
     *
     * @param ssmDeleteParameterRequest
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean deleteParameter(SsmDeleteParameterRequest ssmDeleteParameterRequest) {
        try {
            SsmClient client = ssmClientBuilder.region(ssmDeleteParameterRequest.getRegion()).build();
            client.deleteParameter(getDeleteParameterRequest(ssmDeleteParameterRequest.getParameterName()));
            LOGGER.info("Deleted: " + ssmDeleteParameterRequest.getParameterName().getName() + " in region: " + ssmDeleteParameterRequest.getRegion());
            return true;
        }
        catch (Exception e) {
            LOGGER.warn("Could not delete parameter " + ssmDeleteParameterRequest.getParameterName().getName() + " in region: " + ssmDeleteParameterRequest.getRegion());
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
            SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(ssmPutParameterRequest.getRegion()).build();
            client.putParameter(putParameterRequest);
            LOGGER.info("Saved: " + ssmPutParameterRequest.getParameterName().getName() + " in region: " + ssmPutParameterRequest.getRegion());
            return true;
        }
        catch (Exception e) {
            LOGGER.warn("Could not create parameter: " + ssmPutParameterRequest.getParameterName().getName() + " in region: " + ssmPutParameterRequest.getRegion());
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
                            .overwrite(true) // TODO Fraga Carl
                            .tier(ParameterTier.STANDARD)
                            .build();
    }
}
