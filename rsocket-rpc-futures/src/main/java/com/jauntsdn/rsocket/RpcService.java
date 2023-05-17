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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public interface RpcService extends MessageStreams {

  String service();

  Class<?> serviceType();

  interface Factory<T extends MessageStreams> {

    T withLifecycle(Closeable requester);
  }

  final class ResponseListener<T> implements BiConsumer<T, Throwable> {
    private final Future<?> cancelHandle;
    private final BiConsumer<? super T, ? super Throwable> listener;

    private ResponseListener(
        CompletionStage<?> response, @Nullable BiConsumer<? super T, ? super Throwable> listener) {
      this.cancelHandle = response.toCompletableFuture();
      this.listener = listener;
    }

    public static <T> ResponseListener<T> create(
        CompletionStage<?> response, BiConsumer<? super T, ? super Throwable> listener) {
      return new ResponseListener<T>(response, listener);
    }

    public static <T> ResponseListener<T> create(CompletionStage<?> response) {
      return new ResponseListener<>(response, null);
    }

    @Override
    public void accept(T t, Throwable throwable) {
      if (throwable instanceof CancellationException) {
        cancelHandle.cancel(true);
      }
      BiConsumer<? super T, ? super Throwable> l = listener;
      if (l != null) {
        l.accept(t, throwable);
      }
    }
  }

  @SuppressWarnings("all")
  abstract class ServerFactory<T extends MessageStreams> implements RpcService.Factory<T> {
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
          requester.onClose().thenAccept(ignored -> rpcCodec.dispose());
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
