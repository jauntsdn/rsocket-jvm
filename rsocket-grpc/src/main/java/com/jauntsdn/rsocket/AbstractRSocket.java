/*
 * Copyright 2023 - present Maksym Ostroverkhov.
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

import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractRSocket implements RSocket {

  @Override
  public void metadataPush(Message message, StreamObserver<Message> responseObserver) {
    message.release();
    responseObserver.onError(new UnsupportedOperationException("metadata-push not implemented"));
  }

  @Override
  public void fireAndForget(Message message, StreamObserver<Message> responseObserver) {
    message.release();
    responseObserver.onError(new UnsupportedOperationException("fire-and-forget not implemented"));
  }

  @Override
  public void requestResponse(Message message, StreamObserver<Message> responseObserver) {
    message.release();
    responseObserver.onError(new UnsupportedOperationException("request-response not implemented"));
  }

  @Override
  public void requestStream(Message message, StreamObserver<Message> responseObserver) {
    message.release();
    responseObserver.onError(new UnsupportedOperationException("request-stream not implemented"));
  }

  @Override
  public StreamObserver<Message> requestChannel(StreamObserver<Message> responseObserver) {
    responseObserver.onError(new UnsupportedOperationException("request-channel not implemented"));
    return NOOP_OBSERVER;
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

  static final StreamObserver<Message> NOOP_OBSERVER =
      new StreamObserver<Message>() {
        @Override
        public void onNext(Message message) {
          message.release();
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
      };
}
