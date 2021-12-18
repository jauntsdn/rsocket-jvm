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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AbstractRSocket implements RSocketHandler {

  @Override
  public Mono<Void> fireAndForget(Message message) {
    message.release();
    return Mono.error(new UnsupportedOperationException("fire-and-forget not implemented"));
  }

  @Override
  public Mono<Message> requestResponse(Message message) {
    message.release();
    return Mono.error(new UnsupportedOperationException("request-response not implemented"));
  }

  @Override
  public Flux<Message> requestStream(Message message) {
    message.release();
    return Flux.error(new UnsupportedOperationException("request-stream not implemented"));
  }

  @Override
  public Flux<Message> requestChannel(Publisher<Message> messages) {
    return Flux.error(
        new UnsupportedOperationException("request-channel(messages) not implemented"));
  }

  @Override
  public Flux<Message> requestChannel(Message message, Publisher<Message> messages) {
    message.release();
    return Flux.error(
        new UnsupportedOperationException("request-channel(message, messages) not implemented"));
  }

  @Override
  public Mono<Void> metadataPush(Message message) {
    message.release();
    return Mono.error(new UnsupportedOperationException("metadata-push not implemented"));
  }

  @Override
  public void dispose() {}

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public Mono<Void> onClose() {
    return Mono.never();
  }
}
