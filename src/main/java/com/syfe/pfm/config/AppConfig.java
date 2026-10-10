package com.syfe.pfm.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** General application beans. */
@Configuration
public class AppConfig {

    /** Clock used for all "today" decisions; zone is configurable via {@code app.timezone}. */
    @Bean
    public Clock clock(@Value("${app.timezone:UTC}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
