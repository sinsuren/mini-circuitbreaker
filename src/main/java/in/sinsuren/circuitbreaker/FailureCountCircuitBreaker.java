package in.sinsuren.circuitbreaker;

import java.util.concurrent.Callable;

public class FailureCountCircuitBreaker<T> implements CircuitBreaker<T> {
  private enum State {
    CLOSED,
    OPEN,
    HALF_OPEN
  }

  private State state = State.CLOSED;
  private final int failureThreshold; // Number of failures before opening
  private int failureCount = 0;
  private final long openStateTimeout; // Timeout in milliseconds for the open state
  private long lastFailureTime;

  public FailureCountCircuitBreaker(int failureThreshold, long openStateTimeout) {
    this.failureThreshold = failureThreshold;
    this.openStateTimeout = openStateTimeout;
  }

  @Override
  public T execute(Callable<T> callable) throws Exception {
    if (state == State.OPEN) {
      if (isTimeoutReached()) {
        state = State.HALF_OPEN;
        System.out.println("Circuit Breaker is in HALF_OPEN state, testing system...");
      } else {
        System.out.println("Circuit Breaker is OPEN, rejecting request.");
        throw new RuntimeException("Circuit Breaker is OPEN. Request blocked.");
      }
    }

    try {
      // Attempt to execute the function
      T result = callable.call();
      recordSuccess();
      return result;
    } catch (Exception e) {
      recordFailure();
      throw new RuntimeException("Request failed", e);
    }
  }

  @Override
  public void recordFailure() {
    failureCount++;
    lastFailureTime = System.currentTimeMillis();

    if (failureCount >= failureThreshold) {
      state = State.OPEN;
      System.out.println(
          "Circuit Breaker switched to OPEN state due to " + failureThreshold + " failures.");
    }
  }

  @Override
  public void recordSuccess() {
    if (state == State.HALF_OPEN || state == State.CLOSED) {
      state = State.CLOSED;
      failureCount = 0; // Reset the failure count on success
      System.out.println("Circuit Breaker switched to CLOSED state.");
    }
  }

  @Override
  public String getState() {
    return state.name();
  }

  private boolean isTimeoutReached() {
    return System.currentTimeMillis() - lastFailureTime > openStateTimeout;
  }
}
