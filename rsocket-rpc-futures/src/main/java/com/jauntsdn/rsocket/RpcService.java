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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public interface RpcService extends MessageStreams {

  String service();

  Class<?> serviceType();

  interface Factory<T extends MessageStreams> {

    T withLifecycle(Closeable requester);
  }

  class ResponseListener<T> implements BiConsumer<T, Throwable> {
    final Future<?> cancelHandle;
    final BiConsumer<? super T, ? super Throwable> listener;

    ResponseListener(
        CompletionStage<?> response, @Nullable BiConsumer<? super T, ? super Throwable> listener) {
      this.cancelHandle = response.toCompletableFuture();
      this.listener = listener;
    }

    public static <T> ResponseListener<T> create(
        CompletionStage<?> response, BiConsumer<? super T, ? super Throwable> listener) {
      return new ResponseListener<>(response, listener);
    }

    public static <T> ResponseListener<T> create(
        CompletionStage<?> response,
        BiConsumer<? super T, ? super Throwable> listener,
        @Nullable ScheduledExecutorService timeoutScheduler,
        long timeoutMillis) {
      return new ResponseTimeoutListener<T>(response, listener)
          .scheduleTimeout(timeoutScheduler, timeoutMillis);
    }

    public static <T> ResponseListener<T> create(CompletionStage<?> response) {
      return new ResponseListener<>(response, null);
    }

    public static <T> ResponseListener<T> create(
        CompletionStage<?> response,
        @Nullable ScheduledExecutorService timeoutScheduler,
        long timeoutMillis) {
      return new ResponseTimeoutListener<T>(response, null)
          .scheduleTimeout(timeoutScheduler, timeoutMillis);
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

  final class ResponseTimeoutListener<T> extends ResponseListener<T> {
    volatile ScheduledFuture<?> timeoutHandle;

    ResponseTimeoutListener(
        CompletionStage<?> response, @Nullable BiConsumer<? super T, ? super Throwable> listener) {
      super(response, listener);
    }

    ResponseListener<T> scheduleTimeout(
        @Nullable ScheduledExecutorService timeoutScheduler, long timeoutMillis) {
      if (timeoutMillis > 0 && timeoutScheduler != null) {
        timeoutHandle =
            timeoutScheduler.schedule(
                () -> {
                  cancelHandle.cancel(true);
                },
                timeoutMillis,
                TimeUnit.MILLISECONDS);
      }
      return this;
    }

    @Override
    public void accept(T t, Throwable throwable) {
      ScheduledFuture<?> h = timeoutHandle;
      if (h != null) {
        h.cancel(true);
      }
      super.accept(t, throwable);
    }
  }

  static ScheduledExecutorService timeoutScheduler(
      MessageStreams messageStreams, long timeoutMillis) {
    if (timeoutMillis <= 0) {
      return null;
    }
    return messageStreams.scheduler().orElse(null);
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
