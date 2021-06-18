package attini.role.mapper.domain;

import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.domain.exceptions.WrongEventTypeException;
import attini.role.mapper.facades.EnvironmentVariables;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

public class DeleteRoleEvent {

    private final RoleName roleName;
    private final ParameterName parameterName;

    private DeleteRoleEvent(RoleName roleName, ParameterName parameterName) {
        this.roleName = Objects.requireNonNull(roleName, "roleName");
        this.parameterName = Objects.requireNonNull(parameterName, "parameterName");
    }

    public static DeleteRoleEvent create(RoleName roleName, ParameterName parameterName) {
        return new DeleteRoleEvent(roleName, parameterName);
    }

    public static DeleteRoleEvent create(EnvironmentVariables environmentVariables, Map<String, Object> eventPayload) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode eventPayloadJson = objectMapper.valueToTree(eventPayload);
            if (!eventPayloadJson.get("eventName").asText().equals("DeleteRole")) {
                throw new WrongEventTypeException("\"eventName\" field must be DeleteRole.");
            }
            RoleName roleName = RoleName.create(eventPayloadJson.get("requestParameters").get("roleName").asText());
            if (!roleName.toString().startsWith("AWSReservedSSO")) {
                throw new InvalidEventPayloadException("\"roleName\" field must start with AWSReservedSSO.");
            }
            ParameterName parameterName = ParameterName.create(environmentVariables.getParameterStorePrefix(),
                    PermissionSetName.create(roleName.toString()));
            return new DeleteRoleEvent(roleName, parameterName);
        } catch (WrongEventTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidEventPayloadException(e);
        }
    }

    public RoleName getRoleName() {
        return roleName;
    }


    public ParameterName getParameterName() {
        return parameterName;
    }

}
