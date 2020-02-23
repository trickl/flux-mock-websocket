package com.trickl.flux.websocket;

import com.trickl.flux.config.WebSocketConfiguration;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

@ActiveProfiles({ "unittest" })
@SpringBootTest(classes = WebSocketConfiguration.class)
public class EchoWebSocketTest {

  @Test
  public void testEchoWebSocket() throws IOException {

    MockServerWithWebSocket mockServer = new MockServerWithWebSocket();

    mockServer.beginVerifier()
        .thenWaitServerStartThenUpgrade()
        .thenExpectOpen()
        .thenSend("MESSAGE 1")
        .thenWait(Duration.ofMillis(500))
        .thenSend("MESSAGE 2") 
        .thenWait(Duration.ofMillis(500))
        .thenSend("MESSAGE 3")
        .thenWait(Duration.ofMillis(500))
        .thenSend("MESSAGE 4")
        .thenWait(Duration.ofMillis(500))
        .thenSend("MESSAGE 5")
        .thenWait(Duration.ofMillis(500))
        .thenClose()
        .thenWaitServerShutdown()
        .thenVerify();        

    WebSocketClient client = new ReactorNettyWebSocketClient();
    EchoWebSocketHandler handler = new EchoWebSocketHandler();
    mockServer.start();
    client
        .execute(mockServer.getWebSocketUri(), new HttpHeaders(), handler)
        .log("SESSION")
        .block(Duration.ofSeconds(60));
    mockServer.shutdown();
  }
}
