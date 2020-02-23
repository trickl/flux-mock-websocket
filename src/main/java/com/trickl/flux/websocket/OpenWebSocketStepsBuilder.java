package com.trickl.flux.websocket;

import com.trickl.exceptions.StepVerifierException;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import okhttp3.WebSocket;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.scheduler.Scheduler;

@Log
@RequiredArgsConstructor
public final class OpenWebSocketStepsBuilder {

  private final Supplier<MockWebServer> serverSupplier;

  private final MockWebServerListener mockWebServerListener;

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
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(String body) {
    return thenExpectMessage(body, Duration.ofSeconds(10));
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
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(Pattern bodyPattern) {
    return thenExpectMessage(bodyPattern, Duration.ofSeconds(10));
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
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(Predicate<String> bodyMatcher) {
    return thenExpectMessage(bodyMatcher, Duration.ofSeconds(10));
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
        log.info("Waiting on MESSAGE");
        mockWebSocketListener.getSyncEvent().wait(timeout.toMillis());
        WebSocketStepType nextStep =
            Optional.ofNullable(mockWebSocketListener.getSteps().poll())
                .orElse(WebSocketStepType.NOTHING);
        String nextMessage =
            Optional.ofNullable(mockWebSocketListener.getMessages().poll()).orElse("<null>");
        if (!nextStep.equals(WebSocketStepType.MESSAGE)) {
          throw new StepVerifierException("Expected MESSAGE got - " + nextStep);
        } else if (!bodyMatcher.test(nextMessage)) {
          throw new StepVerifierException("Unexpected message - " + nextMessage);
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
          try {
            log.info("Waiting for " + period);
            TimeUnit.MILLISECONDS.sleep(period.toMillis());
          } catch (InterruptedException ex) {
            log.info(WAIT_INTERRUPTED_MESSAGE);
            Thread.currentThread().interrupt();
          }
        });
    return this;
  }

  /** Expect the socket to be closed. */
  public ClosedWebSocketStepsBuilder thenExpectClose() {
    return thenExpectClose(Duration.ofSeconds(10));
  }

  /**
   * Expect the socket to be closed.
   *
   * @param timeout How long to wait
   */
  public ClosedWebSocketStepsBuilder thenExpectClose(Duration timeout) {
    steps.add(
        () -> {
          synchronized (mockWebSocketListener.getSyncEvent()) {
            try {
              log.info("Waiting on CLOSE");
              mockWebSocketListener.getSyncEvent().wait(timeout.toMillis());
              WebSocketStepType nextStep =
                  Optional.ofNullable(mockWebSocketListener.getSteps().poll())
                      .orElse(WebSocketStepType.NOTHING);
              if (!nextStep.equals(WebSocketStepType.CLOSE)) {
                throw new StepVerifierException("Expected CLOSE got - " + nextStep);
              }
            } catch (InterruptedException ex) {
              log.info(WAIT_INTERRUPTED_MESSAGE);
              Thread.currentThread().interrupt();
            }
          }
        });

    return new ClosedWebSocketStepsBuilder(
        serverSupplier, mockWebServerListener, mockWebSocketListener, scheduler, steps);
  }

  /** Close the connection. */
  public ClosedWebSocketStepsBuilder thenClose() {
    steps.add(
        () -> {
          WebSocket ws = mockWebSocketListener.getWebSocket();
          if (ws != null) {
            log.info("Terminating connection.");
            ws.close(CloseStatus.NORMAL.getCode(), "Normal termination.");
          }
        });

    return new ClosedWebSocketStepsBuilder(
        serverSupplier, mockWebServerListener, mockWebSocketListener, scheduler, steps);
  }
}
