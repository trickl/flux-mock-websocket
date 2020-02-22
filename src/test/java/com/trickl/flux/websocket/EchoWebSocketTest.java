package com.trickl.flux.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trickl.flux.config.WebSocketConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

@RunWith(SpringRunner.class)
@ActiveProfiles({ "unittest" })
@SpringBootTest(classes = WebSocketConfiguration.class)
public class EchoWebSocketTest extends BaseWebSocketClientTest {

  @Autowired
  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  private void setup() {
    startServer(objectMapper);
  }

  @AfterEach
  private void shutdown() throws IOException, InterruptedException {
    server.shutdown();
  }

  @Test
  public void testEchoWebSocket() throws IOException, InterruptedException {

    new MockWebSocket(server)
        .verifier()
        .thenExpectOpen(Duration.ofSeconds(3))
        //.thenExpectMessage(Pattern.compile("CONNECT.*"), Duration.ofSeconds(3))
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
        .thenCloseAndVerify();        

    WebSocketClient client = new ReactorNettyWebSocketClient();
    EchoWebSocketHandler handler = new EchoWebSocketHandler();
    client
        .execute(server.url("/websocket").uri(), new HttpHeaders(), handler)
        .log("SESSION")
        .block(Duration.ofSeconds(60));
  }
}
