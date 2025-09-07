package com.example.demo;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RestController
@RequestMapping("/hello")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "HelloService is a Spring-managed bean and safe to inject")
public class HelloController {
  private final HelloService service;

  // Spring will automatically inject HelloService
  public HelloController(HelloService service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, String> hello(@RequestParam(value = "name", required = false) String name) {
    return Map.of("message", service.greet(name));
  }

  @PostMapping("/sum")
  public ResponseEntity<Map<String, Integer>> sum(@RequestBody Map<String, Integer> body) {
    int a = body.getOrDefault("a", 0);
    int b = body.getOrDefault("b", 0);
    return ResponseEntity.ok(Map.of("result", service.add(a, b)));
  }
}
