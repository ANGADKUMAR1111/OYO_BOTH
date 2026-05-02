package com.oyo.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Allow all concrete types our codebase produces (PageImpl, HotelResponse, etc.)
        // plus standard JDK collections so Jackson can reconstruct the right class on read.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.oyo.backend")
                .allowIfSubType("org.springframework.data.domain")
                .allowIfSubType("java.util")
                .allowIfSubType("java.lang")
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Embed "@class" type metadata so Redis can deserialize back to PageImpl/HotelResponse
        // instead of falling back to LinkedHashMap, which causes ClassCastException.
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // Mixins to allow Jackson to instantiate Spring Data PageImpl and related classes
        objectMapper.addMixIn(org.springframework.data.domain.PageImpl.class, PageImplMixin.class);
        objectMapper.addMixIn(org.springframework.data.domain.PageRequest.class, PageRequestMixin.class);
        objectMapper.addMixIn(org.springframework.data.domain.Sort.class, SortMixin.class);
        objectMapper.addMixIn(org.springframework.data.domain.Sort.Order.class, SortOrderMixin.class);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration("adminStats",   defaultCacheConfig.entryTtl(Duration.ofMinutes(1)))
                .withCacheConfiguration("cities",       defaultCacheConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("featuredHotels", defaultCacheConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("defaultHotels",  defaultCacheConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(RedisCacheManager.class)
    public org.springframework.cache.CacheManager fallbackCacheManager() {
        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class PageImplMixin<T> {
        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES)
        PageImplMixin(
                @com.fasterxml.jackson.annotation.JsonProperty("content") java.util.List<T> content,
                @com.fasterxml.jackson.annotation.JsonProperty("pageable") org.springframework.data.domain.Pageable pageable,
                @com.fasterxml.jackson.annotation.JsonProperty("totalElements") long total) {}
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class PageRequestMixin {
        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES)
        PageRequestMixin(
                @com.fasterxml.jackson.annotation.JsonProperty("pageNumber") int page,
                @com.fasterxml.jackson.annotation.JsonProperty("pageSize") int size,
                @com.fasterxml.jackson.annotation.JsonProperty("sort") org.springframework.data.domain.Sort sort) {}
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class SortMixin {
        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES)
        SortMixin(@com.fasterxml.jackson.annotation.JsonProperty("orders") java.util.List<org.springframework.data.domain.Sort.Order> orders) {}
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private abstract static class SortOrderMixin {
        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES)
        SortOrderMixin(
                @com.fasterxml.jackson.annotation.JsonProperty("direction") org.springframework.data.domain.Sort.Direction direction,
                @com.fasterxml.jackson.annotation.JsonProperty("property") String property) {}
    }
}

