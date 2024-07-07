package com.javasteam.steam.handlers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

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

  default <T> JobHandler.JobListenerItem<T> addJobListener(JobHandler.JobListenerItem<T> item) {
    return (JobHandler.JobListenerItem<T>) getJobHandler().addMessageListener(item);
  }

  default <T> JobHandler.JobListenerItem<T> addJobListener(
      Long id, int priority, Function<byte[], T> mapper, Consumer<T> consumer) {
    return addJobListener(JobHandler.JobListenerItem.of(id, priority, mapper, consumer));
  }

  default <T> JobHandler.JobListenerItem<T> addJobListener(
      Long id, Function<byte[], T> mapper, Consumer<T> consumer) {
    return addJobListener(
        JobHandler.JobListenerItem.of(id, JobHandler.DEFAULT_PRIORITY, mapper, consumer));
  }

  default <T> JobHandler.JobFutureItem<T> waitForJob(JobHandler.JobFutureItem<T> item) {
    return (JobHandler.JobFutureItem<T>) getJobHandler().addMessageFuture(item);
  }

  default <T> T waitForJob(Long id, Function<byte[], T> mapper, long timeoutMs, int priority)
      throws TimeoutException {
    try {
      return waitForJob(JobHandler.JobFutureItem.of(id, priority, mapper))
          .getFuture()
          .get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
      throw new TimeoutException("Timeout waiting for job %d".formatted(id));
    }
  }

  default <T> T waitForJob(Long id, Function<byte[], T> mapper, long timeoutMs)
      throws TimeoutException {
    try {
      return waitForJob(JobHandler.JobFutureItem.of(id, JobHandler.DEFAULT_PRIORITY, mapper))
          .getFuture()
          .get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
      throw new TimeoutException("Timeout waiting for job %d".formatted(id));
    }
  }

  default <T> T waitForJob(Long id, int priority, Function<byte[], T> mapper) {
    return waitForJob(JobHandler.JobFutureItem.of(id, priority, mapper)).getFuture().join();
  }

  default <T> T waitForJob(Long id, Function<byte[], T> mapper) {
    return waitForJob(JobHandler.JobFutureItem.of(id, JobHandler.DEFAULT_PRIORITY, mapper))
        .getFuture()
        .join();
  }

  default <T> void notifyJobListeners(Long id, T item) {
    getJobHandler().notifyListeners(id, item);
  }
}
