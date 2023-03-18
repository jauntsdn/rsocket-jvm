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

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.netty.util.ReferenceCounted;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;

public abstract class AbstractRSocket implements RSocketHandler {

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
    return MessageStreamsHandler.noopServerObserver();
  }

  @Override
  public StreamObserver<Message> requestChannel(
      Message message, StreamObserver<Message> responseObserver) {
    message.release();
    responseObserver.onError(new UnsupportedOperationException("request-channel not implemented"));
    return MessageStreamsHandler.noopServerObserver();
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

  static final ClientCallStreamObserver<?> NOOP_CLIENT_OBSERVER =
      new ClientCallStreamObserver<Object>() {
        @Override
        public void cancel(@Nullable String message, @Nullable Throwable cause) {}

        @Override
        public boolean isReady() {
          return false;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {}

        @Override
        public void disableAutoInboundFlowControl() {}

        @Override
        public void disableAutoRequestWithInitial(int request) {}

        @Override
        public void request(int count) {}

        @Override
        public void setMessageCompression(boolean enable) {}

        @Override
        public void onNext(Object value) {
          if (value instanceof ReferenceCounted) {
            ((ReferenceCounted) value).release();
          }
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
      };

  static final ServerCallStreamObserver<?> NOOP_SERVER_OBSERVER =
      new ServerCallStreamObserver<Object>() {
        @Override
        public boolean isCancelled() {
          return false;
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {}

        @Override
        public void setCompression(String compression) {}

        @Override
        public boolean isReady() {
          return false;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {}

        @Override
        public void request(int count) {}

        @Override
        public void setMessageCompression(boolean enable) {}

        @Override
        public void disableAutoInboundFlowControl() {}

        @Override
        public void disableAutoRequest() {
          super.disableAutoRequest();
        }

        @Override
        public void setOnCloseHandler(Runnable onCloseHandler) {
          Objects.requireNonNull(onCloseHandler, "onCloseHandler").run();
        }

        @Override
        public void onNext(Object value) {
          if (value instanceof ReferenceCounted) {
            ((ReferenceCounted) value).release();
          }
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
      };
}
