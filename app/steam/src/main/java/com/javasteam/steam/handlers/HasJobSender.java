package com.javasteam.steam.handlers;

import com.javasteam.models.AbstractMessage;
import com.javasteam.models.HasJob;
import com.javasteam.models.Header;
import com.javasteam.models.Job;

/** Marks a class as having a method to send jobs. */
public interface HasJobSender {
  <H extends Header & HasJob> Job sendJob(AbstractMessage<H, ?> message, Job job);
}
