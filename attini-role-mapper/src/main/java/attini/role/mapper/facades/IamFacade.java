package attini.role.mapper.facades;


import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;

public class IamFacade {
    private final IamClient iamClient;

    @Inject
    public IamFacade(IamClient iamClient) { this.iamClient = iamClient; }

    /**
     * @return All roles on IAM from path /aws-reserved/sso.amazonaws.com/
     */
    //TODO skulle kunna ha ett enhetstest.
    // Inte super viktigt då den inte gör så mycket men helt plötsligt kommer det en klåfingrig utvecklare som inte fattar hur paginering fungerar och har sönder allt
    public Set<Role> listAllRoles() {
        ListRolesIterable listRolesResponses = iamClient.listRolesPaginator(ListRolesRequest.builder().pathPrefix("/aws-reserved/sso.amazonaws.com/").build());
        return listRolesResponses
                .roles()
                .stream()
                .collect(Collectors.toSet());
    }
}
