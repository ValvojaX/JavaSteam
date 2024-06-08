package com.javasteam.models;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/** Helper interface to provide methods for classes with a {@link ReentrantReadWriteLock}. */
public interface HasReadWriteLock {
  ReentrantReadWriteLock getLock();

  default void withReadLock(Runnable runnable) {
    getLock().readLock().lock();
    try {
      runnable.run();
    } finally {
      getLock().readLock().unlock();
    }
  }

  default <T> T withReadLock(Supplier<T> supplier) {
    getLock().readLock().lock();
    try {
      return supplier.get();
    } finally {
      getLock().readLock().unlock();
    }
  }

  default void withWriteLock(Runnable runnable) {
    getLock().writeLock().lock();
    try {
      runnable.run();
    } finally {
      getLock().writeLock().unlock();
    }
  }

  default <T> T withWriteLock(Supplier<T> supplier) {
    getLock().writeLock().lock();
    try {
      return supplier.get();
    } finally {
      getLock().writeLock().unlock();
    }
  }
}
