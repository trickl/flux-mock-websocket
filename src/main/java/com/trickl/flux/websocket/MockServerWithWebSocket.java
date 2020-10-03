package com.trickl.flux.websocket;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Log
@NoArgsConstructor
public class MockServerWithWebSocket {

  private MockWebServer mockServer;
  MockWebServerListener serverListener = new MockWebServerListener();
  MockWebSocketListener webSocketListener = new MockWebSocketListener();

  private final Duration waitStartTimeout = Duration.ofSeconds(10);
  Semaphore canStartSemaphore = new Semaphore(1);

  /** Expect an open event. 
   * @return The verifier builder 
  */
  public ClosedWebSocketStepsBuilder beginVerifier() {
    Scheduler scheduler = Schedulers.newParallel("verifier");
    Queue<Runnable> steps = new ConcurrentLinkedQueue<>();

    return new ClosedWebSocketStepsBuilder(
        () -> mockServer, serverListener, webSocketListener, scheduler, steps);
  }

  /** Start a mock web server. */
  public void start() {
    try {
      if (!canStartSemaphore.tryAcquire(waitStartTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
        String warning = MessageFormat.format(
            "Timeout of {0} elapsed waiting for shutdown, attempting to start anyway.", 
            waitStartTimeout);
        log.log(Level.WARNING, warning);
      }
    } catch (InterruptedException ex) {
      log.log(Level.WARNING, "Interruped wait on shutdown");
      Thread.currentThread().interrupt();
    }

    mockServer = new MockWebServer();
    try {
      mockServer.start();
      serverListener.onStart(mockServer);
    } catch (IOException ex) {
      log.log(Level.WARNING, "Unable to start server", ex);
    }
  }

  /** Shutdown the mock web server. 
   * @throws IOException if the server cannot be shutdown
  */
  public void shutdown() throws IOException {
    mockServer.shutdown();
    serverListener.onShutdown();
    canStartSemaphore.release();
  }

  public URI getWebSocketUri() {
    return mockServer.url("/websocket").uri();
  }
}
