package aws.sso.role.names.mapper.domain;

import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.domain.exceptions.WrongEventTypeException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

public class CreateRoleEvent {
    private final RoleName roleName;
    private final Arn iamRoleName;
    private final ParameterName parameterName;
    private final PermissionSetName permissionSetName;

    private CreateRoleEvent(RoleName roleName, Arn arn, ParameterName parameterName) {
        this.roleName = Objects.requireNonNull(roleName, "roleName");
        this.iamRoleName = Objects.requireNonNull(arn, "roleName");
        this.parameterName = Objects.requireNonNull(parameterName, "parameterName");
        this.permissionSetName = Objects.requireNonNull(PermissionSetName.create(this.roleName.toString()), "permissionSetName");
    }

    public static CreateRoleEvent create(RoleName roleName, Arn arn, ParameterName parameterName) {
        return new CreateRoleEvent(roleName, arn, parameterName);
    }

    public static CreateRoleEvent create(EnvironmentVariables environmentVariables, JsonNode eventDetailPayload) {
        try {
            if (!eventDetailPayload.get("eventName").asText().equals("CreateRole")) {
                throw new WrongEventTypeException("\"eventName\" field must be CreateRole.");
            }
            RoleName roleName = RoleName.create(eventDetailPayload.get("responseElements").get("role").get("roleName").asText());
            if (!roleName.toString().startsWith("AWSReservedSSO")) {
                throw new InvalidEventPayloadException("\"roleName\" field must start with AWSReservedSSO.");
            }
            Arn arn = Arn.create(eventDetailPayload.get("responseElements").get("role").get("arn").asText());
            ParameterName parameterName = ParameterName.create(environmentVariables.getParameterStorePrefix(),
                    PermissionSetName.create(roleName.toString()));
            return new CreateRoleEvent(roleName, arn, parameterName);
        } catch (WrongEventTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidEventPayloadException(e);
        }
    }

    public Arn getIamRoleName() {
        return iamRoleName;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public ParameterName getParameterName() {
        return parameterName;
    }

    public PermissionSetName getPermissionSetName() {
        return permissionSetName;
    }
}
