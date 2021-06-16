package attini.role.mapper;

import attini.role.mapper.domain.*;
import attini.role.mapper.services.DistributeSSORolesService;
import attini.role.mapper.facades.IamFacade;
import attini.role.mapper.facades.SsmFacade;
import com.amazonaws.services.acmpca.model.InvalidArgsException;
import com.amazonaws.services.kinesisanalytics.model.InvalidArgumentException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

// Compile with: mvn clean package -Pnative -Dquarkus.native.container-build=true

// aws s3 cp target/function.zip s3://attini-artifact-store-us-east-1-855066048591/attini/tmp/labb/function.zip
// aws cloudformation delete-stack --stack-name joel-test
// aws cloudformation deploy --template cf-template.yaml --stack-name joel-test --capabilites CAPABILITY_IAM
// mvn clean package && sam local invoke -t target/sam.jvm.yaml -e payload.json
//
@Named("DistributeSSORolesLambda")
public class DistributeSSORolesLambda implements RequestHandler<ScheduledEvent, DistributeSSORolesResponse> {

    private final static Logger LOGGER = Logger.getLogger(DistributeSSORolesLambda.class);

    @Inject
    public DistributeSSORolesLambda(DistributeSSORolesService distributeSSORolesService, SsmFacade ssmFacade, IamFacade iamFacade) {
        this.distributeSSORolesService = distributeSSORolesService;
        this.ssmFacade = ssmFacade;
        this.iamFacade = iamFacade;
    }

    private final DistributeSSORolesService distributeSSORolesService;
    private final SsmFacade ssmFacade;
    private final IamFacade iamFacade;

    @Override
    public DistributeSSORolesResponse handleRequest(ScheduledEvent event, Context context) {

        // TODO: Koll att event är fine, validera event, skapa class?

        LOGGER.info("Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());
        if (event.getResources().size() != 1) {
            throw new IllegalArgumentException("Resource array in json payload must contain exactly one element.");
        } else if (event.getResources().get(0).contains("-TriggerMonthly-")) {
            return distributeSSORolesService.monthlyCleanup(iamFacade.listAllRoles());
        } else {
            return handleEventTrigger(details);
        }
    }
}

