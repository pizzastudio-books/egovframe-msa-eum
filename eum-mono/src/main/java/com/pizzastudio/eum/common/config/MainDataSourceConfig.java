package com.pizzastudio.eum.common.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 이음 본체 데이터베이스.
 *
 * <p>타 기관 데이터소스를 함께 선언하는 순간 스프링 부트의 자동 설정이 물러난다.
 * 어느 것이 주 데이터소스인지 애플리케이션이 밝혀야 하고, 그러지 않으면
 * EntityManagerFactory 부터 만들어지지 않는다. 데이터소스를 여럿 두는 구조가 치르는
 * 첫 번째 비용이다.</p>
 */
@Configuration
public class MainDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}
