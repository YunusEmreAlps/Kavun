package com.kavun;

import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;


@ExtendWith(MockitoExtension.class)
class KavunApplicationTests {

  @Mock private transient ConfigurableApplicationContext context;

  @AfterEach
  void tearDown() {
    if (Objects.nonNull(context)) {
      context.close();
    }
  }

  @Test
  void testClassConstructor() {
    Assertions.assertDoesNotThrow(KavunApplication::new);
  }

  /** Test the main method with mocked application context. */
  @Test
  void contextLoads() {
    try (var mockStatic = Mockito.mockStatic(SpringApplication.class)) {
      mockStatic
          .when(() -> context = SpringApplication.run(KavunApplication.class))
          .thenReturn(context);

      KavunApplication.main(new String[] {});

      mockStatic.verify(() -> context = SpringApplication.run(KavunApplication.class));
    }
  }
}
