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

import io.netty.buffer.ByteBufAllocator;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;

public interface MessageStreams extends Closeable {

  Uni<Void> fireAndForget(Message message);

  Uni<Message> requestResponse(Message message);

  Multi<Message> requestStream(Message message);

  Multi<Message> requestChannel(Flow.Publisher<Message> messages);

  default Optional<Message.Factory> messageFactory() {
    return Optional.empty();
  }

  default Optional<ScheduledExecutorService> scheduler() {
    return Optional.empty();
  }

  default Optional<ByteBufAllocator> allocator() {
    return Optional.empty();
  }
}
