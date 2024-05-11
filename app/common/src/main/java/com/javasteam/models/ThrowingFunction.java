package com.javasteam.models;

/**
 * A functional interface that can throw an exception. This is useful for lambda expressions that
 * need to throw an exception.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of the exception that can be thrown
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
  R apply(T t) throws E;
}
