package com.javasteam.steam.handlers;

import com.javasteam.handlers.BaseMessageHandler;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/** A class for handling incoming and outgoing jobs. */
@Slf4j
public class JobHandler extends BaseMessageHandler<Long> {
  private final AtomicInteger jobIdCounter = new AtomicInteger(0);

  public JobHandler() {
    super();
  }

  public JobHandler(int threads) {
    super(threads);
  }

  public synchronized long getNextJobId() {
    return jobIdCounter.incrementAndGet();
  }
}
