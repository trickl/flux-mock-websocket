package com.trickl.flux.websocket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
public final class MockWebSocket {

  private final MockWebServer server;

  /** Expect an open event. */
  public ClosedWebSocketStepsBuilder verifier() {

    MockWebSocketListener listener = new MockWebSocketListener();
    MockResponse response = new MockResponse().withWebSocketUpgrade(listener);
    this.server.enqueue(response);

    Scheduler scheduler = Schedulers.parallel();
    Queue<Runnable> steps = new ConcurrentLinkedQueue<>();

    return new ClosedWebSocketStepsBuilder(listener, scheduler, steps);
  }
}
