package com.trickl.flux.websocket;

import lombok.extern.java.Log;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log
public class EchoWebSocketHandler implements WebSocketHandler {

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    Flux<WebSocketMessage> output = session.receive().doOnNext(message ->
        log.info("Client received: " + message)
    ).map(value -> session.textMessage("(Echo) " + value.getPayloadAsText()));
    
    return session.send(output).then(session.close());
  }
}
