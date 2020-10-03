package com.trickl.flux.websocket;

import com.trickl.exceptions.StepVerifierException;
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
    boolean didComplete = false;
    try {
      didComplete = completeSignal.await(duration.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      log.warning("Verifier interrupted");
    }
    if (!didComplete) {
      throw new StepVerifierException("Verifier did not complete within " + duration.toString());
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
