package com.trickl.flux.websocket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.java.Log;
import okhttp3.WebSocket;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Log
public final class WebSocketEventsBuilder {

  private final Scheduler scheduler = Schedulers.parallel();

  private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();

  private Queue<Runnable> events = new ConcurrentLinkedQueue<>();

  /**
   * Schedule sending a websocket message.
   *
   * @param payload The message payloadfto send
   */
  public WebSocketEventsBuilder thenSend(final String payload) {
    events.add(() -> {
      WebSocket ws = webSocketRef.get();
      if (ws != null) {
        ws.send(payload);
      }
    });
    return this;
  }

  /**
   * Wait a period of time.
   * 
   * @param delay    The length of the delay.
   * @param timeUnit The time unit of delay.
   */
  public WebSocketEventsBuilder thenWait(int delay, TimeUnit timeUnit) {
    events.add(() -> {
      final CountDownLatch latch = new CountDownLatch(1);
      try {
        latch.await(delay, timeUnit);
      } catch (InterruptedException ex) {
        log.info("Wait interrupted.");
        Thread.currentThread().interrupt();
      }
    });
    return this;
  }

  /** Close the connection. */
  public void thenClose() {
    events.add(() -> {
      WebSocket ws = webSocketRef.get();
      if (ws != null) {        
        log.info("Terminating connection.");
        ws.close(CloseStatus.NORMAL.getCode(), "Normal termination.");        
      }
    });    
  }

  /** Run the event chain. */
  public void runEvents(WebSocket webSocket) {
    webSocketRef.set(webSocket);
    scheduler.schedule(
        () -> {
          while (!events.isEmpty()) {
            events.remove().run();
          }
        });
  }
}
