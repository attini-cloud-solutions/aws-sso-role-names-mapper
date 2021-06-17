package attini.role.mapper.domain;

import attini.role.mapper.domain.Arn;
import attini.role.mapper.domain.CreateRoleEvent;
import attini.role.mapper.domain.RoleName;
import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.domain.exceptions.WrongEventTypeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CreateRoleEventTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void create_CorrectJson_ShouldPass() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRolePayload.json").toFile(), Map.class);
        CreateRoleEvent actualCreateRoleEvent = CreateRoleEvent.create(event);

        CreateRoleEvent expectedCreateRoleEvent = CreateRoleEvent.create(
                RoleName.create("AWSReservedSSO_test-latest_58dcaf6a4cfad558"),
                Arn.create("arn:aws:iam::855066048591:role/aws-reserved/sso.amazonaws.com/eu-west-1/AWSReservedSSO_test-latest_58dcaf6a4cfad558"));
        assertEquals(expectedCreateRoleEvent.getRoleName().toString(), actualCreateRoleEvent.getRoleName().toString());
        assertEquals(expectedCreateRoleEvent.getArn().toString(), actualCreateRoleEvent.getArn().toString());

    }

    @Test
    public void create_WrongEventType_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRolePayload.json").toFile(), Map.class);
        assertThrows(WrongEventTypeException.class, () -> CreateRoleEvent.create(event));

    }

    @Test
    public void create_BadRoleName_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRoleBadRoleNamePayload.json").toFile(), Map.class);
        assertThrows(InvalidEventPayloadException.class, () -> CreateRoleEvent.create(event));

    }

    @Test
    public void create_MissingResponseElements_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRoleMissingResponseElementsPayload.json").toFile(), Map.class);
        assertThrows(InvalidEventPayloadException.class, () -> CreateRoleEvent.create(event));

    }
}
