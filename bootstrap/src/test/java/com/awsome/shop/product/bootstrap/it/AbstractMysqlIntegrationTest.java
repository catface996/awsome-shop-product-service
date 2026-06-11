package com.awsome.shop.product.bootstrap.it;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基类：启动 MySQL 8.4 容器，Flyway 在上下文启动时应用 V2–V5 迁移。
 *
 * <p>采用「单例容器」模式：容器在静态块中启动一次、整个 JVM 生命周期内复用、不在每个
 * 测试类结束后停止——这样跨测试类共享同一个 Spring 缓存上下文时数据源始终有效。</p>
 *
 * <p>{@code @Testcontainers(disabledWithoutDocker = true)} 使得在无法发现 Docker 的
 * 环境下这些集成测试被自动跳过（而非失败）；静态启动也通过 {@code isDockerAvailable()}
 * 守卫，避免在无 Docker 时于类加载阶段抛错。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMysqlIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("awsome_shop_product")
            .withUsername("root")
            .withPassword("root");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            MYSQL.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}
