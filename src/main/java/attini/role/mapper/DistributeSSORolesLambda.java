package attini.role.mapper;

import attini.role.mapper.domain.CreateRoleEvent;
import attini.role.mapper.domain.DeleteRoleEvent;
import attini.role.mapper.domain.DistributeSSORolesResponse;
import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.facades.EnvironmentVariables;
import attini.role.mapper.facades.IamFacade;
import attini.role.mapper.services.DistributeSSORolesService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
        LOGGER.info("PROCESSORS: " + Runtime.getRuntime().availableProcessors());
        LOGGER.info("Got event " + event);
        if (event.containsKey("eventName")) {
            String eventName = event.get("eventName").toString();
            if (eventName.equals("CreateRole")) {
                return distributeSSORolesService.handleCreateRoleEvent(CreateRoleEvent.create(environmentVariables, event));
            } else if (eventName.equals("DeleteRole")) {
                return distributeSSORolesService.handleDeleteRoleEvent(DeleteRoleEvent.create(environmentVariables, event));
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

