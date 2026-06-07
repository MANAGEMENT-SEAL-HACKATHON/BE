package com.sealhackathon.api.config.seed;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DevSeedProperties.class)
public class DevSeedConfiguration {
}
