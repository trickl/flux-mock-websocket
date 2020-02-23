package com.trickl.flux.websocket;

import okhttp3.mockwebserver.MockWebServer;

public interface ServerListener {

  void onStart(MockWebServer server);

  void onShutdown();
}
