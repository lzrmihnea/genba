package eu.px.genba;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableCaching
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class GenbaBackendApplication {

    private final Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(GenbaBackendApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStarted(ContextRefreshedEvent event) {
        if (event.getApplicationContext() instanceof ServletWebServerApplicationContext context) {
            int port = context.getWebServer().getPort();
            String host = environment.getProperty("server.host", "localhost");

            log.info("=================================================================");
            log.info("Genba backend started");
            log.info("Local URL:    http://{}:{}", host, port);
            log.info("Swagger UI:   http://{}:{}/swagger-ui/index.html", host, port);
            log.info("OpenAPI docs: http://{}:{}/v3/api-docs", host, port);
            log.info("Actuator:     http://{}:{}/actuator/health", host, port);
            log.info("=================================================================");
        }
    }
}
