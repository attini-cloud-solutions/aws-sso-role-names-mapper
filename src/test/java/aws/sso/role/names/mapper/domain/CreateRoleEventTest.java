package aws.sso.role.names.mapper.domain;

import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.domain.exceptions.WrongEventTypeException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class CreateRoleEventTest {

    ObjectMapper mapper = new ObjectMapper();
    EnvironmentVariables environmentVariablesMock;

    public CreateRoleEventTest() {
        environmentVariablesMock = mock(EnvironmentVariables.class);
        when(environmentVariablesMock.getParameterStorePrefix()).thenReturn("/test/");
    }

    @Test
    public void create_CorrectJson() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRolePayload.json").toFile(), Map.class);
        CreateRoleEvent actualCreateRoleEvent = CreateRoleEvent.create(environmentVariablesMock, event);

        RoleName roleName = RoleName.create("AWSReservedSSO_test-role_58dcaf6a4cfad558");

        CreateRoleEvent expectedCreateRoleEvent = CreateRoleEvent.create(
                roleName,
                Arn.create("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_test-role_58dcaf6a4cfad558"),
                ParameterName.create(environmentVariablesMock.getParameterStorePrefix(), PermissionSetName.create(roleName.toString())));
        assertEquals(expectedCreateRoleEvent.getRoleName().toString(), actualCreateRoleEvent.getRoleName().toString());
        assertEquals(expectedCreateRoleEvent.getIamRoleName().toString(), actualCreateRoleEvent.getIamRoleName().toString());

    }

    @Test
    public void create_WrongEventType_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRolePayload.json").toFile(), Map.class);
        assertThrows(WrongEventTypeException.class, () -> CreateRoleEvent.create(environmentVariablesMock, event));

    }

    @Test
    public void create_BadRoleName_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRoleBadRoleNamePayload.json").toFile(), Map.class);
        assertThrows(InvalidEventPayloadException.class, () -> CreateRoleEvent.create(environmentVariablesMock, event));

    }

    @Test
    public void create_MissingResponseElements_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRoleMissingResponseElementsPayload.json").toFile(), Map.class);
        assertThrows(InvalidEventPayloadException.class, () -> CreateRoleEvent.create(environmentVariablesMock, event));

    }
}
