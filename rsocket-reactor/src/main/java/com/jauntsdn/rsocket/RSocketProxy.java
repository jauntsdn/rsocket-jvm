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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class RSocketProxy implements RSocket {
  protected final RSocket source;

  public RSocketProxy(RSocket source) {
    this.source = source;
  }

  @Override
  public Mono<Void> fireAndForget(Message message) {
    return source.fireAndForget(message);
  }

  @Override
  public Mono<Message> requestResponse(Message message) {
    return source.requestResponse(message);
  }

  @Override
  public Flux<Message> requestStream(Message message) {
    return source.requestStream(message);
  }

  @Override
  public Flux<Message> requestChannel(Publisher<Message> messages) {
    return source.requestChannel(messages);
  }

  @Override
  public Mono<Void> metadataPush(Message message) {
    return source.metadataPush(message);
  }

  @Override
  public double availability(int rank) {
    return source.availability(rank);
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
  public Mono<Void> onClose() {
    return source.onClose();
  }

  @Override
  public Attributes attributes() {
    return source.attributes();
  }
}
