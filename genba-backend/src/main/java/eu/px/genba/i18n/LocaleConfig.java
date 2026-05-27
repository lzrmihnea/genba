package eu.px.genba.i18n;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Locale wiring for the API.
 *
 * <p>Resolves the request locale from the {@code Accept-Language} header,
 * accepting only the languages Genba ships translations for. Defaults to
 * English when the header is missing or asks for an unsupported locale.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final Locale ENGLISH = Locale.forLanguageTag("en");
    public static final Locale ROMANIAN = Locale.forLanguageTag("ro");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(ENGLISH, ROMANIAN));
        resolver.setDefaultLocale(ENGLISH);
        return resolver;
    }
}
