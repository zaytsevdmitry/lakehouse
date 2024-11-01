package lakehouse.api.configuration;

import lakehouse.api.mapper.Mapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapper {
	@Bean
	Mapper getMapper() {
		return new Mapper();
	}
}
