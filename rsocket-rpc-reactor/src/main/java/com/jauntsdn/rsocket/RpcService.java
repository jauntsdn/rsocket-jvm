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
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public interface RpcService extends MessageStreamsHandler {

  String service();

  Class<?> serviceType();

  interface Factory<T extends MessageStreamsHandler> {

    T withLifecycle(Closeable requester);
  }

  @SuppressWarnings("all")
  abstract class ServerFactory<T extends MessageStreamsHandler> implements RpcService.Factory<T> {
    private final Object service;
    private final Optional<RpcInstrumentation> instrumentation;

    public ServerFactory(Object service, Optional<RpcInstrumentation> instrumentation) {
      this.service = Objects.requireNonNull(service, "service");
      this.instrumentation = Objects.requireNonNull(instrumentation, "instrumentation");
    }

    public ServerFactory(Object service) {
      this.service = Objects.requireNonNull(service, "service");
      this.instrumentation = null;
    }

    protected final <S> S service() {
      return (S) service;
    }

    @Override
    public final T withLifecycle(Closeable requester) {
      Objects.requireNonNull(requester, "requester");
      Rpc.Codec rpcCodec = requester.attributes().attr(Attributes.RPC_CODEC);
      if (rpcCodec != null) {
        if (rpcCodec.isDisposable()) {
          requester.onClose().subscribe(ignored -> rpcCodec.dispose());
        }
        ByteBufAllocator alloc = requester.attributes().attr(Attributes.ALLOCATOR);
        ByteBufAllocator allocator = alloc != null ? alloc : ByteBufAllocator.DEFAULT;
        Optional<RpcInstrumentation> instr = instrumentation;
        RpcInstrumentation rpcInstrumentation =
            instr == null
                ? requester.attributes().attr(Attributes.RPC_INSTRUMENTATION)
                : instr.orElse(null);
        return create(rpcInstrumentation, allocator, rpcCodec);
      }
      throw new IllegalArgumentException(
          "Requester " + requester.getClass() + " does not provide RPC codec");
    }

    public abstract T create(
        @Nullable RpcInstrumentation rpcInstrumentation,
        ByteBufAllocator allocator,
        Rpc.Codec codec);
  }
}
