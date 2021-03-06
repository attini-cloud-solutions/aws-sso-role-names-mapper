package aws.sso.role.names.mapper.facades;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;

import aws.sso.role.names.mapper.domain.Arn;
import aws.sso.role.names.mapper.domain.ParameterName;
import aws.sso.role.names.mapper.domain.PermissionSetName;
import aws.sso.role.names.mapper.domain.SsmDeleteParameterRequest;
import aws.sso.role.names.mapper.domain.SsmDeleteParametersRequest;
import aws.sso.role.names.mapper.domain.SsmPutParameterRequest;
import aws.sso.role.names.mapper.domain.exceptions.CouldNotGetParametersException;
import aws.sso.role.names.mapper.factories.SsmClientFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest;
import software.amazon.awssdk.services.ssm.model.DeleteParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterTier;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

public class SsmFacade {

    private final static Logger LOGGER = Logger.getLogger(SsmFacade.class);

    private final SsmClientFactory ssmClientFactory;
    private final EnvironmentVariables environmentVariables;

    public SsmFacade(SsmClientFactory ssmClientFactory, EnvironmentVariables environmentVariables) {
        this.ssmClientFactory = Objects.requireNonNull(ssmClientFactory, "ssmClientFactory");
        this.environmentVariables = environmentVariables;
    }

    private static DeleteParameterRequest getDeleteParameterRequest(ParameterName parameterName) {
        return DeleteParameterRequest.builder().name(parameterName.getName()).build();
    }

    private static PutParameterRequest getCreateParameterRequest(ParameterName parameterName,
                                                                 PermissionSetName permissionSetName,
                                                                 Arn arn) {
        return PutParameterRequest.builder()
                                  .type(ParameterType.STRING)
                                  .name(parameterName.getName())
                                  .description("Role arn for AWS SSO PermissionSet " + permissionSetName.getName())
                                  .value(arn.toString())
                                  .overwrite(true)
                                  .tier(ParameterTier.STANDARD)
                                  .build();
    }

    /**
     * Ignores CN/gov regions.
     *
     * @return all regions from /aws/service/global-infrastructure/regions.
     */
    public Set<Region> getAllRegions() {
        // Will exclude regions containing substring in the set.
        final Set<String> prefixes = Set.of("-gov-", "cn-");
        return ssmClientFactory
                .createGlobalSsmClient()
                .getParametersByPathPaginator(GetParametersByPathRequest.builder()
                                                                        .path("/aws/service/global-infrastructure/regions")
                                                                        .build())
                .stream()
                .parallel()
                .map(GetParametersByPathResponse::parameters)
                .flatMap(List::stream)
                .map(Parameter::value)
                .filter(value -> validParameter(value, prefixes))
                .map(Region::of)
                .collect(toSet());
    }

    private Boolean validParameter(String parameter, Set<String> prefixes) {
        return prefixes.stream().noneMatch(parameter::contains);
    }

    /**
     * @return Set of parameters from environment variable PARAMETER_STORE_PREFIX
     * Empty if exception thrown or no parameters found.
     */
    public Set<Parameter> getParameters(Region region) {
        try {
            return ssmClientFactory
                    .createSsmClient(region)
                    .getParametersByPathPaginator(GetParametersByPathRequest.builder()
                                                                            .path(environmentVariables.getParameterStorePrefix()
                                                                                                      .getPrefix())
                                                                            .build())
                    .stream()
                    .parallel()
                    .map(GetParametersByPathResponse::parameters)
                    .flatMap(List::stream)
                    .collect(toSet());
        } catch (SsmException e) {
            LOGGER.warn("Could not get parameters from region: " + region
                        + " Error message: " + e.getMessage()
                        + " Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw new CouldNotGetParametersException("Could not get parameters from SSM", e);
        }
    }

    /**
     * @return a Set of parameters that was deleted
     */
    public Set<ParameterName> deleteParameters(SsmDeleteParametersRequest ssmDeleteParametersRequest) {
        try {
            DeleteParametersRequest deleteParametersRequest = DeleteParametersRequest
                    .builder()
                    .names(ssmDeleteParametersRequest.getParameterNames()
                                                     .stream()
                                                     .map(ParameterName::getName)
                                                     .collect(toList()))
                    .build();
            return ssmClientFactory
                    .createSsmClient(ssmDeleteParametersRequest.getRegion())
                    .deleteParameters(deleteParametersRequest)
                    .deletedParameters()
                    .stream()
                    .map(ParameterName::create)
                    .collect(toSet());
        } catch (SsmException e) {
            LOGGER.warn("Could not delete parameters: " + ssmDeleteParametersRequest.getParameterNames().toString()
                        + " in region: " + ssmDeleteParametersRequest.getRegion()
                        + " Error message: " + e.getMessage()
                        + " Stack trace: " + Arrays.toString(e.getStackTrace()), e);
            return emptySet();
        }
    }

    /**
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean deleteParameter(SsmDeleteParameterRequest ssmDeleteParameterRequest) {
        try {
            ssmClientFactory
                    .createSsmClient(ssmDeleteParameterRequest.getRegion())
                    .deleteParameter(getDeleteParameterRequest(ssmDeleteParameterRequest.getParameterName()));
            LOGGER.info("Deleted: " + ssmDeleteParameterRequest.getParameterName()
                                                               .getName() + " in region: " + ssmDeleteParameterRequest.getRegion());
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not delete parameter: " + ssmDeleteParameterRequest.getParameterName().getName()
                        + " in region: " + ssmDeleteParameterRequest.getRegion()
                        + " Error message: " + e.getMessage()
                        + " Stack trace: " + Arrays.toString(e.getStackTrace()));
            return false;
        }
    }

    /**
     * @return true if successfully deleted parameter in region, false otherwise.
     */
    public boolean putParameter(SsmPutParameterRequest ssmPutParameterRequest) {
        try {
            PutParameterRequest putParameterRequest = getCreateParameterRequest(ssmPutParameterRequest.getParameterName(),
                                                                                ssmPutParameterRequest.getPermissionSetName(),
                                                                                ssmPutParameterRequest.getIamRoleArn());
            ssmClientFactory
                    .createSsmClient(ssmPutParameterRequest.getRegion())
                    .putParameter(putParameterRequest);
            LOGGER.info("Saved: " + ssmPutParameterRequest.getParameterName()
                                                          .getName() + " in region: " + ssmPutParameterRequest.getRegion());
            return true;
        } catch (SsmException e) {
            LOGGER.warn("Could not create parameter: " + ssmPutParameterRequest.getParameterName().getName()
                        + " in region: " + ssmPutParameterRequest.getRegion()
                        + " Error message: " + e.getMessage()
                        + " Stack trace: " + Arrays.toString(e.getStackTrace()));
            return false;
        }
    }
}
