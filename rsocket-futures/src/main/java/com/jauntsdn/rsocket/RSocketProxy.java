/*
 * Copyright 2020 - present Maksym Ostroverkhov.
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

import io.netty.buffer.ByteBufAllocator;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

public class RSocketProxy implements RSocket {
  protected final MessageStreams source;

  public RSocketProxy(MessageStreams source) {
    this.source = source;
  }

  @Override
  public CompletionStage<Void> fireAndForget(Message message) {
    return source.fireAndForget(message);
  }

  @Override
  public CompletionStage<Message> requestResponse(Message message) {
    return source.requestResponse(message);
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
  public CompletionStage<Void> metadataPush(Message message) {
    MessageStreams s = source;
    if (s instanceof RSocket) {
      return ((RSocket) s).metadataPush(message);
    }
    message.release();
    return AbstractRSocket.completedFuture(
        new UnsupportedOperationException(
            "metadata-push is not supported by source: " + s.getClass().getName()));
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
