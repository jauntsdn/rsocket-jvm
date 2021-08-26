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

/** Wrapper/Proxy for a RSocket. This is useful when we want to override a specific method. */
public class RSocketProxy implements RSocket {
  protected final RSocket source;

  public RSocketProxy(RSocket source) {
    this.source = source;
  }

  @Override
  public Single<Void> fireAndForget(Message message) {
    return source.fireAndForget(message);
  }

  @Override
  public Single<Message> requestResponse(Message message) {
    return source.requestResponse(message);
  }

  @Override
  public Multi<Message> requestStream(Message message) {
    return source.requestStream(message);
  }

  @Override
  public Multi<Message> requestChannel(Flow.Publisher<Message> payloads) {
    return source.requestChannel(payloads);
  }

  @Override
  public Single<Void> metadataPush(Message message) {
    return source.metadataPush(message);
  }

  @Override
  public Optional<Message.Factory> messageFactory() {
    return source.messageFactory();
  }

  @Override
  public double availability(int rank) {
    return source.availability(rank);
  }

  @Override
  public Optional<ScheduledExecutorService> scheduler() {
    return source.scheduler();
  }

  @Override
  public Optional<ByteBufAllocator> allocator() {
    return source.allocator();
  }

  @Override
  public void dispose() {
    source.dispose();
  }

  @Override
  public void dispose(String reason, boolean isGraceful) {
    source.dispose(reason, isGraceful);
  }

  @Override
  public boolean isDisposed() {
    return source.isDisposed();
  }

  @Override
  public Single<Void> onClose() {
    return source.onClose();
  }

  @Override
  public Attributes attributes() {
    return source.attributes();
  }
}
