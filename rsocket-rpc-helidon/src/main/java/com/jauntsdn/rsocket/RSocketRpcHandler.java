/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import com.jauntsdn.rsocket.exceptions.RpcException;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public final class RSocketRpcHandler implements RSocketHandler {
  private static final String NO_DEFAULT_ZERO_SERVICES_MESSAGE =
      "RSocketRpcHandler: no default service because 0 services registered";
  private static final String NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE =
      "RSocketRpcHandler: no default service because more than 1 service registered";

  private final Map<String, RSocketRpcService> services;
  private final RSocketRpcService defaultService;
  private final CompletableFuture<Void> onClose = new CompletableFuture<>();
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
  public Single<Void> fireAndForget(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Single.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.fireAndForget(message);
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

      return rSocketService.fireAndForget(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Single.error(t);
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
  public Multi<Message> requestStream(Message message) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Multi.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestStream(message);
          default:
            message.release();
            return Multi.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Multi.error(new RpcException(serviceName));
      }

      return rSocketService.requestStream(message);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Multi.error(t);
    }
  }

  @Override
  public Multi<Message> requestChannel(Flow.Publisher<Message> messages) {
    return Multi.error(
        new RpcException(
            "RSocketRpcHandler: unsupported method: requestChannel(Publisher<Payload>)"));
  }

  @Override
  public Multi<Message> requestChannel(Message message, Flow.Publisher<Message> messages) {
    try {
      String serviceName = service(message.metadata());

      if (serviceName.isEmpty()) {
        int size = services.size();
        switch (size) {
          case 0:
            message.release();
            return Multi.error(new RpcException(NO_DEFAULT_ZERO_SERVICES_MESSAGE));
          case 1:
            return defaultService.requestChannel(message, messages);
          default:
            message.release();
            return Multi.error(new RpcException(NO_DEFAULT_MULTIPLE_SERVICES_MESSAGE));
        }
      }

      RSocketRpcService rSocketService = services.get(serviceName);
      if (rSocketService == null) {
        message.release();
        return Multi.error(new RpcException(serviceName));
      }

      return rSocketService.requestChannel(message, messages);
    } catch (Throwable t) {
      ReferenceCountUtil.safeRelease(message);
      return Multi.error(t);
    }
  }

  @Override
  public Single<Void> metadataPush(Message message) {
    message.release();
    return Single.error(new RpcException("RSocketRpcHandler: unsupported method: metadataPush"));
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
  public Single<Void> onClose() {
    return Single.create(onClose, true);
  }

  static String service(ByteBuf metadata) {
    long header = Rpc.RpcMetadata.header(metadata);
    int flags = Rpc.RpcMetadata.flags(header);
    return Rpc.RpcMetadata.service(metadata, header, flags);
  }
}
