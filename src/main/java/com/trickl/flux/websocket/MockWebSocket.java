package com.trickl.flux.websocket;

import java.util.logging.Level;
import lombok.extern.java.Log;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

@Log
public final class MockWebSocket extends WebSocketListener {

  private WebSocketEventsBuilder builder;

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    log.info("OPEN");
    if (builder != null) {
      builder.runEvents(webSocket);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    log.info("Server received: " + text);
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    log.info("Server received: " + bytes.hex());
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    log.info("CLOSE: " + code + " " + reason);
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    log.log(Level.WARNING, "WebSocket Failure", t);
  }

  public WebSocketEventsBuilder awaitOpen() {
    builder = new WebSocketEventsBuilder();
    return builder;
  }
}
