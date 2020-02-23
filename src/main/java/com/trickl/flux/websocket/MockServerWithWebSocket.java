package com.trickl.flux.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NoArgsConstructor;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@NoArgsConstructor
public class MockServerWithWebSocket {

  private MockWebServer mockServer;
  MockWebServerListener serverListener = new MockWebServerListener();
  MockWebSocketListener webSocketListener = new MockWebSocketListener();

  /** Expect an open event. */
  public ClosedWebSocketStepsBuilder beginVerifier() {
    Scheduler scheduler = Schedulers.newParallel("verifier");
    Queue<Runnable> steps = new ConcurrentLinkedQueue<>();

    return new ClosedWebSocketStepsBuilder(
        () -> mockServer, serverListener, webSocketListener, scheduler, steps);
  }

  /** Start a mock web server. */
  public void start() {
    mockServer = new MockWebServer();
    serverListener.onStart(mockServer);
  }

  /** Shutdown the mock web server. */
  public void shutdown() throws IOException {
    mockServer.shutdown();
    serverListener.onShutdown();
  }

  public URI getWebSocketUri() {
    return mockServer.url("/websocket").uri();
  }
}
