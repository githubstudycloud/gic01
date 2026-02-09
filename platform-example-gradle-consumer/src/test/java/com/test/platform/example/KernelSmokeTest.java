package com.test.platform.example;

import com.test.platform.kernel.error.ErrorCode;
import com.test.platform.kernel.error.PlatformException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class KernelSmokeTest {
  private enum Err implements ErrorCode {
    X;

    @Override
    public String code() {
      return "X";
    }

    @Override
    public String message() {
      return "x";
    }
  }

  @Test
  void canUseKernelFromGradle() {
    PlatformException ex = new PlatformException(Err.X);
    assertEquals("X", ex.getCode());
  }
}
