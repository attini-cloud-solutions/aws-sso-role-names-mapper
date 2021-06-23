package aws.sso.role.names.mapper.facades;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IamFacadeTest {

    IamFacade iamFacade;

    @Mock
    IamClient iamClientMock;

    @Mock
    ListRolesIterable listRolesIterableMock;

    @BeforeEach
    public void setup() {
        iamFacade = new IamFacade(iamClientMock);
    }

    @Test
    public void listAllRoles_ValidIterable() {
        when(iamClientMock.listRolesPaginator(any(ListRolesRequest.class))).thenReturn(listRolesIterableMock);

        ListRolesResponse.Builder responseBuilder = ListRolesResponse.builder();
        Role.Builder roleBuilder = Role.builder();

        Stream<ListRolesResponse> responseStream = Stream.of(
                responseBuilder.roles(
                        roleBuilder.roleName("Role1Page1").build(),
                        roleBuilder.roleName("Role2Page1").build(),
                        roleBuilder.roleName("Role3Page1").build()).build(),
                responseBuilder.roles(
                        roleBuilder.roleName("Role1Page2").build(),
                        roleBuilder.roleName("Role2Page2").build()).build());


        Set<Role> expectedSet = Set.of(
                Role.builder().roleName("Role1Page1").build(),
                Role.builder().roleName("Role2Page1").build(),
                Role.builder().roleName("Role3Page1").build(),
                Role.builder().roleName("Role1Page2").build(),
                Role.builder().roleName("Role2Page2").build());

        when(listRolesIterableMock.stream()).thenReturn(responseStream);

        Set<Role> roles = iamFacade.listAllRoles();

        assertEquals(expectedSet, roles);
    }
}
