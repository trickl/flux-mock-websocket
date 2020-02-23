package com.trickl.flux.websocket;

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
}
