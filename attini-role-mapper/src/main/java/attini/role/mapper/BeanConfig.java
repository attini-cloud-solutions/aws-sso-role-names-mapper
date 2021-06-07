package attini.role.mapper;

import javax.enterprise.context.ApplicationScoped;

public class BeanConfig {

    @ApplicationScoped
    public Test1 test(){
        return new Test1();
    }

    @ApplicationScoped
    public ProcessingService processingService(Test1 test) {
        return new ProcessingService(test);
    }
}
