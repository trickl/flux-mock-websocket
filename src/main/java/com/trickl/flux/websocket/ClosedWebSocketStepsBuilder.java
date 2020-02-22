package com.trickl.flux.websocket;

import com.trickl.exceptions.StepVerifierException;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.scheduler.Scheduler;

@Log
@RequiredArgsConstructor
public final class ClosedWebSocketStepsBuilder {

  private final MockWebSocketListener mockWebSocketListener;

  private final Scheduler scheduler;

  private final Queue<Runnable> steps;

  /**
   * Expect the socket to be opened.
   *
   * @param timeout How long to wait
   */
  public OpenWebSocketStepsBuilder thenExpectOpen(Duration timeout) {
    steps.add(() -> testWasOpen(timeout));

    return new OpenWebSocketStepsBuilder(mockWebSocketListener, scheduler, steps);
  }

  protected void testWasOpen(Duration timeout) {
    synchronized (mockWebSocketListener.getSyncEvent()) {
      try {
        mockWebSocketListener.getSyncEvent().wait(timeout.toMillis());
        StepType nextStep = Optional.ofNullable(
            mockWebSocketListener.getSteps().poll()).orElse(StepType.NOTHING);
        if (!nextStep.equals(StepType.OPEN)) {
          throw new StepVerifierException(
              "Expected OPEN got - " + nextStep);
        }
      } catch (InterruptedException ex) {
        log.info("Wait interrupted.");
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Verify the steps ran. */
  public void verify() {
    scheduler.schedule(
        () -> {
          while (!steps.isEmpty()) {
            steps.remove().run();
          }
        });
  }
}
