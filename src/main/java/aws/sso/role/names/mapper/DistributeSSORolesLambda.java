package aws.sso.role.names.mapper;

import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import aws.sso.role.names.mapper.domain.CreateRoleEvent;
import aws.sso.role.names.mapper.domain.DeleteRoleEvent;
import aws.sso.role.names.mapper.domain.DistributeSSORolesResponse;
import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import aws.sso.role.names.mapper.facades.IamFacade;
import aws.sso.role.names.mapper.services.DistributeSSORolesService;

@Named("DistributeSSORolesLambda")
public class DistributeSSORolesLambda implements RequestHandler<Map<String, Object>, DistributeSSORolesResponse> {

    private static final Logger LOGGER = Logger.getLogger(DistributeSSORolesLambda.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final DistributeSSORolesService distributeSSORolesService;
    private final IamFacade iamFacade;
    private final EnvironmentVariables environmentVariables;

    @Inject
    public DistributeSSORolesLambda(DistributeSSORolesService distributeSSORolesService,
                                    IamFacade iamFacade,
                                    EnvironmentVariables environmentVariables) {
        this.distributeSSORolesService = Objects.requireNonNull(distributeSSORolesService, "distributeSSORolesService");
        this.iamFacade = Objects.requireNonNull(iamFacade, "iamFacade");
        this.environmentVariables = Objects.requireNonNull(environmentVariables, "environmentVariables");
    }

    @Override
    public DistributeSSORolesResponse handleRequest(Map<String, Object> event, Context context) {
        JsonNode eventPayloadJson = mapper.valueToTree(event);
        LOGGER.info("Got event: " + eventPayloadJson.toString());

        if (eventPayloadJson.has("detail") &&  eventPayloadJson.get("detail").has("eventName")) {
            JsonNode detail = eventPayloadJson.get("detail");
            switch (detail.get("eventName").asText()){
                case "CreateRole":
                    return distributeSSORolesService.handleCreateRoleEvent(CreateRoleEvent.create(environmentVariables.getParameterStorePrefix(), detail));
                case "DeleteRole":
                    return distributeSSORolesService.handleDeleteRoleEvent(DeleteRoleEvent.create(environmentVariables.getParameterStorePrefix(), detail));
                default:
                    throw new InvalidEventPayloadException("\"eventName\" field must be CreateRole or DeleteRole.");
            }
        } else if (eventPayloadJson.has("ExecutionType")) {
            switch (eventPayloadJson.get("ExecutionType").asText()){
                case "Sync":
                    return distributeSSORolesService.handleSyncRolesEvent(iamFacade.listAllRoles());
                case "Cleanup":
                    return distributeSSORolesService.handleDeleteAllRolesEvent();
                default:
                    throw new InvalidEventPayloadException("Unknown ExecutionType=" +eventPayloadJson.get("ExecutionType").asText());
            }
        } else {
            throw new InvalidEventPayloadException("payload must contain \"ExecutionType\" or \"detail\" with \"eventName\"");
        }
    }
}

