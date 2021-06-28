package aws.sso.role.names.mapper.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.domain.exceptions.WrongEventTypeException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DeleteRoleEventTest {


    private final static ObjectMapper mapper = new ObjectMapper();

    @Mock
    EnvironmentVariables environmentVariablesMock;

    @BeforeEach
    void setUp() {
        when(environmentVariablesMock.getParameterStorePrefix()).thenReturn(ParameterStorePrefix.create("/test/"));

    }

    @Test
    public void create_CorrectJson() throws IOException {

        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRolePayload.json").toFile(),
                                                     Map.class);
        JsonNode detail = mapper.valueToTree(event.get("detail"));
        DeleteRoleEvent actualDeleteRoleEvent = DeleteRoleEvent.create(environmentVariablesMock.getParameterStorePrefix(),
                                                                       detail);

        RoleName roleName = RoleName.create("AWSReservedSSO_test-role_58dcaf6a4cfad558");
        DeleteRoleEvent expectedDeleteRoleEvent = DeleteRoleEvent.create(
                roleName,
                ParameterName.create(environmentVariablesMock.getParameterStorePrefix(),
                                     PermissionSetName.create(roleName.getName())));
        assertEquals(expectedDeleteRoleEvent.getRoleName().getName(), actualDeleteRoleEvent.getRoleName().getName());

    }

    @Test
    public void create_WrongEventType_ShouldThrow() throws IOException {

        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRolePayload.json").toFile(),
                                                     Map.class);
        JsonNode detail = mapper.valueToTree(event.get("detail"));
        assertThrows(WrongEventTypeException.class,
                     () -> DeleteRoleEvent.create(environmentVariablesMock.getParameterStorePrefix(), detail));

    }

    @Test
    public void create_BadRoleName_ShouldThrow() throws IOException {

        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRoleBadRoleNamePayload.json")
                                                          .toFile(), Map.class);
        JsonNode detail = mapper.valueToTree(event.get("detail"));
        assertThrows(InvalidEventPayloadException.class,
                     () -> DeleteRoleEvent.create(environmentVariablesMock.getParameterStorePrefix(), detail));

    }

    @Test
    public void create_MissingResponseElements_ShouldThrow() throws IOException {

        Map<String, Object> event = mapper.readValue(Paths.get(
                "src/test/resources/deleteRoleMissingRequestParametersPayload.json").toFile(), Map.class);
        JsonNode detail = mapper.valueToTree(event.get("detail"));
        assertThrows(InvalidEventPayloadException.class,
                     () -> DeleteRoleEvent.create(environmentVariablesMock.getParameterStorePrefix(), detail));
    }
}