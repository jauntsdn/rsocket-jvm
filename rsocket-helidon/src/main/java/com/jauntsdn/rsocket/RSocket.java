/*
 * Copyright 2021 - present Maksym Ostroverkhov.
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

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.netty.buffer.ByteBufAllocator;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public interface RSocket extends Availability, Closeable {

  Single<Void> fireAndForget(Message message);

  Single<Message> requestResponse(Message message);

  Multi<Message> requestStream(Message message);

  Multi<Message> requestChannel(Flow.Publisher<Message> messages);

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

  @FunctionalInterface
  interface Interceptor extends Function<RSocket, RSocket> {}
}
