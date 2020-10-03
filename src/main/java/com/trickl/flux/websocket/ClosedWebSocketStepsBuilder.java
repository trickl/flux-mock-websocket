package com.trickl.flux.websocket;

import com.trickl.exceptions.StepVerifierException;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
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

  /** 
   * Wait for the server to be started and available.
   * @return The verifier builder
   * */
  public ClosedWebSocketStepsBuilder thenWaitServerShutdown() {
    return thenWaitServerShutdown(Duration.ofSeconds(10));
  }

  /** Wait for the server to be started and available.
   * @param timeout How long to wait
   * @return The verifier builder    
   */
  public ClosedWebSocketStepsBuilder thenWaitServerShutdown(Duration timeout) {
    steps.add(() -> testWasShutdown(timeout));

    return this;
  }

  protected void testWasShutdown(Duration timeout) {
    log.info("Waiting on SERVER_SHUTDOWN");
    WebServerStepType nextStep = mockWebServerListener.nextStep(timeout);
    if (!nextStep.equals(WebServerStepType.SERVER_SHUTDOWN)) {
      throw new StepVerifierException("Expected SERVER_SHUTDOWN got - " + nextStep);
    }    
  }


  /**
   *  Wait for the server to be started and available.
   * @return The verifier builder
   *  */
  public ClosedWebSocketStepsBuilder thenWaitServerStartThenUpgrade() {
    return thenWaitServerStartThenUpgrade(Duration.ofSeconds(10));
  }

  /** Wait for the server to be started and available.
   * @param timeout How long to wait
   * @return The verifier builder    
   */
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
    log.info("Waiting on SERVER_START");
    WebServerStepType nextStep = mockWebServerListener.nextStep(timeout);
    if (!nextStep.equals(WebServerStepType.SERVER_START)) {
      throw new StepVerifierException("Expected SERVER_START got - " + nextStep);
    }    
  }

  /** Expect the socket to be opened. 
   * @return The verifier builder
   * 
  */
  public OpenWebSocketStepsBuilder thenExpectOpen() {
    return thenExpectOpen(Duration.ofSeconds(10));
  }

  /**
   * Expect the socket to be opened.
   *
   * @param timeout How long to wait
   * @return The verifier builder
   */
  public OpenWebSocketStepsBuilder thenExpectOpen(Duration timeout) {
    steps.add(() -> testWasOpen(timeout));

    return new OpenWebSocketStepsBuilder(
        serverSupplier, mockWebServerListener, mockWebSocketListener, scheduler, steps);
  }

  protected void testWasOpen(Duration timeout) {
    log.info("Waiting on OPEN");
    WebSocketStepType nextStep = mockWebSocketListener.nextStep(timeout);
    if (!nextStep.equals(WebSocketStepType.OPEN)) {
      throw new StepVerifierException("Expected OPEN got - " + nextStep);
    }        
  }

  /** Perform an action.
   * @param step the action to complete
   * @return The verifier builder   
   */
  public ClosedWebSocketStepsBuilder then(Runnable step) {
    steps.add(step);
    return this;
  }

  /** Verify the steps ran. 
   * @return The verification completion object
   * 
  */
  public VerifierComplete thenVerify() {
    CountDownLatch completeSignal = new CountDownLatch(1);
    scheduler.schedule(
        () -> {
          while (!steps.isEmpty()) {
            steps.remove().run();
          }
          completeSignal.countDown();
        });
    return new VerifierComplete(completeSignal);
  }  
}
