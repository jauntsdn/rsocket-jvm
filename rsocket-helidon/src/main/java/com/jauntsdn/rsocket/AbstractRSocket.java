/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import java.util.concurrent.Flow;

/**
 * An abstract implementation of {@link RSocket}. All request handling methods emit {@link
 * UnsupportedOperationException} and hence must be overridden to provide a valid implementation.
 */
public abstract class AbstractRSocket implements RSocketHandler {

  @Override
  public Single<Void> fireAndForget(Message message) {
    message.release();
    return Single.error(new UnsupportedOperationException("Fire and forget not implemented."));
  }

  @Override
  public Single<Message> requestResponse(Message message) {
    message.release();
    return Single.error(new UnsupportedOperationException("Request-Response not implemented."));
  }

  @Override
  public Multi<Message> requestStream(Message message) {
    message.release();
    return Multi.error(new UnsupportedOperationException("Request-Stream not implemented."));
  }

  @Override
  public Multi<Message> requestChannel(Flow.Publisher<Message> payloads) {
    return Multi.error(new UnsupportedOperationException("Request-Channel not implemented."));
  }

  @Override
  public Single<Void> metadataPush(Message message) {
    message.release();
    return Single.error(new UnsupportedOperationException("Metadata-Push not implemented."));
  }

  @Override
  public void dispose() {}

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public Single<Void> onClose() {
    return Single.never();
  }
}
