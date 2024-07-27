package com.javasteam.steam.handlers;

import com.google.protobuf.GeneratedMessage;
import com.javasteam.handlers.FutureItem;
import com.javasteam.handlers.ListenerItem;
import java.util.function.Consumer;

/**
 * Helper interface to delegate {@link JobHandler} methods. Implement either of the methods {@link
 * #getJobHandler()} or {@link #getJobHandlerInstance()} to delegate the methods.
 */
public interface HasJobHandler {

  /** Implement this method if you have access to the {@link JobHandler} object */
  default JobHandler getJobHandler() {
    return getJobHandlerInstance().getJobHandler();
  }

  /** Implement this if you have access to an object that implements {@link HasJobHandler} */
  default HasJobHandler getJobHandlerInstance() {
    return null;
  }

  default <T> ListenerItem<Long, byte[], T> addJobListener(ListenerItem<Long, byte[], T> item) {
    return getJobHandler().addMessageListener(item);
  }

  default <T> T waitForJob(FutureItem<Long, byte[], T> item) {
    return getJobHandler().addMessageFuture(item);
  }

  default <T> void notifyJobListeners(Long id, T item) {
    getJobHandler().notifyListeners(id, item);
  }

  /** Commonly used method to add a job listener. */
  default <T extends GeneratedMessage> void addJobListener(
      Long id, Class<T> tClass, Consumer<T> consumer) {
    getJobHandler().addMessageListener(ListenerItem.builder(id, tClass, consumer).build());
  }

  /** Commonly used method to wait for a job. */
  default <T extends GeneratedMessage> T waitForJob(Long id, Class<T> tClass) {
    return getJobHandler().addMessageFuture(FutureItem.builder(id, tClass).build());
  }
}
