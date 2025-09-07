package hr.tvz.ar_zoo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/models/**")
                .addResourceLocations("file:D:/Users/User/Desktop/DIPLOMSKI/AR Modeli/")
                .setCachePeriod(0);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:D:/Users/User/Desktop/DIPLOMSKI/slike")
                .setCachePeriod(0);
    }
}
