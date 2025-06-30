package com.techishthoughts.stocks.config;

import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.techishthoughts.stocks.adapter.out.mapper.FinnhubMapper;

/**
 * Configuration for MapStruct mappers.
 * Explicitly registers mappers as Spring beans.
 */
@Configuration
public class MapperConfig {

    @Bean
    public FinnhubMapper finnhubMapper() {
        return Mappers.getMapper(FinnhubMapper.class);
    }
}
