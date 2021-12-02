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

import com.jauntsdn.rsocket.exceptions.RpcException;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.reactivestreams.Publisher;

public final class RSocketRpcHandler implements RSocketHandler {
  private static final String NO_DEFAULT_ZERO_SERVICES_MESSAGE =
      "RSocketRpcHandler: no default service because 0 services registered";
  private static final String NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE =
      "RSocketRpcHandler: no default service because more than 1 service registered";

  private final Map<String, RSocketRpcService> services;
  private final RSocketRpcService defaultService;
  private final CompletableFuture<Void> onClose = new CompletableFuture<>();
  private final Consumer<Throwable> errorConsumer;

  public static RSocketRpcHandler create(RSocketRpcService... rSocketServices) {
    return new RSocketRpcHandler(null, rSocketServices);
  }

  public static RSocketRpcHandler create(
      Consumer<Throwable> errorConsumer, RSocketRpcService... rSocketServices) {
    return new RSocketRpcHandler(null, rSocketServices);
  }

  public static Factory create(RSocketRpcService.Factory<?>... rSocketServices) {
    return new Factory(null, rSocketServices);
  }

  public static Factory create(
      Consumer<Throwable> errorConsumer, RSocketRpcService.Factory<?>... rSocketServices) {
    return new Factory(errorConsumer, rSocketServices);
  }

  RSocketRpcHandler(
      @Nullable Consumer<Throwable> errorConsumer, RSocketRpcService... rSocketServices) {
    this.errorConsumer = errorConsumer;
    Objects.requireNonNull(rSocketServices, "rSocketServices");
    int length = rSocketServices.length;
    switch (length) {
      case 0:
        {
          services = Collections.emptyMap();
          defaultService = null;
        }
        break;
      case 1:
        {
          RSocketRpcService service = rSocketServices[0];
          defaultService = service;
          services = Collections.singletonMap(service.service(), service);
          break;
        }
      default:
        {
          defaultService = null;
          Map<String, RSocketRpcService> svcs = services = new HashMap<>(length);
          for (RSocketRpcService rSocketService : rSocketServices) {
            String service = rSocketService.service();
            svcs.put(service, rSocketService);
          }
        }
    }
  }

  @Override
  public Completable fireAndForget(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Completable.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.fireAndForget(message);
          default:
            message.release();
            return Completable.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Completable.error(new RpcException(serviceName));
      }

      return rSocketService.fireAndForget(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Completable.error(t);
    }
  }

  @Override
  public Single<Message> requestResponse(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Single.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestResponse(message);
          default:
            message.release();
            return Single.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Single.error(new RpcException(serviceName));
      }
      return rSocketService.requestResponse(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Single.error(t);
    }
  }

  @Override
  public Flowable<Message> requestStream(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Flowable.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestStream(message);
          default:
            message.release();
            return Flowable.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Flowable.error(new RpcException(serviceName));
      }

      return rSocketService.requestStream(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Flowable.error(t);
    }
  }

  @Override
  public Flowable<Message> requestChannel(Publisher<Message> messages) {
    return Flowable.error(
        new RpcException(
            "RSocketRpcHandler: unsupported method: requestChannel(Publisher<Payload>)"));
  }

  @Override
  public Flowable<Message> requestChannel(Message message, Publisher<Message> payloads) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Flowable.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestChannel(message, payloads);
          default:
            message.release();
            return Flowable.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Flowable.error(new RpcException(serviceName));
      }

      return rSocketService.requestChannel(message, payloads);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Flowable.error(t);
    }
  }

  @Override
  public Completable metadataPush(Message message) {
    message.release();
    return Completable.error(new RpcException("RSocketRpcHandler: metadataPush not implemented"));
  }

  @Override
  public void dispose() {
    Map<String, RSocketRpcService> svcs = services;
    if (svcs.isEmpty()) {
      return;
    }
    svcs.forEach(
        (serviceName, rSocketRpcService) -> {
          try {
            rSocketRpcService.dispose();
          } catch (Throwable t) {
            if (t instanceof Error) {
              throw t;
            }
            Consumer<Throwable> c = errorConsumer;
            if (c != null) {
              c.accept(
                  new RpcException("RSocket-RPC service " + serviceName + " dispose error", t));
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
  public Completable onClose() {
    return Completable.fromCompletionStage(onClose);
  }

  static String service(ByteBuf metadata) {
    long header = Rpc.RpcMetadata.header(metadata);
    int flags = Rpc.RpcMetadata.flags(header);
    return Rpc.RpcMetadata.service(metadata, header, flags);
  }

  public static final class Factory implements RSocketRpcService.Factory<RSocketRpcHandler> {
    private final Consumer<Throwable> errorConsumer;
    private final RSocketRpcService.Factory<?>[] serviceFactories;

    Factory(
        @Nullable Consumer<Throwable> errorConsumer,
        RSocketRpcService.Factory<?>... serviceFactories) {
      this.errorConsumer = errorConsumer;
      this.serviceFactories = Objects.requireNonNull(serviceFactories, "serviceFactories");
    }

    @Override
    public RSocketRpcHandler withLifecycle(Closeable requester) {
      RSocketRpcService.Factory<?>[] factories = serviceFactories;
      RSocketRpcService[] services = new RSocketRpcService[factories.length];
      for (int i = 0; i < factories.length; i++) {
        RSocketRpcService.Factory<?> factory = factories[i];
        RSocketHandler handler = factory.withLifecycle(requester);
        if (handler instanceof RSocketRpcService) {
          services[i++] = (RSocketRpcService) handler;
        } else {
          throw new IllegalArgumentException(
              "RpcService.Factory "
                  + factory.getClass()
                  + " created non - RSocketRpcService: "
                  + handler.getClass());
        }
      }
      return new RSocketRpcHandler(errorConsumer, services);
    }
  }
}
