package attini.role.mapper;

import java.util.ArrayList;
import java.util.List;

import attini.role.mapper.domain.ParameterName;
import attini.role.mapper.domain.PermissionSetName;
import attini.role.mapper.domain.SsmDeleteParameterRequest;
import attini.role.mapper.domain.SsmPutParameterRequest;
import com.amazonaws.services.kinesis.model.Consumer;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

public class SsmService {
    /**
     * Get all regions from /aws/service/global-infrastructure/regions.
     * Ignores CN/gov regions.
     */
    private final SsmClient ssmClient;

    public SsmService(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

    public List<Region> getAllRegions() {
        GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/regions"); // /aws/service/global-infrastructure/services/ssm/regions
        GetParametersByPathIterable iterable = ssmClient.getParametersByPathPaginator(requestBuilder.build());

        ArrayList<Parameter> parameters = new ArrayList<Parameter>();

        iterable.stream().forEach(page -> {
            parameters.addAll(page.parameters());
        });

        ArrayList<Region> regions = new ArrayList<Region>();

        parameters.stream().filter(parameter -> !parameter.value().contains("-gov-") && !parameter.value().contains("cn-")).forEach(param -> {
            regions.add(Region.of(param.value()));
        });

        return regions;
    }



    public DeleteParameterResponse deleteParameter(SsmDeleteParameterRequest ssmDeleteParameterRequest) {
        SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(ssmDeleteParameterRequest.getRegion()).build();
        return client.deleteParameter(getDeleteParameterRequest(ssmDeleteParameterRequest.getParameterName()));
    }

    private static DeleteParameterRequest getDeleteParameterRequest(ParameterName parameterName) {
        return DeleteParameterRequest.builder().name(parameterName.getName()).build();
    }
    public PutParameterResponse putParameter(SsmPutParameterRequest ssmPutParameterRequest) {
        PutParameterRequest putParameterRequest = getCreateParameterRequest(ssmPutParameterRequest.getParameterName(), ssmPutParameterRequest.getPermissionSetName());
        SsmClient client = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).region(ssmPutParameterRequest.getRegion()).build();
        return client.putParameter(putParameterRequest);
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
