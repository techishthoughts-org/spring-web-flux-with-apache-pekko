package com.techishthoughts.stocks.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MapStruct mappers.
 * Explicitly registers mappers as Spring beans.
 */
@Configuration
public class MapperConfig {

    // Temporarily disabled due to MapStruct compilation issues
    // @Bean
    // public FinnhubMapper finnhubMapper() {
    //     return Mappers.getMapper(FinnhubMapper.class);
    // }
}
