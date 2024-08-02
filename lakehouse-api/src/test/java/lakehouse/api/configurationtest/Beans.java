package lakehouse.api.configurationtest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Beans {

    @Bean(name = "restManipulator")
    public RestManipulator getRestManipulator() {
        return new RestManipulator();
    }
}
