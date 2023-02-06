/*
 * Copyright 2023 - present Maksym Ostroverkhov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jauntsdn.rsocket;

import io.grpc.stub.StreamObserver;
import io.netty.buffer.ByteBufAllocator;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

public class RSocketProxy implements RSocket, RSocketHandler {
  protected final MessageStreams source;

  public RSocketProxy(MessageStreams source) {
    this.source = source;
  }

  @Override
  public void metadataPush(Message message, StreamObserver<Message> responseObserver) {
    MessageStreams s = source;
    if (s instanceof RSocket) {
      ((RSocket) s).metadataPush(message, responseObserver);
      return;
    }
    responseObserver.onError(
        new UnsupportedOperationException(
            "metadata-push is not supported by source: " + s.getClass().getName()));
  }

  @Override
  public void fireAndForget(Message message, StreamObserver<Message> responseObserver) {
    source.fireAndForget(message, responseObserver);
  }

  @Override
  public void requestResponse(Message message, StreamObserver<Message> responseObserver) {
    source.requestResponse(message, responseObserver);
  }

  @Override
  public void requestStream(Message message, StreamObserver<Message> responseObserver) {
    source.requestStream(message, responseObserver);
  }

  @Override
  public StreamObserver<Message> requestChannel(StreamObserver<Message> responseObserver) {
    return source.requestChannel(responseObserver);
  }

  @Override
  public StreamObserver<Message> requestChannel(
      Message message, StreamObserver<Message> responseObserver) {
    MessageStreams s = source;
    if (s instanceof MessageStreamsHandler) {
      return ((MessageStreamsHandler) s).requestChannel(message, responseObserver);
    }
    return s.requestChannel(responseObserver);
  }

  @Override
  public Optional<ScheduledExecutorService> scheduler() {
    return source.scheduler();
  }

  @Override
  public Optional<ScheduledExecutorService> coarseScheduler() {
    MessageStreams s = source;
    if (s instanceof RSocket) {
      return ((RSocket) s).coarseScheduler();
    }
    return Optional.empty();
  }

  @Override
  public Optional<ByteBufAllocator> allocator() {
    return source.allocator();
  }

  @Override
  public Optional<Message.Factory> messageFactory() {
    return source.messageFactory();
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
  public CompletionStage<Void> onClose() {
    return source.onClose();
  }

  @Override
  public Attributes attributes() {
    return source.attributes();
  }

  @Override
  public double availability(int rank) {
    MessageStreams s = source;
    if (s instanceof RSocket) {
      return ((RSocket) s).availability(rank);
    }
    return s.isDisposed() ? 0.0 : 1.0;
  }
}
