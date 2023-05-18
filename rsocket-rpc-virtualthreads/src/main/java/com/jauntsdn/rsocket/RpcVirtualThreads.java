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

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Supplier;

public abstract class RpcVirtualThreads {
  private static final AtomicIntegerFieldUpdater<RpcVirtualThreads> CALL_COUNT =
      AtomicIntegerFieldUpdater.newUpdater(RpcVirtualThreads.class, "callCount");
  final EventExecutor eventExecutor;
  private final Queue<Runnable> callQueue = new MpscUnboundedArrayQueue<>(128);
  private volatile int callCount;

  RpcVirtualThreads(EventExecutor eventExecutor) {
    this.eventExecutor = eventExecutor;
  }

  final void runOnEventLoop(Runnable call) {
    callQueue.offer(call);
    if (CALL_COUNT.getAndIncrement(this) == 0) {
      eventExecutor.execute(this::run);
    }
  }

  final void run() {
    do {
      callQueue.poll().run();
    } while (CALL_COUNT.decrementAndGet(this) != 0);
  }

  public static final class ServerCalls extends RpcVirtualThreads {
    private ServerCalls(EventExecutor eventExecutor) {
      super(eventExecutor);
    }

    public static ServerCalls create(Closeable messageStreams) {
      if (!(messageStreams instanceof MessageStreams)) {
        throw new IllegalArgumentException(
            "Requester is not MessageStreams: " + messageStreams.getClass().getName());
      }
      ScheduledExecutorService scheduler =
          ((MessageStreams) messageStreams).scheduler().orElse(null);
      if (scheduler == null) {
        throw new IllegalArgumentException(
            "MessageStreams does not have scheduler: " + messageStreams.getClass().getName());
      }
      if (!(scheduler instanceof EventExecutor)) {
        throw new IllegalArgumentException(
            "MessageStreams scheduler is not EventExecutor: " + scheduler.getClass().getName());
      }
      return new ServerCalls((EventExecutor) scheduler);
    }

    public CompletionStage<Void> fireAndForget(Runnable fireAndForgetCall) {
      var downstream = new CompletableFuture<Void>();
      Thread.startVirtualThread(
          () -> {
            try {
              fireAndForgetCall.run();
              runOnEventLoop(() -> downstream.complete(null));
            } catch (Throwable t) {
              runOnEventLoop(() -> downstream.completeExceptionally(t));
            }
          });
      return downstream;
    }

    public <T> CompletionStage<Message> requestResponse(RequestResponse<T> requestResponseCall) {
      var downstream = new CompletableFuture<Message>();
      Thread virtualThread =
          Thread.startVirtualThread(
              () -> {
                try {
                  T orderResponse = requestResponseCall.handle();
                  runOnEventLoop(
                      () -> {
                        try {
                          downstream.complete(requestResponseCall.encode(orderResponse));
                        } catch (Exception e) {
                          downstream.completeExceptionally(e);
                        }
                      });
                } catch (Exception e) {
                  if (downstream.isDone()) {
                    return;
                  }
                  runOnEventLoop(() -> downstream.completeExceptionally(e));
                }
              });

      downstream.whenComplete(
          (msg, err) -> {
            if (err instanceof CancellationException) {
              virtualThread.interrupt();
            }
          });

      return downstream;
    }

    public interface RequestResponse<T> {

      T handle();

      Message encode(T response);
    }
  }

  public static final class ClientCalls extends RpcVirtualThreads {

    private ClientCalls(EventExecutor eventExecutor) {
      super(eventExecutor);
    }

    public static ClientCalls create(MessageStreams messageStreams) {
      Optional<ScheduledExecutorService> executorService = messageStreams.scheduler();
      var executor = executorService.orElse(null);
      if (executor == null) {
        throw new IllegalArgumentException("executorService is not present");
      }
      if (!(executor instanceof EventExecutor)) {
        throw new IllegalArgumentException(
            "executorService is not EventExecutor: " + executor.getClass().getName());
      }
      return new ClientCalls((EventExecutor) executor);
    }

    public void fireAndForget(Supplier<CompletionStage<Void>> fireAndForget) {
      if (eventExecutor.inEventLoop()) {
        fireAndForget.get();
        return;
      }
      if (!Thread.currentThread().isVirtual()) {
        throw new IllegalStateException("must be called on virtual thread");
      }
      var downstream = new CompletableFuture<Void>();

      runOnEventLoop(
          () -> {
            try {
              fireAndForget.get();
              downstream.complete(null);
            } catch (Throwable t) {
              downstream.completeExceptionally(t);
            }
          });

      try {
        downstream.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new CompletionException(e);
      }
    }

    public <T> T requestResponse(Supplier<CompletionStage<T>> requestResponse) {
      if (!Thread.currentThread().isVirtual()) {
        throw new IllegalStateException("must be called by virtual thread");
      }
      var downstream = new CompletableFuture<T>();
      runOnEventLoop(
          () -> {
            CompletableFuture<T> upstream;
            try {
              upstream = requestResponse.get().toCompletableFuture();
            } catch (Throwable t) {
              downstream.completeExceptionally(t);
              return;
            }

            upstream.whenComplete(
                (t, err) -> {
                  if (err != null) {
                    if (!(err instanceof CancellationException)) {
                      downstream.completeExceptionally(err);
                    }
                  } else {
                    downstream.complete(t);
                  }
                });

            downstream.whenComplete(
                (t, err) -> {
                  if (err instanceof CancellationException) {
                    upstream.cancel(true);
                  }
                });
          });
      try {
        return downstream.get();
      } catch (InterruptedException | ExecutionException e) {
        if (e instanceof InterruptedException) {
          downstream.cancel(true);
        }
        throw new CompletionException(e);
      }
    }
  }
}
