package in.sinsuren.circuitbreaker;

import java.util.concurrent.Callable;

public interface CircuitBreaker<T> {
  // Executes the passed function with Circuit Breaker logic
  T execute(Callable<T> callable) throws Exception;

  // Record the failure for custom strategy implementations
  void recordFailure();

  // Record the success for custom strategy implementations
  void recordSuccess();

  // Get the current state of the Circuit Breaker (Open, Closed, Half-Open)
  String getState();
}
