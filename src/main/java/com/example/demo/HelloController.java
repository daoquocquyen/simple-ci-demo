package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/hello")
public class HelloController {
  private final HelloService service;
  public HelloController(HelloService service) { this.service = service; }

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
