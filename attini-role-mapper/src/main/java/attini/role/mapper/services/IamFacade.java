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
import java.util.Set;
import java.util.stream.Collectors;

public class IamFacade {
    private final IamClient iamClient;

    @Inject
    public IamFacade(IamClient iamClient) { this.iamClient = iamClient; }

    /**
     * @return All roles on IAM from path /aws-reserved/sso.amazonaws.com/
     */
    public Set<Role> listAllRoles() {
        ListRolesIterable listRolesResponses = iamClient.listRolesPaginator(ListRolesRequest.builder().pathPrefix("/aws-reserved/sso.amazonaws.com/").build());
        return listRolesResponses
                .roles()
                .stream()
                .collect(Collectors.toSet());
    }
}
