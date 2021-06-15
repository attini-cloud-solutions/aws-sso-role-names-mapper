package attini.role.mapper.services;


import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class IamService {
    private final IamClientBuilder iamClientBuilder;

    @Inject
    public IamService(IamClientBuilder iamClientBuilder) {
        this.iamClientBuilder = iamClientBuilder;
    }

    public List<Role> listAllRoles() {
        IamClient iamClient = iamClientBuilder.region(Region.AWS_GLOBAL).httpClient(UrlConnectionHttpClient.create()).build();
        ListRolesIterable listRolesResponses = iamClient.listRolesPaginator(ListRolesRequest.builder().pathPrefix("/aws-reserved/sso.amazonaws.com/").build());
        List<Role> roles = listRolesResponses.roles().stream().collect(Collectors.toList());
        return roles;
    }
}
