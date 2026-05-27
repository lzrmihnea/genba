package eu.px.genba.i18n;

import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Locale wiring for the API.
 *
 * <p>Resolves the request locale from the {@code Accept-Language} header,
 * accepting only the languages Genba ships translations for. Defaults to
 * English when the header is missing or asks for an unsupported locale.
 *
 * <p>The explicit {@link MessageSource} bean uses
 * {@link ReloadableResourceBundleMessageSource} so the bundles can be reloaded
 * without restart in dev — and, more importantly, it sidesteps a
 * {@code ResourceBundleMessageSource} caching quirk that prevented lookups
 * from finding {@code messages_en.properties} / {@code messages_ro.properties}
 * during initial bring-up.
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

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        source.setCacheSeconds(10);
        return source;
    }
}
