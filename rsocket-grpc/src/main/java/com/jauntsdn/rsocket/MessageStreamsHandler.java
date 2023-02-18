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

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public interface MessageStreamsHandler extends MessageStreams {

  default StreamObserver<Message> requestChannel(
      Message message, StreamObserver<Message> responseObserver) {
    return requestChannel(responseObserver);
  }

  @SuppressWarnings("unchecked")
  static <T> ServerCallStreamObserver<T> noopServerObserver() {
    return (ServerCallStreamObserver<T>) AbstractRSocket.NOOP_SERVER_OBSERVER;
  }
}
