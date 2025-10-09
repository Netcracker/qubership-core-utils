package com.netcracker.cloud.security.core.utils.k8s.impl;

import lombok.AllArgsConstructor;

// Simple utility class inspired by scala Try
public interface Try<T> {
    @FunctionalInterface
    interface OmnivoreSupplier<T> {
        T get() throws Exception;
    }

    static <T> Success<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Failure<T> failure(RuntimeException e) {
        return new Failure<>(e);
    }

    static <T> Try<T> of(OmnivoreSupplier<T> supplier) {
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return new Failure<>(new RuntimeException(e));
        }
    }

    T getOrThrow();

    @AllArgsConstructor
    class Success<T> implements Try<T> {
        T value;

        @Override
        public T getOrThrow() {
            return value;
        }
    }

    @AllArgsConstructor
    class Failure<T> implements Try<T> {
        RuntimeException exception;

        @Override
        public T getOrThrow() {
            throw exception;
        }
    }
}
