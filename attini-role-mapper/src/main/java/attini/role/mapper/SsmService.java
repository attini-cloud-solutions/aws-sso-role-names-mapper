package attini.role.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import attini.role.mapper.domain.ParameterName;
import attini.role.mapper.domain.PermissionSetName;
import attini.role.mapper.domain.SsmDeleteParameterRequest;
import attini.role.mapper.domain.SsmPutParameterRequest;
import com.amazonaws.services.dynamodbv2.xspec.B;
import com.amazonaws.services.kinesis.model.Consumer;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

public class SsmService {

    private final SsmClient ssmClient;

    public SsmService(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }


    /**
     * Ignores CN/gov regions.
     * @return all regions from /aws/service/global-infrastructure/regions.
     */
    public List<Region> getAllRegions() { // TODO should return Set<>
        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions"); // /aws/service/global-infrastructure/services/ssm/regions
        GetParametersByPathIterable iterable = ssmClient.getParametersByPathPaginator(requestBuilder.build());

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
        SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region).build();
        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/attini/aws-sso-role-names-mapper");
        GetParametersByPathIterable iterable = client.getParametersByPathPaginator(requestBuilder.build());
        HashSet<Parameter> parameters = new HashSet<>();
        iterable.stream().forEach(page -> parameters.addAll(page.parameters()));
        return parameters;
    }


    /**
     *
     * @param ssmDeleteParameterRequest
     * @return true if success, false otherwise.
     */
    public boolean deleteParameter(SsmDeleteParameterRequest ssmDeleteParameterRequest) {
        try {
            SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(ssmDeleteParameterRequest.getRegion()).build();
            client.deleteParameter(getDeleteParameterRequest(ssmDeleteParameterRequest.getParameterName()));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * @param ssmPutParameterRequest
     * @return true if success, false otherwise.
     */
    public boolean putParameter(SsmPutParameterRequest ssmPutParameterRequest) {
        try {
            PutParameterRequest putParameterRequest = getCreateParameterRequest(ssmPutParameterRequest.getParameterName(), ssmPutParameterRequest.getPermissionSetName());
            SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(ssmPutParameterRequest.getRegion()).build();
            client.putParameter(putParameterRequest);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    private static DeleteParameterRequest getDeleteParameterRequest(ParameterName parameterName) {
        return DeleteParameterRequest.builder().name(parameterName.getName()).build();
    }
    private static PutParameterRequest getCreateParameterRequest(ParameterName parameterName, PermissionSetName permissionSetName) {
        return PutParameterRequest.builder()
                            .dataType("String")
                            .name(parameterName.getName())
                            .description("Role arn for AWS SSO PermissionSet " + permissionSetName.getName())
                            .value("CreateRole")
                            .overwrite(true)
                            .tier("Standard")
                            .build();
    }
}
