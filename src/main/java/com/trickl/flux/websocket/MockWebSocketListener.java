package com.trickl.flux.websocket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import lombok.Getter;
import lombok.extern.java.Log;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@Log
@Getter
public final class MockWebSocketListener extends WebSocketListener {

  protected Queue<String> messages = new ConcurrentLinkedDeque<>();

  protected Throwable failure = null;

  protected Queue<WebSocketStepType> steps = new ConcurrentLinkedDeque<>();

  protected WebSocket webSocket = null;

  private Object syncEvent = new Object();

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    synchronized (syncEvent) {
      log.info("OPEN");
      steps.add(WebSocketStepType.OPEN);
      this.webSocket = webSocket;
      syncEvent.notifyAll();
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    synchronized (syncEvent) {
      log.info("Server received: " + text);
      messages.add(text);
      steps.add(WebSocketStepType.MESSAGE);
      syncEvent.notifyAll();
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    onMessage(webSocket, bytes.utf8());
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    synchronized (syncEvent) {
      log.info("CLOSE: " + code + " " + reason);
      steps.add(WebSocketStepType.CLOSE);
      this.webSocket = null;
      syncEvent.notifyAll();
    }
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
    synchronized (syncEvent) {
      log.log(Level.WARNING, "WebSocket Failure", throwable);
      failure = throwable;
      steps.add(WebSocketStepType.FAILURE);
      syncEvent.notifyAll();
    }
  }
}
