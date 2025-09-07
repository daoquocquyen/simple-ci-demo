package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelloApiIT {

    @Autowired
    TestRestTemplate http;

    @Test
    void hello_endpoint_returns_greeting() {
        var res = http.getForEntity("/hello?name=Bob", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("message")).isEqualTo("Hello, Bob!");
    }

    @Test
    void sum_endpoint_returns_result() {
        var req = new HttpEntity<>(Map.of("a", 2, "b", 5));
        var res = http.postForEntity("/hello/sum", req, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("result")).isEqualTo(7);
    }
}
