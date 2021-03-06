package com.eboy.honey.oss.config;

import com.eboy.honey.oss.api.HoneyOss;
import com.eboy.honey.oss.aspect.SecondTransAspect;
import com.eboy.honey.oss.client.HoneyMiniO;
import com.eboy.honey.oss.properties.MinioProperties;
import com.eboy.honey.oss.strategy.SecondTransStrategy;
import com.eboy.honey.oss.strategy.impl.Md5DigestAsHex;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * @author yangzhijie
 * @date 2020/11/4 10:38
 */
@Slf4j
@Configuration
@ConditionalOnClass(HoneyOss.class)
@Lazy
public class HoneyOssAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public HoneyOss honeyOss() {
        return new HoneyOss();
    }


    @Bean
    @ConditionalOnMissingBean
    public HoneyMiniO honeyMiniO() {
        return new HoneyMiniO();
    }

    @Bean
    public MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    public MinioClient minioClient(@Qualifier("minioProperties") MinioProperties minioProperties) {
        try {
            return new MinioClient(
                    minioProperties.getUrl(),
                    minioProperties.getAccessKey(),
                    minioProperties.getSecretKey()
            );
        } catch (InvalidEndpointException | InvalidPortException e) {
            log.warn("实例化MinioClient失败，原因：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Bean
    public SecondTransAspect secondTransAspect() {
        return new SecondTransAspect();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(Md5DigestAsHex.class)
    public SecondTransStrategy secondTransStrategy() {
        return new Md5DigestAsHex();
    }
}
