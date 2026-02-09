package com.test.platform.kernel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformExceptionTest {
  private enum SampleError implements ErrorCode {
    BAD_THING;

    @Override
    public String code() {
      return "BAD_THING";
    }

    @Override
    public String message() {
      return "Something went wrong";
    }
  }

  @Test
  void exposesCodeAndDetails() {
    PlatformException ex = new PlatformException(SampleError.BAD_THING, Map.of("k", "v"));
    assertEquals("BAD_THING", ex.getCode());
    assertEquals("Something went wrong", ex.getMessage());
    assertEquals("v", ex.getDetails().get("k"));
  }

  @Test
  void detailsAreUnmodifiable() {
    PlatformException ex = new PlatformException(SampleError.BAD_THING, Map.of("k", "v"));
    assertTrue(ex.getDetails().containsKey("k"));
    try {
      ex.getDetails().put("x", "y");
    } catch (UnsupportedOperationException expected) {
      return;
    }
    throw new AssertionError("details must be unmodifiable");
  }
}

