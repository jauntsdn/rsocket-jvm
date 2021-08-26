/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.netty.buffer.ByteBufAllocator;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * A contract providing different interaction models for <a
 * href="https://github.com/RSocket/reactivesocket/blob/master/Protocol.md">RSocket protocol</a>.
 */
public interface RSocket extends Availability, Closeable {

  /**
   * Fire and Forget interaction model of {@code RSocket}.
   *
   * @param message Request payload.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   *     handled, otherwise errors.
   */
  Single<Void> fireAndForget(Message message);

  /**
   * Request-Response interaction model of {@code RSocket}.
   *
   * @param message Request payload.
   * @return {@code Publisher} containing at most a single {@code Payload} representing the
   *     response.
   */
  Single<Message> requestResponse(Message message);

  /**
   * Request-Stream interaction model of {@code RSocket}.
   *
   * @param message Request payload.
   * @return {@code Publisher} containing the stream of {@code Payload}s representing the response.
   */
  Multi<Message> requestStream(Message message);

  /**
   * Request-Channel interaction model of {@code RSocket}.
   *
   * @param payloads Stream of request payloads.
   * @return Stream of response payloads.
   */
  Multi<Message> requestChannel(Flow.Publisher<Message> payloads);

  /**
   * Metadata-Push interaction model of {@code RSocket}.
   *
   * @param message Request payloads.
   * @return {@code Publisher} that completes when the passed {@code payload} is successfully
   *     handled, otherwise errors.
   */
  Single<Void> metadataPush(Message message);

  default Optional<Message.Factory> messageFactory() {
    return Optional.empty();
  }

  @Override
  default double availability(int rank) {
    return isDisposed() ? 0.0 : 1.0;
  }

  @Override
  default double availability() {
    return availability(0);
  }

  default Optional<ScheduledExecutorService> scheduler() {
    return Optional.empty();
  }

  default Optional<ByteBufAllocator> allocator() {
    return Optional.empty();
  }

  default Attributes attributes() {
    return Attributes.EMPTY;
  }

  /** Contract to decorate RSocket requester and responder */
  @FunctionalInterface
  interface Interceptor extends Function<RSocket, RSocket> {}
}
