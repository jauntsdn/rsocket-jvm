/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import io.helidon.common.reactive.Single;

public interface RSocketRpcService extends RSocketHandler {

  String service();

  Class<?> serviceType();

  @Override
  default Single<Void> metadataPush(Message message) {
    message.release();
    return Single.error(
        new UnsupportedOperationException("RSocketRpcService: metadata-push is not supported"));
  }

  interface Factory<T extends RSocketHandler> {

    T withLifecycle(Closeable requester);
  }
}
