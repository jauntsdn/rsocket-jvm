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

/**
 * Channel interactions for async exchange of binary messages using jdk's {@link
 * java.util.concurrent.CompletableFuture} API.
 */
public interface MessageStreams extends Closeable {

  /**
   * Unreliable and unordered 1 to 0 messages exchange interaction.
   *
   * @return {@link CompletionStage} completed successfully as soon as message is scheduled for
   *     write if channel is connected, or completed with error if channel is disconnected. {@link
   *     CompletionStage} completion does not imply that message was written on wire; messages
   *     ordering between several fire-and-forget interactions, or fire-and-forget and other
   *     interactions is not guaranteed.
   */
  CompletionStage<Void> fireAndForget(Message message);

  /**
   * 1 to strictly 1 messages exchange interaction.
   *
   * @return {@link CompletionStage} completed successfully with exactly 1 response message,
   *     otherwise completed with error.
   */
  CompletionStage<Message> requestResponse(Message message);

  /** @return factory to create messages from byte buffer data & metadata */
  default Optional<Message.Factory> messageFactory() {
    return Optional.empty();
  }

  /** @return ExecutorService to schedule actions on channel's event loop */
  default Optional<ScheduledExecutorService> scheduler() {
    return Optional.empty();
  }

  /** @return channel's ByteBufAllocator */
  default Optional<ByteBufAllocator> allocator() {
    return Optional.empty();
  }
}
