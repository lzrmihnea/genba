package eu.px.genba.i18n;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's {@link MessageSource} resolving against the
 * current request locale (or any caller-supplied locale).
 *
 * <p>Convention: every i18n key is treated as authoritative; a missing key
 * surfaces the key itself rather than a localized fallback, so the gap is
 * visible during development.
 */
@Component
@RequiredArgsConstructor
public class MessageProvider {

    private static final List<Locale> SUPPORTED = List.of(
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("ro"));
    private static final Locale DEFAULT_LOCALE = SUPPORTED.get(0);

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return get(key, LocaleContextHolder.getLocale(), args);
    }

    public String get(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    /**
     * Resolve the supported request locale from the {@code Accept-Language}
     * header, falling back to English. Use this in flows that run BEFORE the
     * Spring MVC {@code LocaleContextResolver} populates
     * {@link LocaleContextHolder} — primarily the security filter chain and
     * the {@code @RestControllerAdvice} (which observes whatever locale was
     * set, but only when the handler completes the request normally).
     */
    public Locale resolveLocaleFor(HttpServletRequest request) {
        Locale requested = request.getLocale();
        if (requested == null) {
            return DEFAULT_LOCALE;
        }
        return SUPPORTED.stream()
                .filter(s -> s.getLanguage().equals(requested.getLanguage()))
                .findFirst()
                .orElse(DEFAULT_LOCALE);
    }

    public String getForRequest(HttpServletRequest request, String key, Object... args) {
        return get(key, resolveLocaleFor(request), args);
    }
}
