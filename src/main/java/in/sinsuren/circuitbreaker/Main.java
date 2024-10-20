package in.sinsuren.circuitbreaker;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    // Use a time-window-based circuit breaker
    CircuitBreaker<String> timeWindowBreaker =
        new TimeWindowCircuitBreaker<>(3, Duration.ofSeconds(10), Duration.ofSeconds(5));

    // Use a failure-count-based circuit breaker
    CircuitBreaker<String> failureCountBreaker = new FailureCountCircuitBreaker<>(3, 5000);

    Callable<String> riskyOperation =
        () -> {
          // Simulate failure with a random exception
          if (new Random().nextInt(10) < 7) {
            throw new RuntimeException("Service failure!");
          }
          return "Success!";
        };

    for (int i = 0; i < 10; i++) {
      try {
        System.out.println("TimeWindowBreaker - Attempt " + (i + 1));
        String result = timeWindowBreaker.execute(riskyOperation);
        System.out.println("Operation result: " + result);
      } catch (Exception e) {
        System.out.println("Operation failed: " + e.getMessage());
      }

      try {
        System.out.println("FailureCountBreaker - Attempt " + (i + 1));
        String result = failureCountBreaker.execute(riskyOperation);
        System.out.println("Operation result: " + result);
      } catch (Exception e) {
        System.out.println("Operation failed: " + e.getMessage());
      }

      // Simulate time passage
      Thread.sleep(2000); // 2 seconds delay between attempts
    }
  }
}
