package aws.sso.role.names.mapper;

import aws.sso.role.names.mapper.domain.CreateRoleEvent;
import aws.sso.role.names.mapper.domain.DeleteRoleEvent;
import aws.sso.role.names.mapper.domain.DistributeSSORolesResponse;
import aws.sso.role.names.mapper.domain.exceptions.InvalidEventPayloadException;
import aws.sso.role.names.mapper.facades.EnvironmentVariables;
import aws.sso.role.names.mapper.facades.IamFacade;
import aws.sso.role.names.mapper.services.DistributeSSORolesService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Objects;

@Named("DistributeSSORolesLambda")
public class DistributeSSORolesLambda implements RequestHandler<Map<String, Object>, DistributeSSORolesResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesLambda.class);
    private final DistributeSSORolesService distributeSSORolesService;
    private final IamFacade iamFacade;
    private final EnvironmentVariables environmentVariables;

    @Inject
    public DistributeSSORolesLambda(DistributeSSORolesService distributeSSORolesService, IamFacade iamFacade, EnvironmentVariables environmentVariables) {
        this.distributeSSORolesService = Objects.requireNonNull(distributeSSORolesService, "distributeSSORolesService");
        this.iamFacade = Objects.requireNonNull(iamFacade, "iamFacade");
        this.environmentVariables = Objects.requireNonNull(environmentVariables, "environmentVariables");
    }

    @Override
    public DistributeSSORolesResponse handleRequest(Map<String, Object> event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode eventPayloadJson = mapper.valueToTree(event);
        LOGGER.info("Got event: " + eventPayloadJson.toString());

        JsonNode detail = mapper.valueToTree(event.get("detail")); //TODO lite väl att göra valueToTree på eventet två gånger, gör  eventPayloadJson.get("detail"); istället

        //TODO skulle vara snyggt hör om man kunde unvika dom nästlade if-satserna. Inget krav dock
        if (detail.has("eventName")) {
            String eventName = detail.get("eventName").asText();
            if (eventName.equals("CreateRole")) {
                return distributeSSORolesService.handleCreateRoleEvent(CreateRoleEvent.create(environmentVariables, detail));
            } else if (eventName.equals("DeleteRole")) {
                return distributeSSORolesService.handleDeleteRoleEvent(DeleteRoleEvent.create(environmentVariables, detail));
            } else {
                throw new InvalidEventPayloadException("\"eventName\" field must be CreateRole or DeleteRole.");
            }
        } else if (event.containsKey("resources") && event.get("resources").toString().contains("-TriggerOnSchedule-")) {
            return distributeSSORolesService.handleScheduledEvent(iamFacade.listAllRoles());
        } else {
            throw new InvalidEventPayloadException("payload must contain resources with event from TriggerOnSchedule.");
        }
    }
}

