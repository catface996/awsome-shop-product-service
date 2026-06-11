package com.awsome.shop.product.bootstrap.it;

import com.awsome.shop.product.bootstrap.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内网鉴权过滤器 E2E 测试（HTTP 入口）。
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalAuthFilterE2ETest extends AbstractMysqlIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<String> postStockGet(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("X-Internal-Token", token);
        }
        HttpEntity<String> entity = new HttpEntity<>("{\"productId\":1}", headers);
        return restTemplate.exchange("/api/v1/private/stock/get", HttpMethod.POST, entity, String.class);
    }

    @Test
    void privateEndpoint_withoutToken_returns401() {
        assertThat(postStockGet(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void privateEndpoint_withInvalidToken_returns401() {
        assertThat(postStockGet("wrong-token").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void privateEndpoint_withValidToken_passesFilter() {
        // dev-internal-token 是默认配置值；通过过滤器后即使业务结果不同，也不应是 401
        assertThat(postStockGet("dev-internal-token").getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicEndpoint_unaffectedByFilter() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"page\":1,\"size\":10}", headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/public/product/list", HttpMethod.POST, entity, String.class);
        assertThat(resp.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
