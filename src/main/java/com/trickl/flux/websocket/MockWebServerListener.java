package com.trickl.flux.websocket;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.Getter;
import lombok.extern.java.Log;
import okhttp3.mockwebserver.MockWebServer;

@Log
@Getter
public final class MockWebServerListener implements ServerListener {

  protected Queue<WebServerStepType> steps = new ConcurrentLinkedDeque<>();

  protected MockWebServer server = null;

  private Object syncEvent = new Object();

  @Override
  public void onStart(MockWebServer server) {
    synchronized (syncEvent) {
      log.info("SERVER START");
      steps.add(WebServerStepType.SERVER_START);
      this.server = server;
      syncEvent.notifyAll();
    }
  }

  @Override
  public void onShutdown() {
    synchronized (syncEvent) {
      log.info("SERVER SHUTDOWN");     
      steps.add(WebServerStepType.SERVER_SHUTDOWN);
      this.server = null;
      syncEvent.notifyAll();
    }
  }

  WebServerStepType nextStep(Duration timeout) {
    synchronized (syncEvent) {      
      Instant start = Instant.now();
      Instant timeoutTime = start.plus(timeout);
      while (steps.isEmpty() && Instant.now().isBefore(timeoutTime)) {
        try {
          syncEvent.wait(Math.max(Duration.between(Instant.now(), timeoutTime).toMillis(), 0));
        } catch (InterruptedException ex) {
          log.info("Wait Interrupted");
          Thread.currentThread().interrupt();
        }    
      }

      return Optional.ofNullable(steps.poll())
          .orElse(WebServerStepType.NOTHING);   
    }  
  }
}
