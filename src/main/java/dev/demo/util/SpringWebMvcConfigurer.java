package dev.demo.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Log4j2
public class SpringWebMvcConfigurer //
        implements WebMvcConfigurer {

    @Autowired
    private ApiLogInterceptor apiLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        log.info("init interceptors");

        registry.addInterceptor(apiLogInterceptor) //
                .order(10) //
                .addPathPatterns("/**") //
                .excludePathPatterns( //
                        "/", //
                        "/css/**", //
                        "/img/**", //
                        "/js/**", //
                        "/favicon.ico");
    }

}
