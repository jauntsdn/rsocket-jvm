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

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Models RSocket interactions as described in
 * https://github.com/rsocket/rsocket/blob/master/Protocol.md#stream-sequences-and-lifetimes
 */
public interface RSocket extends MessageStreams, Availability {

  /**
   * Enables metadata exchange between connection endpoints.
   *
   * @param message containing connection metadata.
   */
  Completable metadataPush(Message message);

  /**
   * @return lightweight {@link ScheduledExecutorService} intended for non-fine-grained tasks
   *     scheduling (e.g. timeouts).
   */
  default Optional<Scheduler> coarseScheduler() {
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

  @FunctionalInterface
  interface Interceptor extends Function<RSocket, RSocket> {}

  static RSocket from(MessageStreams messageStreams) {
    if (messageStreams instanceof RSocket) {
      return (RSocket) messageStreams;
    }
    return new RSocketProxy(messageStreams);
  }
}
