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

public final class RSocketRpcHandler implements RSocketHandler {
  private static final String NO_DEFAULT_ZERO_SERVICES_MESSAGE =
      "RSocketRpcHandler: no default service because 0 services registered";
  private static final String NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE =
      "RSocketRpcHandler: no default service because more than 1 service registered";

  private final Map<String, RSocketRpcService> services;
  private final RSocketRpcService defaultService;
  private final MonoProcessor<Void> onClose = MonoProcessor.create();
  private final Consumer<Throwable> errorConsumer;

  public RSocketRpcHandler(RSocketRpcService... rSocketServices) {
    this(null, rSocketServices);
  }

  public RSocketRpcHandler(
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

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Mono.error(new RpcException(serviceName));
      }

      return rSocketService.fireAndForget(message);
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

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Mono.error(new RpcException(serviceName));
      }
      return rSocketService.requestResponse(message);
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

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Flux.error(new RpcException(serviceName));
      }

      return rSocketService.requestStream(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Message> requestChannel(Publisher<Message> messages) {
    return Flux.error(
        new RpcException(
            "RSocketRpcHandler: unsupported method: requestChannel(Publisher<Payload>)"));
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

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Flux.error(new RpcException(serviceName));
      }

      return rSocketService.requestChannel(message, messages);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Flux.error(t);
    }
  }

  @Override
  public Mono<Void> metadataPush(Message message) {
    message.release();
    return Mono.error(new RpcException("RSocketRpcHandler: metadataPush not implemented"));
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
}
