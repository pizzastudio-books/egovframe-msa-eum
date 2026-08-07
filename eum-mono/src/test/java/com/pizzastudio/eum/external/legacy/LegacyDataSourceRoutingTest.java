package com.pizzastudio.eum.external.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 타 기관 조회가 본체 데이터소스로 새지 않는지 확인한다.
 *
 * <p>이 시험이 없을 때 실제로 겪은 일이다. {@code JdbcTemplate bizInfoJdbcTemplate(DataSource
 * bizInfoDataSource)} 처럼 파라미터 이름만으로 주입하면, 본체 데이터소스에 {@code @Primary}
 * 가 붙는 순간 이름 매칭이 아니라 {@code @Primary} 가 이긴다. 그러면 타 기관 조회가 조용히
 * 본체 데이터베이스로 간다.</p>
 *
 * <p>H2 로 띄우면 드러나지 않는다. 실습에서는 초기화 스크립트가 같은 인메모리 데이터베이스에
 * 타 기관 테이블까지 만들어 두기 때문에 조회가 그냥 된다. MySQL 로 띄우면 그제야
 * {@code Table 'eum.biz_registration' doesn't exist} 로 터진다.</p>
 *
 * <p>그래서 "붙는지"가 아니라 "어디에 붙었는지"를 본다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("타 기관 데이터소스 배선")
class LegacyDataSourceRoutingTest {

    @Autowired
    @Qualifier("bizInfoJdbcTemplate")
    private JdbcTemplate bizInfoJdbcTemplate;

    @Autowired
    @Qualifier("taxInfoJdbcTemplate")
    private JdbcTemplate taxInfoJdbcTemplate;

    @Autowired
    @Qualifier("bizInfoDataSource")
    private DataSource bizInfoDataSource;

    @Autowired
    @Qualifier("taxInfoDataSource")
    private DataSource taxInfoDataSource;

    @Autowired
    private DataSource primaryDataSource;

    @Test
    @DisplayName("사업자 조회용 JdbcTemplate 은 본체가 아니라 bizinfo 에 붙는다")
    void bizInfoTemplateUsesItsOwnDataSource() {
        assertThat(bizInfoJdbcTemplate.getDataSource()).isSameAs(bizInfoDataSource);
        assertThat(bizInfoJdbcTemplate.getDataSource()).isNotSameAs(primaryDataSource);
    }

    @Test
    @DisplayName("체납 조회용 JdbcTemplate 은 본체가 아니라 taxinfo 에 붙는다")
    void taxInfoTemplateUsesItsOwnDataSource() {
        assertThat(taxInfoJdbcTemplate.getDataSource()).isSameAs(taxInfoDataSource);
        assertThat(taxInfoJdbcTemplate.getDataSource()).isNotSameAs(primaryDataSource);
    }

    @Test
    @DisplayName("세 데이터소스가 서로 다른 데이터베이스를 본다")
    void threeDataSourcesPointToDifferentDatabases() throws Exception {
        String main = urlOf(primaryDataSource);
        String biz = urlOf(bizInfoDataSource);
        String tax = urlOf(taxInfoDataSource);

        assertThat(main).isNotEqualTo(biz);
        assertThat(main).isNotEqualTo(tax);
        assertThat(biz).isNotEqualTo(tax);
    }

    private String urlOf(DataSource dataSource) throws Exception {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }
}
