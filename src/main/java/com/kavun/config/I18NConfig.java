package com.kavun.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Defines the Internationalization configuration.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class I18NConfig implements WebMvcConfigurer {

  /** The number of seconds for the message source to reload. */
  private static final int CACHE_MILLIS = 1800;

  /** Languages with a message bundle; extend together with the bundles under i18n/. */
  private static final List<Locale> SUPPORTED_LOCALES =
      List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"));
  private static final String MESSAGES_LOCATION = "classpath:i18n/messages";

  /**
   * Setup properties message source.
   *
   * @return messageSource.
   */
  @Bean
  public ReloadableResourceBundleMessageSource messageSource() {
    var messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename(MESSAGES_LOCATION);
    messageSource.setCacheMillis(CACHE_MILLIS);
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    // Unsupported locales resolve to the default bundle, not to whatever the host OS locale is.
    messageSource.setFallbackToSystemLocale(false);

    return messageSource;
  }

  /**
   * Resolves the locale from the Accept-Language header, the standard for an API consumed by
   * separate frontends. Unsupported languages fall back to English.
   *
   * @return localeResolver.
   */
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setSupportedLocales(SUPPORTED_LOCALES);
    localeResolver.setDefaultLocale(Locale.ENGLISH);

    return localeResolver;
  }

  @Bean
  public StringHttpMessageConverter stringHttpMessageConverter() {
    StringHttpMessageConverter messageConverter =
        new StringHttpMessageConverter(StandardCharsets.UTF_8);
    messageConverter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN));
    return messageConverter;
  }
}
