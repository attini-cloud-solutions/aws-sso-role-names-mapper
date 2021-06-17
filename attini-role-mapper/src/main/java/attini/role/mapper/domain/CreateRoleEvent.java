package attini.role.mapper.domain;

import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.domain.exceptions.WrongEventTypeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

public class CreateRoleEvent {
    private final RoleName roleName;
    private final Arn arn;

    private CreateRoleEvent(RoleName roleName, Arn arn) {
        this.roleName = Objects.requireNonNull(roleName);
        this.arn = Objects.requireNonNull(arn);
    }

    public static CreateRoleEvent create(RoleName roleName, Arn arn) {
        return new CreateRoleEvent(roleName, arn);
    }

    public static CreateRoleEvent create(Map<String, Object> eventPayload) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode eventPayloadJson = objectMapper.valueToTree(eventPayload);
            if (!eventPayloadJson.get("eventName").asText().equals("CreateRole")){
                throw new WrongEventTypeException("\"eventName\" field must be CreateRole.");
            }
            RoleName roleName = RoleName.create(eventPayloadJson.get("responseElements").get("role").get("roleName").asText());
            if (!roleName.toString().startsWith("AWSReservedSSO")) {
                throw new InvalidEventPayloadException("\"roleName\" field must start with AWSReservedSSO.");
            }
            Arn arn = Arn.create(eventPayloadJson.get("responseElements").get("role").get("arn").asText());
            return new CreateRoleEvent(roleName, arn);
        }
        catch (WrongEventTypeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new InvalidEventPayloadException(e);
        }
    }

    public PermissionSetName getPermissionSetName() {
        return PermissionSetName.create(this.roleName.toString());
    }

    public ParameterName getParameterName() {
        return ParameterName.create(getPermissionSetName());
    }

    public Arn getArn() {
        return arn;
    }

    public RoleName getRoleName() {
        return roleName;
    }
}
