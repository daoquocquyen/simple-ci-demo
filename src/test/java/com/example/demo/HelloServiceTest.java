package com.example.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelloServiceTest {
  private final HelloService svc = new HelloService();

  @Test void greet_defaults_to_world_when_blank() {
    assertEquals("Hello, World!", svc.greet(null));
    assertEquals("Hello, World!", svc.greet("   "));
  }

  @Test void greet_trims_and_inserts_name() {
    assertEquals("Hello, Alice!", svc.greet(" Alice "));
  }

  @Test void add_adds_two_integers() {
    assertEquals(7, svc.add(3, 4));
  }
}
