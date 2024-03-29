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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/** Utility for serving multiple {@link RpcService} from single {@link MessageStreams} endpoint. */
public final class RpcHandler implements MessageStreamsHandler {
  private static final String NO_DEFAULT_ZERO_SERVICES_MESSAGE =
      "RpcHandler: no default service because 0 services registered";
  private static final String NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE =
      "RpcHandler: no default service because more than 1 service registered";

  private final Map<String, RpcService> services;
  private final RpcService defaultService;
  private final MonoProcessor<Void> onClose = MonoProcessor.create();
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
  public Mono<Void> fireAndForget(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Mono.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.fireAndForget(message);
          default:
            message.release();
            return Mono.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        return Mono.error(new RpcException(serviceName));
      }

      return rpcService.fireAndForget(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Message> requestResponse(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Mono.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestResponse(message);
          default:
            message.release();
            return Mono.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        return Mono.error(new RpcException(serviceName));
      }
      return rpcService.requestResponse(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Mono.error(t);
    }
  }

  @Override
  public Flux<Message> requestStream(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Flux.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestStream(message);
          default:
            message.release();
            return Flux.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        return Flux.error(new RpcException(serviceName));
      }

      return rpcService.requestStream(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Message> requestChannel(Publisher<Message> messages) {
    return Flux.error(
        new RpcException("RpcHandler: unsupported method: requestChannel(Publisher<Payload>)"));
  }

  @Override
  public Flux<Message> requestChannel(Message message, Publisher<Message> messages) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Flux.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestChannel(message, messages);
          default:
            message.release();
            return Flux.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RpcService rpcService = services.get(serviceName);
      if (rpcService == null) {
        message.release();
        return Flux.error(new RpcException(serviceName));
      }

      return rpcService.requestChannel(message, messages);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Flux.error(t);
    }
  }

  @Override
  public void dispose() {
    Map<String, RpcService> svcs = services;
    if (svcs.isEmpty()) {
      onClose.onComplete();
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
    onClose.onComplete();
  }

  @Override
  public boolean isDisposed() {
    return onClose.isDisposed();
  }

  @Override
  public Mono<Void> onClose() {
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
        MessageStreamsHandler handler = factory.withLifecycle(requester);
        if (handler instanceof RpcService) {
          services[i] = (RpcService) handler;
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
