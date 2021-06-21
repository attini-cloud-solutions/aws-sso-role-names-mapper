package attini.role.mapper.facades;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IamFacade {
    private final IamClient iamClient;

    @Inject
    public IamFacade(IamClient iamClient) {
        this.iamClient = Objects.requireNonNull(iamClient, "iamClient");
    }

    /**
     * @return All roles on IAM from path /aws-reserved/sso.amazonaws.com/
     */
    public Set<Role> listAllRoles() {
        ListRolesIterable listRolesResponses = iamClient.listRolesPaginator(ListRolesRequest.builder().pathPrefix("/aws-reserved/sso.amazonaws.com/").build());

        return listRolesResponses.stream()
                .map(ListRolesResponse::roles)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }
}