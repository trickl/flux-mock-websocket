package com.trickl.flux.websocket;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

@Log
@RequiredArgsConstructor
public class VerifierComplete {

  private final CountDownLatch completeSignal;

  /**
   * Block until complete.
   * @param duration how long to wait
   */
  public void waitComplete(Duration duration) {
    try {
      completeSignal.await(duration.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      log.warning("Verifier interrupted");
    }
  }

  /**
   * Block until complete.
   */
  public void waitComplete() {
    try {
      completeSignal.await();
    } catch (InterruptedException ex) {
      log.warning("Verifier interrupted");
    }
  }
}
