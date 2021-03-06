package aws.sso.role.names.mapper.facades;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;

public class IamFacade {
    private final IamClient iamClient;

    public IamFacade(IamClient iamClient) {
        this.iamClient = Objects.requireNonNull(iamClient, "iamClient");
    }

    /**
     * @return All roles on IAM with path /aws-reserved/sso.amazonaws.com/
     */
    public Set<Role> listAllRoles() {
        return iamClient
                .listRolesPaginator(ListRolesRequest
                        .builder()
                        .pathPrefix("/aws-reserved/sso.amazonaws.com/")
                        .build())
                .stream()
                .map(ListRolesResponse::roles)
                .flatMap(List::stream)
                .collect(toSet());
    }
}
