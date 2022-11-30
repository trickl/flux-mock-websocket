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
   * @return The verifier builder
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
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(String body) {
    return thenExpectMessage(body, Duration.ofSeconds(10));
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param body expected message body
   * @param timeout How long to wait
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(String body, Duration timeout) {
    return thenExpectMessage(text -> text.equals(body), timeout);
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param bodyPattern test for message content
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(Pattern bodyPattern) {
    return thenExpectMessage(bodyPattern, Duration.ofSeconds(10));
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param bodyPattern test for message content
   * @param timeout How long to wait
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(Pattern bodyPattern, Duration timeout) {
    return thenExpectMessage(text -> bodyPattern.matcher(text).matches(), timeout);
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param bodyMatcher test for message content
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(Predicate<String> bodyMatcher) {
    return thenExpectMessage(bodyMatcher, Duration.ofSeconds(10));
  }

  /**
   * Expect a message within a timeframe.
   *
   * @param bodyMatcher test for message content
   * @param timeout How long to wait
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectMessage(
      Predicate<String> bodyMatcher, Duration timeout) {
    steps.add(() -> testWasMessage(bodyMatcher, timeout));
    return this;
  }

  protected void testWasMessage(Predicate<String> bodyMatcher, Duration timeout) {
    log.info("Waiting on MESSAGE");
    WebSocketStepType nextStep = mockWebSocketListener.nextStep(timeout);
    String nextMessage =
            Optional.ofNullable(mockWebSocketListener.getMessages().poll()).orElse("<null>");
    if (!nextStep.equals(WebSocketStepType.MESSAGE)) {
      throw new StepVerifierException("Expected MESSAGE got - " + nextStep);
    } else if (!bodyMatcher.test(nextMessage)) {
      throw new StepVerifierException("Unexpected message - " + nextMessage);
    }
  }

  /**
   * Wait a period of time doing nothing.
   *
   * @param period how long to wait
   * @return The verifier builder
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

  /** Expect the socket to be closing.
   * @return The verifier builder
   * 
   */
  public ClosedWebSocketStepsBuilder thenExpectClose() {
    return thenExpectClose(Duration.ofSeconds(10));
  }

  /**
   * Expect the socket to be closed.
   *
   * @param timeout How long to wait
   * @return The verifier builder
   */
  public ClosedWebSocketStepsBuilder thenExpectClose(Duration timeout) {
    steps.add(
        () -> {
          log.info("Waiting on CLOSING");
          WebSocketStepType nextStep = mockWebSocketListener.nextStep(timeout);
          if (!nextStep.equals(WebSocketStepType.CLOSING)) {
            throw new StepVerifierException("Expected CLOSING got - " + nextStep);
          }
        });

    // Respond to the request by cancelling
    thenClose();

    steps.add(
        () -> {
          log.info("Waiting on CLOSE");
          WebSocketStepType nextStep = mockWebSocketListener.nextStep(timeout);
          if (!nextStep.equals(WebSocketStepType.CLOSE)) {
            throw new StepVerifierException("Expected CLOSE got - " + nextStep);
          }
        });

    return new ClosedWebSocketStepsBuilder(
        serverSupplier, mockWebServerListener, mockWebSocketListener, scheduler, steps);
  }

  /**
   * Expect the socket to be in a failure state..
   *
   * @param timeout How long to wait
   * @return The verifier builder
   */
  public ClosedWebSocketStepsBuilder thenExpectFailure(Duration timeout) {
    steps.add(
        () -> {
          log.info("Waiting on FAILURE");
          WebSocketStepType nextStep = mockWebSocketListener.nextStep(timeout);
          if (!nextStep.equals(WebSocketStepType.FAILURE)) {
            throw new StepVerifierException("Expected FAILURE got - " + nextStep);
          }
        });

    return new ClosedWebSocketStepsBuilder(
        serverSupplier, mockWebServerListener, mockWebSocketListener, scheduler, steps);
  }

  /** Close the connection. 
   * @return The verifier builder
   * 
  */
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

  /** Perform an action. 
   * @param step the action to complete
   * @return The verifier builder
  */
  public OpenWebSocketStepsBuilder then(Runnable step) {
    steps.add(step);
    return this;
  }
}
