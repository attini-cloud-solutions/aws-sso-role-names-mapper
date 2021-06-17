package attini.role.mapper.domain;

import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.domain.exceptions.WrongEventTypeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeleteRoleEventTest {
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void create_CorrectJson_ShouldPass() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRolePayload.json").toFile(), Map.class);
        DeleteRoleEvent actualCreateRoleEvent = DeleteRoleEvent.create(event);
        DeleteRoleEvent expectedCreateRoleEvent = DeleteRoleEvent.create(
                RoleName.create("AWSReservedSSO_test-latest_58dcaf6a4cfad558"));
        assertEquals(expectedCreateRoleEvent.getRoleName().toString(), actualCreateRoleEvent.getRoleName().toString());

    }


    @Test
    public void create_WrongEventType_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/createRolePayload.json").toFile(), Map.class);
        assertThrows(WrongEventTypeException.class, () -> DeleteRoleEvent.create(event));

    }

    @Test
    public void create_BadRoleName_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRoleBadRoleNamePayload.json").toFile(), Map.class);
        assertThrows(InvalidEventPayloadException.class, () -> DeleteRoleEvent.create(event));

    }

    @Test
    public void create_MissingResponseElements_ShouldThrow() throws IOException {
        Map<String, Object> event = mapper.readValue(Paths.get("src/test/resources/deleteRoleMissingRequestParametersPayload.json").toFile(), Map.class);
        assertThrows(InvalidEventPayloadException.class, () -> DeleteRoleEvent.create(event));

    }
}