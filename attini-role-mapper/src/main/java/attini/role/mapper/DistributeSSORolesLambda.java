package attini.role.mapper;

import attini.role.mapper.domain.*;
import attini.role.mapper.domain.exceptions.InvalidEventPayloadException;
import attini.role.mapper.services.DistributeSSORolesService;
import attini.role.mapper.facades.IamFacade;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

// aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
// aws cloudformation delete-stack --stack-name joel-test
// aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM
// mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json
//
@Named("DistributeSSORolesLambda")
public class DistributeSSORolesLambda implements RequestHandler<Map<String, Object>, DistributeSSORolesResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesLambda.class);
    private final DistributeSSORolesService distributeSSORolesService;
    private final IamFacade iamFacade;

    @Inject
    public DistributeSSORolesLambda(DistributeSSORolesService distributeSSORolesService, IamFacade iamFacade) {
        this.distributeSSORolesService = distributeSSORolesService;
        this.iamFacade = iamFacade;
    }

    @Override
    public DistributeSSORolesResponse handleRequest(Map<String, Object> event, Context context) {
        LOGGER.info("Got event " + event);
        if (event.containsKey("eventName")) {
            String eventName = event.get("eventName").toString();
            if (eventName.equals("CreateRole")) {
                return distributeSSORolesService.handleCreateRoleEvent(CreateRoleEvent.create(event));
            } else if (eventName.equals("DeleteRole")) {
                return distributeSSORolesService.handleDeleteRoleEvent(DeleteRoleEvent.create(event));
            } else {
                throw new InvalidEventPayloadException("\"eventName\" field must be CreateRole or DeleteRole.");
            }
        } else if (event.containsKey("resources") && event.get("resources").toString().contains("-TriggerMonthly-")) {
            return distributeSSORolesService.handleMonthlyEvent(iamFacade.listAllRoles());
        } else {
            throw new InvalidEventPayloadException();
        }
    }
}

