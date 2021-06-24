package aws.sso.role.names.mapper.domain;

import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.domain.exceptions.WrongEventTypeException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
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


    //TODO skicka in prefix i metoden istället för hela environmentVariables, i regel är det bättre att inte skicka med mer saker än vad som behövs
    //Input är en del av metodens signatur, så det är mycket tydligare att säga "Denna metod behöver parameter store prefixet" än  att säga att den
    //behöver systemvariabler.
    public static DeleteRoleEvent create(EnvironmentVariables environmentVariables, JsonNode eventDetailPayload) {
        try {
            if (!eventDetailPayload.get("eventName").asText().equals("DeleteRole")) {
                throw new WrongEventTypeException("\"eventName\" field must be DeleteRole.");
            }
            RoleName roleName = RoleName.create(eventDetailPayload.get("requestParameters").get("roleName").asText());
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
