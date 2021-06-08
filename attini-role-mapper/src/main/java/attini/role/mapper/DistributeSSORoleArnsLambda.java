package attini.role.mapper;

import attini.role.mapper.domain.RoleName;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;

import javax.inject.Named;
import java.util.Objects;

@Named("DistributeSSORoleArnsLambda")
public class DistributeSSORoleArnsLambda implements RequestHandler<ScheduledEvent, String> {
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {



        // System.out.println("Got event " + event);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode details = objectMapper.valueToTree(event.getDetail());

        RoleName roleName = RoleName.create(details.get("requestParameters").get("roleName").asText());

        if(!roleName.getName().startsWith("AWSReservedSSO")) {
            System.err.println("Invalid event, please verify the cloudtrail filter");
            return "Failed";
        }

        String eventName = details.get("eventName").asText();

        // Get regions

        String regions = getRegions();
        System.out.println(regions);


        if(eventName == "CreateRole") {
            String RoleArn = details.get("responseElements").get("role").get("arn").asText();


        }





        // System.out.println(details.get("requestParameters").get("roleName").asText());

        // System.out.println("Got event details " + event);


        // Kom ihåg: Hämta regioner från parameter store ist för ec2

        return "hej\n";
    }


    public String getRegions() {
        SsmClient ssmClient = SsmClient.builder().region(Region.EU_WEST_1).build();
        GetParametersByPathRequest path = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/services/athena/regions").build();

        GetParametersByPathResponse response = ssmClient.getParametersByPath(path);
        // return response.toString();
        return "tjohej";
    }
}
