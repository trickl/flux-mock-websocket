package com.trickl.flux.websocket;

import com.trickl.exceptions.StepVerifierException;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.scheduler.Scheduler;

@Log
@RequiredArgsConstructor
public final class ClosedWebSocketStepsBuilder {

  private final Supplier<MockWebServer> serverSupplier;

  private final MockWebServerListener mockWebServerListener;

  private final MockWebSocketListener mockWebSocketListener;

  private final Scheduler scheduler;

  private final Queue<Runnable> steps;

  private static final String WAIT_INTERRUPTED_MESSAGE = "Wait Interrupted";

  /** Wait for the server to be started and available. */
  public ClosedWebSocketStepsBuilder thenWaitServerShutdown() {
    return thenWaitServerShutdown(Duration.ofSeconds(10));
  }

  /** Wait for the server to be started and available. */
  public ClosedWebSocketStepsBuilder thenWaitServerShutdown(Duration timeout) {
    steps.add(() -> testWasShutdown(timeout));

    return this;
  }

  protected void testWasShutdown(Duration timeout) {
    synchronized (mockWebServerListener.getSyncEvent()) {
      try {
        log.info("Waiting on SERVER_SHUTDOWN");
        mockWebServerListener.getSyncEvent().wait(timeout.toMillis());
        WebServerStepType nextStep =
            Optional.ofNullable(mockWebServerListener.getSteps().poll())
                .orElse(WebServerStepType.NOTHING);
        if (!nextStep.equals(WebServerStepType.SERVER_SHUTDOWN)) {
          throw new StepVerifierException("Expected SERVER_SHUTDOWN got - " + nextStep);
        }
      } catch (InterruptedException ex) {
        log.info(WAIT_INTERRUPTED_MESSAGE);
        Thread.currentThread().interrupt();
      }
    }
  }


  /** Wait for the server to be started and available. */
  public ClosedWebSocketStepsBuilder thenWaitServerStartThenUpgrade() {
    return thenWaitServerStartThenUpgrade(Duration.ofSeconds(10));
  }

  /** Wait for the server to be started and available. */
  public ClosedWebSocketStepsBuilder thenWaitServerStartThenUpgrade(Duration timeout) {
    steps.add(
        () -> {
          testWasStarted(timeout);
          MockResponse response = new MockResponse().withWebSocketUpgrade(mockWebSocketListener);
          serverSupplier.get().enqueue(response);
        });

    return this;
  }

  protected void testWasStarted(Duration timeout) {
    synchronized (mockWebServerListener.getSyncEvent()) {
      try {
        log.info("Waiting on SERVER_START");
        mockWebServerListener.getSyncEvent().wait(timeout.toMillis());
        WebServerStepType nextStep =
            Optional.ofNullable(mockWebServerListener.getSteps().poll())
                .orElse(WebServerStepType.NOTHING);
        if (!nextStep.equals(WebServerStepType.SERVER_START)) {
          throw new StepVerifierException("Expected SERVER_START got - " + nextStep);
        }
      } catch (InterruptedException ex) {
        log.info(WAIT_INTERRUPTED_MESSAGE);
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Expect the socket to be opened. */
  public OpenWebSocketStepsBuilder thenExpectOpen() {
    return thenExpectOpen(Duration.ofSeconds(10));
  }

  /**
   * Expect the socket to be opened.
   *
   * @param timeout How long to wait
   */
  public OpenWebSocketStepsBuilder thenExpectOpen(Duration timeout) {
    steps.add(() -> testWasOpen(timeout));

    return new OpenWebSocketStepsBuilder(
        serverSupplier, mockWebServerListener, mockWebSocketListener, scheduler, steps);
  }

  protected void testWasOpen(Duration timeout) {
    synchronized (mockWebSocketListener.getSyncEvent()) {
      try {
        log.info("Waiting on OPEN");
        mockWebSocketListener.getSyncEvent().wait(timeout.toMillis());
        WebSocketStepType nextStep =
            Optional.ofNullable(mockWebSocketListener.getSteps().poll())
                .orElse(WebSocketStepType.NOTHING);
        if (!nextStep.equals(WebSocketStepType.OPEN)) {
          throw new StepVerifierException("Expected OPEN got - " + nextStep);
        }
      } catch (InterruptedException ex) {
        log.info(WAIT_INTERRUPTED_MESSAGE);
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Verify the steps ran. */
  public void thenVerify() {
    scheduler.schedule(
        () -> {
          while (!steps.isEmpty()) {
            steps.remove().run();
          }
        });
  }
}
