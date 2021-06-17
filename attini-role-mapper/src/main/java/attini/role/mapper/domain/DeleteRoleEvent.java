package attini.role.mapper.domain;

import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.domain.exceptions.WrongEventTypeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class DeleteRoleEvent {

    private final RoleName roleName;

    private DeleteRoleEvent(RoleName roleName) {
        this.roleName = roleName;
    }

    public static DeleteRoleEvent create(RoleName roleName) {
        return new DeleteRoleEvent(roleName);
    }
    public static DeleteRoleEvent create(Map<String, Object> eventPayload) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode eventPayloadJson = objectMapper.valueToTree(eventPayload);
            if (!eventPayloadJson.get("eventName").asText().equals("DeleteRole")){
                throw new WrongEventTypeException("\"eventName\" field must be DeleteRole.");
            }
            RoleName roleName = RoleName.create(eventPayloadJson.get("requestParameters").get("roleName").asText());
            if (!roleName.toString().startsWith("AWSReservedSSO")) {
                throw new InvalidEventPayloadException("\"roleName\" field must start with AWSReservedSSO.");
            }
            return new DeleteRoleEvent(roleName);
        }
        catch (WrongEventTypeException e) {
            throw e;
        }
        catch (Exception e){
            throw new InvalidEventPayloadException(e);
        }
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public PermissionSetName getPermissionSetName() {
        return PermissionSetName.create(this.roleName.toString());
    }

    public ParameterName getParameterName() {
        return ParameterName.create(getPermissionSetName());
    }
}
