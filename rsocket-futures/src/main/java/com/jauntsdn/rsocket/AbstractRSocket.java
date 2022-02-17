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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractRSocket implements RSocket {

  @Override
  public CompletionStage<Void> fireAndForget(Message message) {
    message.release();
    return completedFuture(new UnsupportedOperationException("fire-and-forget not implemented"));
  }

  @Override
  public CompletionStage<Message> requestResponse(Message message) {
    message.release();
    return completedFuture(new UnsupportedOperationException("request-response not implemented"));
  }

  @Override
  public CompletionStage<Void> metadataPush(Message message) {
    message.release();
    return completedFuture(new UnsupportedOperationException("metadata-push not implemented"));
  }

  @Override
  public void dispose() {}

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public CompletionStage<Void> onClose() {
    return new CompletableFuture<>();
  }

  static <T> CompletionStage<T> completedFuture(Throwable t) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(t);
    return future;
  }
}
