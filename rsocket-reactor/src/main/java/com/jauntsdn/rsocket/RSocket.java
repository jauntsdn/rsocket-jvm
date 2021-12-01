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
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public interface RSocket extends Availability, Closeable {

  Mono<Void> fireAndForget(Message message);

  Mono<Message> requestResponse(Message message);

  Flux<Message> requestStream(Message message);

  Flux<Message> requestChannel(Publisher<Message> messages);

  Mono<Void> metadataPush(Message message);

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

  default Optional<Scheduler> scheduler() {
    return Optional.empty();
  }

  default Optional<ByteBufAllocator> allocator() {
    return Optional.empty();
  }

  @FunctionalInterface
  interface Interceptor extends Function<RSocket, RSocket> {}
}
