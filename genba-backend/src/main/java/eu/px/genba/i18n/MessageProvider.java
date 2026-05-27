package eu.px.genba.i18n;

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

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return get(key, LocaleContextHolder.getLocale(), args);
    }

    public String get(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
