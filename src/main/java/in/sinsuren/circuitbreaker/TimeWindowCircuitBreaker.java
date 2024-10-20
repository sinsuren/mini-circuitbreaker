package in.sinsuren.circuitbreaker;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;

public class TimeWindowCircuitBreaker<T> implements CircuitBreaker<T> {
  private enum State {
    CLOSED,
    OPEN,
    HALF_OPEN
  }

  private State state = State.CLOSED;
  private final int failureThreshold; // x failures
  private final Duration failureTimeWindow; // y unit of time
  private final Duration openStateTimeout; // Timeout for open state

  private Deque<Instant> failureTimestamps = new LinkedList<>();
  private Instant lastFailureTime;

  public TimeWindowCircuitBreaker(
      int failureThreshold, Duration failureTimeWindow, Duration openStateTimeout) {
    this.failureThreshold = failureThreshold;
    this.failureTimeWindow = failureTimeWindow;
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
      throw new RuntimeException("Request failed", e); // Rethrow exception after recording failure
    }
  }

  @Override
  public void recordFailure() {
    Instant now = Instant.now();
    failureTimestamps.add(now);
    lastFailureTime = now;

    // Remove failures outside the time window y
    while (!failureTimestamps.isEmpty()
        && Duration.between(failureTimestamps.peek(), now).compareTo(failureTimeWindow) > 0) {
      failureTimestamps.poll();
    }

    // Open circuit if failures exceed threshold in the time window
    if (failureTimestamps.size() >= failureThreshold) {
      state = State.OPEN;
      System.out.println(
          "Circuit Breaker switched to OPEN state due to "
              + failureThreshold
              + " failures in "
              + failureTimeWindow.getSeconds()
              + " seconds.");
    }
  }

  @Override
  public void recordSuccess() {
    if (state == State.HALF_OPEN || state == State.CLOSED) {
      state = State.CLOSED;
      failureTimestamps.clear(); // Reset the failure count on success
      System.out.println("Circuit Breaker switched to CLOSED state.");
    }
  }

  @Override
  public String getState() {
    return state.name();
  }

  private boolean isTimeoutReached() {
    if (lastFailureTime == null) {
      return false;
    }
    return Duration.between(lastFailureTime, Instant.now()).compareTo(openStateTimeout) > 0;
  }
}
