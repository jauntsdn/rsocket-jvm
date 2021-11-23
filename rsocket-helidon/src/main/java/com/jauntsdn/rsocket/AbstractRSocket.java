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
import java.util.concurrent.Flow;

public abstract class AbstractRSocket implements RSocketHandler {

  @Override
  public Single<Void> fireAndForget(Message message) {
    message.release();
    return Single.error(new UnsupportedOperationException("fire-and-forget not implemented"));
  }

  @Override
  public Single<Message> requestResponse(Message message) {
    message.release();
    return Single.error(new UnsupportedOperationException("request-response not implemented"));
  }

  @Override
  public Multi<Message> requestStream(Message message) {
    message.release();
    return Multi.error(new UnsupportedOperationException("request-stream not implemented"));
  }

  @Override
  public Multi<Message> requestChannel(Flow.Publisher<Message> messages) {
    return Multi.error(
        new UnsupportedOperationException("request-channel(messages) not implemented"));
  }

  @Override
  public Multi<Message> requestChannel(Message message, Flow.Publisher<Message> messages) {
    message.release();
    return Multi.error(
        new UnsupportedOperationException("request-channel(message, messages) not implemented"));
  }

  @Override
  public Single<Void> metadataPush(Message message) {
    message.release();
    return Single.error(new UnsupportedOperationException("metadata-push not implemented"));
  }

  @Override
  public void dispose() {}

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public Single<Void> onClose() {
    return Single.never();
  }
}
