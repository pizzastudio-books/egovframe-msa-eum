package com.pizzastudio.eum.core.external.legacy;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import lombok.extern.slf4j.Slf4j;

/**
 * 타 기관 데이터베이스 직결.
 *
 * <p>연계 미들웨어 없이 이음이 주변 기관의 데이터베이스에 직접 붙는다. 공공에서 아주 흔한
 * 형태이고, 실제 조사 표본에서는 한 애플리케이션이 데이터소스를 일곱 개까지 선언하고
 * 있었다. 여기서는 둘로 줄여 재현한다.</p>
 *
 * <p><b>이 구조가 서비스 분리에서 가장 먼저 막힌다.</b> 신청·심사를 떼어내려 해도 이
 * 데이터소스들이 따라붙고, 어느 서비스가 어느 기관 DB를 소유하는지 정할 수가 없다.
 * 13.1에서 이 직결을 연계 API 로 바꾼다.</p>
 */
@Slf4j
@Configuration
// 연계 API 로 바꾸면(13.1) 이 데이터소스들도 함께 사라져야 한다. 남겨 두면 쓰지도 않는
// 상대 기관 DB 에 계속 붙어 있고, 접속 계정도 계속 들고 있어야 한다.
@ConditionalOnProperty(name = "eum.legacy.mode", havingValue = "datasource", matchIfMissing = true)
public class LegacyDataSourceConfig {

    @Value("${eum.legacy.bizinfo.url:jdbc:h2:mem:bizinfo;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false}")
    private String bizInfoUrl;

    @Value("${eum.legacy.taxinfo.url:jdbc:h2:mem:taxinfo;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false}")
    private String taxInfoUrl;

    @Value("${eum.legacy.username:sa}")
    private String legacyUsername;

    @Value("${eum.legacy.password:}")
    private String legacyPassword;

    /** 가상 행정정보 — 사업자 등록 정보 */
    @Bean(name = "bizInfoDataSource")
    DataSource bizInfoDataSource() {
        return build(bizInfoUrl);
    }

    /** 가상 국세 정보 — 체납 내역 */
    @Bean(name = "taxInfoDataSource")
    DataSource taxInfoDataSource() {
        return build(taxInfoUrl);
    }

    private DataSource build(String url) {
        return DataSourceBuilder.create()
            .url(url)
            .username(legacyUsername)
            .password(legacyPassword)
            .driverClassName(url.startsWith("jdbc:mysql") ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver")
            .build();
    }

    @Bean(name = "bizInfoJdbcTemplate")
    JdbcTemplate bizInfoJdbcTemplate(@Qualifier("bizInfoDataSource") DataSource bizInfoDataSource) {
        return new JdbcTemplate(bizInfoDataSource);
    }

    @Bean(name = "taxInfoJdbcTemplate")
    JdbcTemplate taxInfoJdbcTemplate(@Qualifier("taxInfoDataSource") DataSource taxInfoDataSource) {
        return new JdbcTemplate(taxInfoDataSource);
    }

    /**
     * 실습용 — 타 기관 DB 를 흉내 내는 자료를 넣는다.
     *
     * <p>실제로는 상대 기관이 만들고 이음은 조회 권한만 받는다. MySQL 로 띄울 때는
     * 컴포즈 초기화 스크립트가 대신 만들므로 이 초기화를 끈다.</p>
     */
    @Bean
    @ConditionalOnProperty(name = "eum.legacy.init", havingValue = "true", matchIfMissing = true)
    DataSourceInitializer bizInfoInitializer(@Qualifier("bizInfoDataSource") DataSource bizInfoDataSource) {
        return initializer(bizInfoDataSource, "legacy/bizinfo.sql");
    }

    @Bean
    @ConditionalOnProperty(name = "eum.legacy.init", havingValue = "true", matchIfMissing = true)
    DataSourceInitializer taxInfoInitializer(@Qualifier("taxInfoDataSource") DataSource taxInfoDataSource) {
        return initializer(taxInfoDataSource, "legacy/taxinfo.sql");
    }

    private DataSourceInitializer initializer(DataSource dataSource, String script) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(script));
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
