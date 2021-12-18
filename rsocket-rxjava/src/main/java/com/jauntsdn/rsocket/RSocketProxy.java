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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import java.util.Optional;
import org.reactivestreams.Publisher;

public class RSocketProxy implements RSocket, RSocketHandler {
  protected final MessageStreams source;

  public RSocketProxy(MessageStreams source) {
    this.source = source;
  }

  @Override
  public Completable fireAndForget(Message message) {
    return source.fireAndForget(message);
  }

  @Override
  public Single<Message> requestResponse(Message message) {
    return source.requestResponse(message);
  }

  @Override
  public Flowable<Message> requestStream(Message message) {
    return source.requestStream(message);
  }

  @Override
  public Flowable<Message> requestChannel(Publisher<Message> messages) {
    return source.requestChannel(messages);
  }

  @Override
  public Flowable<Message> requestChannel(Message message, Publisher<Message> messages) {
    MessageStreams s = source;
    if (s instanceof MessageStreamsHandler) {
      return ((MessageStreamsHandler) s).requestChannel(message, messages);
    }
    return s.requestChannel(messages);
  }

  @Override
  public Optional<Message.Factory> messageFactory() {
    return source.messageFactory();
  }

  @Override
  public Optional<Scheduler> scheduler() {
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
  public Completable onClose() {
    return source.onClose();
  }

  @Override
  public Attributes attributes() {
    return source.attributes();
  }

  @Override
  public Completable metadataPush(Message message) {
    MessageStreams s = source;
    if (s instanceof RSocket) {
      return ((RSocket) s).metadataPush(message);
    }
    message.release();
    return Completable.error(
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
