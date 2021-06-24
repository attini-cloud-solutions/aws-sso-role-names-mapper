package aws.sso.role.names.mapper.domain;

import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.domain.exceptions.WrongEventTypeException;
import com.fasterxml.jackson.databind.JsonNode;

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

    public static DeleteRoleEvent create(ParameterStorePrefix parameterStorePrefix, JsonNode eventDetailPayload) {
        try {
            if (!eventDetailPayload.get("eventName").asText().equals("DeleteRole")) {
                throw new WrongEventTypeException("\"eventName\" field must be DeleteRole.");
            }
            RoleName roleName = RoleName.create(eventDetailPayload.get("requestParameters").get("roleName").asText());
            if (!roleName.getName().startsWith("AWSReservedSSO")) {
                throw new InvalidEventPayloadException("\"roleName\" field must start with AWSReservedSSO.");
            }
            ParameterName parameterName = ParameterName.create(parameterStorePrefix,
                    PermissionSetName.create(roleName.getName()));
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
