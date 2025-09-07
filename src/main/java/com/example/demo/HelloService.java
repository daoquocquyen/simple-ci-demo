package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class HelloService {
  public String normalizeName(String name) {
    if (name == null || name.trim().isEmpty()) return "World";
    return name.trim();
  }
  public String greet(String name) { return "Hello, " + normalizeName(name) + "!"; }
  public int add(int a, int b) { return a + b; }
}
