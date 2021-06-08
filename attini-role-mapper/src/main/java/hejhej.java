import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;

public class hejhej {
    public static void main(String[] args) {


        SsmClient ssmClient = SsmClient.create();
        GetParametersByPathRequest path = GetParametersByPathRequest.builder().path("/aws/service/global-infrastructure/services/athena/regions").build();

        GetParametersByPathResponse response = ssmClient.getParametersByPath(path);
    }
}