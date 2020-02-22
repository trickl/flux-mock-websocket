package com.trickl.flux.websocket;

import com.trickl.exceptions.StepVerifierException;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import okhttp3.WebSocket;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.scheduler.Scheduler;

@Log
@RequiredArgsConstructor
public final class OpenWebSocketStepsBuilder {

  private final MockWebSocketListener mockWebSocketListener;

  private final Scheduler scheduler;

  private final Queue<Runnable> steps;

  private static final String WAIT_INTERRUPTED_MESSAGE = "Wait Interrupted";

  /**
   * Schedule sending a websocket message.
   *
   * @param payload The message payloadfto send
   */
  public OpenWebSocketStepsBuilder thenSend(final String payload) {
    steps.add(
        () -> {
          WebSocket ws = mockWebSocketListener.getWebSocket();
          if (ws != null) {
            ws.send(payload);
          }
        });
    return this;
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param body expected message body
   * @param timeout How long to wait
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(String body, Duration timeout) {
    return thenExpectMessage(text -> text.equals(body), timeout);
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param bodyPattern test for message content
   * @param timeout How long to wait
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(Pattern bodyPattern, Duration timeout) {
    return thenExpectMessage(text -> bodyPattern.matcher(text).matches(), timeout);
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param bodyMatcher test for message content
   * @param timeout How long to wait
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(
      Predicate<String> bodyMatcher, Duration timeout) {
    steps.add(() -> testWasMessage(bodyMatcher, timeout));
    return this;
  }

  protected void testWasMessage(Predicate<String> bodyMatcher, Duration timeout) {
    synchronized (mockWebSocketListener.getSyncEvent()) {
      try {
        mockWebSocketListener.getSyncEvent().wait(timeout.toMillis());
        StepType nextStep = Optional.ofNullable(
            mockWebSocketListener.getSteps().poll()).orElse(StepType.NOTHING);
        String nextMessage = Optional.ofNullable(
            mockWebSocketListener.getMessages().poll()).orElse("<null>");              
        if (!nextStep.equals(StepType.MESSAGE)) {
          throw new StepVerifierException(
              "Expected MESSAGE got - " + nextStep);
        } else if (!bodyMatcher.test(nextMessage)) {
          throw new StepVerifierException(
              "Unexpected message - " + nextMessage);
        }
      } catch (InterruptedException ex) {
        log.info(WAIT_INTERRUPTED_MESSAGE);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Wait a period of time doing nothing.
   * 
   * @param period how long to wait
   */
  public OpenWebSocketStepsBuilder thenWait(Duration period) {
    steps.add(
        () -> {
          final CountDownLatch latch = new CountDownLatch(1);
          try {
            latch.await(period.toMillis(), TimeUnit.MILLISECONDS);
          } catch (InterruptedException ex) {
            log.info(WAIT_INTERRUPTED_MESSAGE);
            Thread.currentThread().interrupt();
          }
        });
    return this;
  }

  /**
   * Expect the socket to be closed.
   *
   * @param timeout How long to wait
   */
  public void thenExpectCloseAndVerify(Duration timeout) {
    steps.add(
        () -> {
          synchronized (mockWebSocketListener.getSyncEvent()) {
            try {
              mockWebSocketListener.getSyncEvent().wait(timeout.toMillis());
              StepType nextStep = Optional.ofNullable(
                  mockWebSocketListener.getSteps().poll()).orElse(StepType.NOTHING);
              if (!nextStep.equals(StepType.CLOSE)) {
                throw new StepVerifierException(
                    "Expected CLOSE got - " + nextStep);
              }
            } catch (InterruptedException ex) {
              log.info(WAIT_INTERRUPTED_MESSAGE);
              Thread.currentThread().interrupt();
            }
          }
        });

    new ClosedWebSocketStepsBuilder(mockWebSocketListener, scheduler, steps).verify();
  }

  /** Close the connection. */
  public void thenCloseAndVerify() {
    steps.add(
        () -> {
          WebSocket ws = mockWebSocketListener.getWebSocket();
          if (ws != null) {
            log.info("Terminating connection.");
            ws.close(CloseStatus.NORMAL.getCode(), "Normal termination.");
          }
        });

    new ClosedWebSocketStepsBuilder(mockWebSocketListener, scheduler, steps).verify();
  }
}
