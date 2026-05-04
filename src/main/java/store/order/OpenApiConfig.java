package store.order;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Order API")
                .version("1.0.0")
                .description("Manages customer orders, integrating with Product and Exchange services."));
    }
}
