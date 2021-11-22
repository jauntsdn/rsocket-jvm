/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import io.helidon.common.reactive.Multi;
import java.util.concurrent.Flow;

/**
 * Extends the {@link RSocket} that allows an implementer to peek at the first request payload of a
 * channel.
 */
public interface RSocketHandler extends RSocket {
  /**
   * Implement this method to peak at the first payload of the incoming request stream without
   * having to subscribe to Publish&lt;Payload&gt; payloads
   *
   * @param message First payload in the stream - this is the same payload as the first payload in
   *     Publisher&lt;Payload&gt; payloads
   * @param payloads Stream of request payloads.
   * @return Stream of response payloads.
   */
  default Multi<Message> requestChannel(Message message, Flow.Publisher<Message> payloads) {
    return requestChannel(payloads);
  }
}
