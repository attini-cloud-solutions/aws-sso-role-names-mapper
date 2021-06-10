package attini.role.mapper;

import javax.enterprise.context.ApplicationScoped;

public class BeanConfig {
    
    @ApplicationScoped
    public SsmService SsmService() {
        return new SsmService();
    };
}
