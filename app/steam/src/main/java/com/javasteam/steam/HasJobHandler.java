package com.javasteam.steam;

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

  default <T> JobHandler.JobItem<T> addJobListener(JobHandler.JobItem<T> jobItem) {
    return getJobHandler().addJobListener(jobItem);
  }

  default <T> JobHandler.JobItem<T> addJobListener(
      long sourceJobId,
      java.util.function.Function<byte[], T> responseSupplier,
      java.util.function.Consumer<T> listener) {
    return getJobHandler().addJobListener(sourceJobId, responseSupplier, listener);
  }

  default <T> void waitForJob(
      long sourceJobId,
      java.util.function.Function<byte[], T> responseSupplier,
      java.util.function.Consumer<T> listener) {
    getJobHandler().waitForJob(sourceJobId, responseSupplier, listener);
  }

  default <T> void waitForJob(
      long sourceJobId,
      java.util.function.Function<byte[], T> responseSupplier,
      java.util.function.Consumer<T> listener,
      long timeoutMs)
      throws java.util.concurrent.TimeoutException {
    getJobHandler().waitForJob(sourceJobId, responseSupplier, listener, timeoutMs);
  }

  default void waitForJob(long sourceJobId) {
    getJobHandler().waitForJob(sourceJobId);
  }

  default void waitForJob(long sourceJobId, long timeoutMs)
      throws java.util.concurrent.TimeoutException {
    getJobHandler().waitForJob(sourceJobId, timeoutMs);
  }

  default void notifyJobListeners(long sourceJobId, byte[] bodyBytes) {
    getJobHandler().notifyJobListeners(sourceJobId, bodyBytes);
  }
}
