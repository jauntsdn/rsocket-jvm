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

import com.jauntsdn.rsocket.exceptions.RpcException;
import io.grpc.stub.StreamObserver;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public final class RpcHandler implements MessageStreamsHandler {
  private static final String NO_DEFAULT_ZERO_SERVICES_MESSAGE =
      "RpcHandler: no default service because 0 services registered";
  private static final String NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE =
      "RpcHandler: no default service because more than 1 service registered";

  private final Map<String, RpcService> services;
  private final RpcService defaultService;
  private final CompletableFuture<Void> onClose = new CompletableFuture<>();
  private final Consumer<Throwable> errorConsumer;

  public static RpcHandler create(RpcService... rpcServices) {
    return new RpcHandler(null, rpcServices);
  }

  public static RpcHandler create(Consumer<Throwable> errorConsumer, RpcService... rpcServices) {
    return new RpcHandler(null, rpcServices);
  }

  public static Factory create(RpcService.Factory<?>... rpcServices) {
    return new Factory(null, rpcServices);
  }

  public static Factory create(
      Consumer<Throwable> errorConsumer, RpcService.Factory<?>... rpcServices) {
    return new Factory(errorConsumer, rpcServices);
  }

  RpcHandler(@Nullable Consumer<Throwable> errorConsumer, RpcService... rpcServices) {
    this.errorConsumer = errorConsumer;
    Objects.requireNonNull(rpcServices, "rpcServices");
    int length = rpcServices.length;
    switch (length) {
      case 0:
        {
          services = Collections.emptyMap();
          defaultService = null;
        }
        break;
      case 1:
        {
          RpcService service = rpcServices[0];
          defaultService = service;
          services = Collections.singletonMap(service.service(), service);
          break;
        }
      default:
        {
          defaultService = null;
          Map<String, RpcService> svcs = services = new HashMap<>(length);
          for (RpcService rpcService : rpcServices) {
            String service = rpcService.service();
            svcs.put(service, rpcService);
          }
        }
    }
  }

  @Override
  public void fireAndForget(Message message, StreamObserver<Message> responseObserver) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
            return;
          case 1:
            defaultService.fireAndForget(message, responseObserver);
            return;
          default:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
            return;
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        responseObserver.onError(new RpcException(serviceName));
        return;
      }
      rpcService.fireAndForget(message, responseObserver);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      responseObserver.onError(t);
    }
  }

  @Override
  public void requestResponse(Message message, StreamObserver<Message> responseObserver) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
            return;
          case 1:
            defaultService.requestResponse(message, responseObserver);
            return;
          default:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
            return;
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        responseObserver.onError(new RpcException(serviceName));
        return;
      }
      rpcService.requestResponse(message, responseObserver);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      responseObserver.onError(t);
    }
  }

  @Override
  public void requestStream(Message message, StreamObserver<Message> responseObserver) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
            return;
          case 1:
            defaultService.requestStream(message, responseObserver);
            return;
          default:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
            return;
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        responseObserver.onError(new RpcException(serviceName));
        return;
      }
      rpcService.requestStream(message, responseObserver);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      responseObserver.onError(t);
    }
  }

  @Override
  public StreamObserver<Message> requestChannel(StreamObserver<Message> responseObserver) {
    responseObserver.onError(
        new RpcException("RpcHandler: unsupported method: requestChannel(Publisher<Payload>)"));
    return AbstractRSocket.NOOP_OBSERVER;
  }

  @Override
  public StreamObserver<Message> requestChannel(
      Message message, StreamObserver<Message> responseObserver) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
            return AbstractRSocket.NOOP_OBSERVER;
          case 1:
            defaultService.requestStream(message, responseObserver);
            return AbstractRSocket.NOOP_OBSERVER;
          default:
            message.release();
            responseObserver.onError(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
            return AbstractRSocket.NOOP_OBSERVER;
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        responseObserver.onError(new RpcException(serviceName));
        return AbstractRSocket.NOOP_OBSERVER;
      }
      return rpcService.requestChannel(message, responseObserver);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      responseObserver.onError(t);
      return AbstractRSocket.NOOP_OBSERVER;
    }
  }

  @Override
  public void dispose() {
    Map<String, RpcService> svcs = services;
    if (svcs.isEmpty()) {
      onClose.complete(null);
      return;
    }
    svcs.forEach(
        (serviceName, rpcService) -> {
          try {
            rpcService.dispose();
          } catch (Throwable t) {
            if (t instanceof Error) {
              throw t;
            }
            Consumer<Throwable> c = errorConsumer;
            if (c != null) {
              c.accept(new RpcException("RPC service " + serviceName + " dispose error", t));
            }
          }
        });
    onClose.complete(null);
  }

  @Override
  public boolean isDisposed() {
    return onClose.isDone();
  }

  @Override
  public CompletionStage<Void> onClose() {
    return onClose;
  }

  static String service(ByteBuf metadata) {
    long header = Rpc.RpcMetadata.header(metadata);
    int flags = Rpc.RpcMetadata.flags(header);
    return Rpc.RpcMetadata.service(metadata, header, flags);
  }

  public static final class Factory implements RpcService.Factory<RpcHandler> {
    private final Consumer<Throwable> errorConsumer;
    private final RpcService.Factory<?>[] serviceFactories;

    Factory(
        @Nullable Consumer<Throwable> errorConsumer, RpcService.Factory<?>... serviceFactories) {
      this.errorConsumer = errorConsumer;
      this.serviceFactories = Objects.requireNonNull(serviceFactories, "serviceFactories");
    }

    @Override
    public RpcHandler withLifecycle(Closeable requester) {
      RpcService.Factory<?>[] factories = serviceFactories;
      RpcService[] services = new RpcService[factories.length];
      for (int i = 0; i < factories.length; i++) {
        RpcService.Factory<?> factory = factories[i];
        MessageStreams handler = factory.withLifecycle(requester);
        if (handler instanceof RpcService) {
          services[i++] = (RpcService) handler;
        } else {
          throw new IllegalArgumentException(
              "RpcService.Factory "
                  + factory.getClass()
                  + " created non - RpcService: "
                  + handler.getClass());
        }
      }
      return new RpcHandler(errorConsumer, services);
    }
  }
}
