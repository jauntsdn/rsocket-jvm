/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import io.helidon.common.reactive.Single;

/** */
public interface Closeable extends GracefulCloseable {

  /**
   * Returns a {@code Publisher} that completes when this {@code RSocket} is closed. A {@code
   * RSocket} can be closed by explicitly calling {@link RSocket#dispose()} or when the underlying
   * transport connection is closed.
   *
   * @return A {@code Publisher} that completes when this {@code RSocket} close is complete.
   */
  Single<Void> onClose();

  void dispose();

  boolean isDisposed();

  @Override
  default void dispose(String reason, boolean isGraceful) {
    dispose();
  }
}
