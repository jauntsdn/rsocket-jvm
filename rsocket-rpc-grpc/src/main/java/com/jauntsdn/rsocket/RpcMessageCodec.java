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

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.annotation.Nullable;

public final class RpcMessageCodec {

  private RpcMessageCodec() {}

  public static final class FireAndForget {
    private FireAndForget() {}

    public static final class Client {
      private Client() {}

      public static <RespT> StreamObserver<Message> decode(
          StreamObserver<RespT> observer,
          RespT emptyValue,
          @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
        return new FnfObserver<>(observer, emptyValue, instrumentationListener);
      }

      static final class FnfObserver<ReqT, RespT> implements StreamObserver<Message> {
        final StreamObserver<RespT> observer;
        final RespT emptyValue;
        final RpcInstrumentation.Listener<RespT> instrumentationListener;

        @SuppressWarnings("unchecked")
        FnfObserver(
            StreamObserver<RespT> observer,
            RespT emptyValue,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
          this.observer = observer;
          this.emptyValue = emptyValue;
          this.instrumentationListener = instrumentationListener;
          if (observer instanceof ClientResponseObserver) {
            ((ClientResponseObserver<ReqT, RespT>) observer)
                .beforeStart((ClientCallStreamObserver<ReqT>) FNF_CLIENT_CALL_OBSERVER);
          }
          if (instrumentationListener != null) {
            instrumentationListener.onStart();
          }
        }

        @Override
        public void onNext(Message unused) {}

        @Override
        public void onError(Throwable t) {
          observer.onError(t);
          RpcInstrumentation.Listener<?> l = instrumentationListener;
          if (l != null) {
            l.onError(t);
          }
        }

        @Override
        public void onCompleted() {
          StreamObserver<RespT> o = observer;
          o.onNext(emptyValue);
          o.onCompleted();
          RpcInstrumentation.Listener<?> l = instrumentationListener;
          if (l != null) {
            l.onComplete();
          }
        }
      }

      static final ClientCallStreamObserver<?> FNF_CLIENT_CALL_OBSERVER =
          new ClientCallStreamObserver<Object>() {

            @Override
            public void cancel(@Nullable String message, @Nullable Throwable cause) {}

            @Override
            public boolean isReady() {
              return false;
            }

            @Override
            public void setOnReadyHandler(Runnable onReadyHandler) {}

            @Override
            public void request(int count) {}

            @Override
            public void setMessageCompression(boolean enable) {
              if (enable) {
                throw new UnsupportedOperationException("GRPC compression not supported");
              }
            }

            @Override
            public void disableAutoInboundFlowControl() {}

            @Override
            public void onNext(Object value) {
              /*protobuf messages are not refcounted - just drop*/
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
          };
    }

    public static final class Server {
      private Server() {}

      public static <RespT> StreamObserver<RespT> encode(
          StreamObserver<Message> observer,
          @Nullable RpcInstrumentation.Listener<Message> instrumentationListener) {
        return new FnfServerEncode<>(observer, instrumentationListener);
      }
    }

    static final class FnfServerEncode<RespT> extends AbstractServerEncode<RespT> {

      FnfServerEncode(
          StreamObserver<Message> upstream,
          @Nullable RpcInstrumentation.Listener<Message> instrumentationListener) {
        super(upstream, instrumentationListener);
        if (instrumentationListener != null) {
          instrumentationListener.onStart();
        }
      }

      @Override
      public void onNext(RespT unused) {}

      @Override
      public void onError(Throwable t) {
        upstream.onError(t);
        RpcInstrumentation.Listener<Message> l = instrumentationListener;
        if (l != null) {
          l.onError(t);
        }
      }

      @Override
      public void onCompleted() {
        upstream.onCompleted();
        RpcInstrumentation.Listener<Message> l = instrumentationListener;
        if (l != null) {
          l.onComplete();
        }
      }
    }
  }

  public static final class Stream {
    private Stream() {}

    public static final class Server {
      private Server() {}

      public static <RespT> StreamObserver<RespT> encode(
          StreamObserver<Message> observer,
          Function<? super RespT, ? extends Message> encoder,
          @Nullable RpcInstrumentation.Listener<Message> instrumentationListener) {
        return new ResponseServerEncode<>(observer, encoder, instrumentationListener);
      }

      static final class ResponseServerEncode<RespT> extends AbstractServerEncode<RespT> {
        private final Function<? super RespT, ? extends Message> encoder;

        ResponseServerEncode(
            StreamObserver<Message> upstream,
            Function<? super RespT, ? extends Message> encoder,
            @Nullable RpcInstrumentation.Listener<Message> instrumentationListener) {
          super(upstream, instrumentationListener);
          this.encoder = encoder;
          if (instrumentationListener != null) {
            instrumentationListener.onStart();
          }
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
          RpcInstrumentation.Listener<?> l = instrumentationListener;
          if (l == null) {
            super.setOnCancelHandler(onCancelHandler);
            return;
          }
          super.setOnCancelHandler(
              () -> {
                onCancelHandler.run();
                l.onCancel();
              });
        }

        @Override
        public void onNext(RespT t) {
          Message message = encoder.apply(t);
          upstream.onNext(message);
          RpcInstrumentation.Listener<Message> l = instrumentationListener;
          if (l != null) {
            l.onNext(message);
          }
        }

        @Override
        public void onError(Throwable t) {
          upstream.onError(t);
          RpcInstrumentation.Listener<Message> l = instrumentationListener;
          if (l != null) {
            l.onError(t);
          }
        }

        @Override
        public void onCompleted() {
          upstream.onCompleted();
          RpcInstrumentation.Listener<?> l = instrumentationListener;
          if (l != null) {
            l.onComplete();
          }
        }
      }
    }

    public static final class Client {
      private Client() {}

      public static <ReqT, RespT> StreamObserver<Message> decode(
          StreamObserver<RespT> observer,
          Function<Message, RespT> decoder,
          @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
        if (observer instanceof ClientResponseObserver) {
          return new RequestResponseCancellableObserver<>(
              (ClientResponseObserver<ReqT, RespT>) observer, decoder, instrumentationListener);
        }
        return new RequestResponseObserver<>(observer, decoder, instrumentationListener);
      }

      public static <ReqT, RespT> StreamObserver<Message> decode(
          StreamObserver<RespT> observer,
          Function<Message, RespT> decoder,
          @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener,
          @Nullable ScheduledExecutorService scheduler,
          long timeoutMillis) {
        if (observer instanceof ClientResponseObserver) {
          ClientResponseObserver<ReqT, RespT> clientResponseObserver =
              (ClientResponseObserver<ReqT, RespT>) observer;
          if (timeoutMillis <= 0 || scheduler == null) {
            return new RequestResponseCancellableObserver<>(
                clientResponseObserver, decoder, instrumentationListener);
          }
          return new RequestResponseTimeoutObserver<>(
              clientResponseObserver, decoder, instrumentationListener, scheduler, timeoutMillis);
        }
        return new RequestResponseObserver<>(observer, decoder, instrumentationListener);
      }

      static class RequestResponseObserver<RespT> implements StreamObserver<Message> {
        final StreamObserver<RespT> observer;
        final Function<Message, RespT> decoder;
        final RpcInstrumentation.Listener<RespT> instrumentationListener;

        RequestResponseObserver(
            StreamObserver<RespT> observer,
            Function<Message, RespT> decoder,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
          this.observer = observer;
          this.decoder = decoder;
          this.instrumentationListener = instrumentationListener;
          if (instrumentationListener != null) {
            instrumentationListener.onStart();
          }
        }

        @Override
        public void onNext(Message message) {
          RespT t = decoder.apply(message);
          observer.onNext(t);
          RpcInstrumentation.Listener<RespT> l = instrumentationListener;
          if (l != null) {
            l.onNext(t);
          }
        }

        @Override
        public void onError(Throwable t) {
          observer.onError(t);
          RpcInstrumentation.Listener<RespT> l = instrumentationListener;
          if (l != null) {
            l.onError(t);
          }
        }

        @Override
        public void onCompleted() {
          observer.onCompleted();
          RpcInstrumentation.Listener<RespT> l = instrumentationListener;
          if (l != null) {
            l.onComplete();
          }
        }
      }

      static final class RequestResponseTimeoutObserver<ReqT, RespT>
          extends RequestResponseObserver<RespT>
          implements ClientResponseObserver<Message, Message> {
        final long timeoutMillis;
        final ScheduledExecutorService scheduler;
        volatile ScheduledFuture<?> timeoutHandle;
        boolean timeoutCancelled;

        RequestResponseTimeoutObserver(
            ClientResponseObserver<ReqT, RespT> observer,
            Function<Message, RespT> decoder,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener,
            ScheduledExecutorService scheduler,
            long timeoutMillis) {
          super(observer, decoder, instrumentationListener);
          this.scheduler = scheduler;
          this.timeoutMillis = timeoutMillis;
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<Message> requestStream) {
          timeoutHandle =
              scheduler.schedule(
                  () -> {
                    timeoutCancelled = true;
                    TimeoutException e =
                        new TimeoutException("timed out after " + timeoutMillis + " millis");
                    requestStream.cancel(null, e);
                    onError(e);
                  },
                  timeoutMillis,
                  TimeUnit.MILLISECONDS);

          ((ClientResponseObserver<ReqT, RespT>) observer)
              .beforeStart(
                  new ClientCallStreamObserver<ReqT>() {
                    @Override
                    public void cancel(@Nullable String message, @Nullable Throwable cause) {
                      RpcInstrumentation.Listener<?> l = instrumentationListener;
                      if (l != null) {
                        l.onCancel();
                      }
                      requestStream.cancel(message, cause);
                      cancelTimeout();
                    }

                    @Override
                    public boolean isReady() {
                      return requestStream.isReady();
                    }

                    @Override
                    public void setOnReadyHandler(Runnable onReadyHandler) {
                      requestStream.setOnReadyHandler(onReadyHandler);
                    }

                    @Override
                    public void request(int count) {
                      requestStream.request(count);
                    }

                    @Override
                    public void setMessageCompression(boolean enable) {
                      requestStream.setMessageCompression(enable);
                    }

                    @Override
                    public void disableAutoInboundFlowControl() {
                      requestStream.disableAutoInboundFlowControl();
                    }

                    @Override
                    public void disableAutoRequestWithInitial(int request) {
                      requestStream.disableAutoRequestWithInitial(request);
                    }

                    @Override
                    public void onNext(ReqT value) {
                      /*protobuf is not refcounted - ignore*/
                    }

                    @Override
                    public void onError(Throwable t) {
                      requestStream.onError(t);
                    }

                    @Override
                    public void onCompleted() {
                      requestStream.onCompleted();
                    }
                  });
        }

        @Override
        public void onNext(Message message) {
          cancelTimeout();
          super.onNext(message);
        }

        @Override
        public void onError(Throwable t) {
          cancelTimeout();
          super.onError(t);
        }

        @Override
        public void onCompleted() {
          cancelTimeout();
          super.onCompleted();
        }

        void cancelTimeout() {
          if (!timeoutCancelled) {
            timeoutCancelled = true;
            ScheduledFuture<?> h = timeoutHandle;
            if (h != null) {
              h.cancel(true);
            }
          }
        }
      }

      static final class RequestResponseCancellableObserver<ReqT, RespT>
          extends RequestResponseObserver<RespT>
          implements ClientResponseObserver<Message, Message> {

        RequestResponseCancellableObserver(
            ClientResponseObserver<ReqT, RespT> observer,
            Function<Message, RespT> decoder,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
          super(observer, decoder, instrumentationListener);
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<Message> requestStream) {
          ((ClientResponseObserver<ReqT, RespT>) observer)
              .beforeStart(
                  new ClientCallStreamObserver<ReqT>() {
                    @Override
                    public void cancel(@Nullable String message, @Nullable Throwable cause) {
                      RpcInstrumentation.Listener<?> l = instrumentationListener;
                      if (l != null) {
                        l.onCancel();
                      }
                      requestStream.cancel(message, cause);
                    }

                    @Override
                    public boolean isReady() {
                      return requestStream.isReady();
                    }

                    @Override
                    public void setOnReadyHandler(Runnable onReadyHandler) {
                      requestStream.setOnReadyHandler(onReadyHandler);
                    }

                    @Override
                    public void request(int count) {
                      requestStream.request(count);
                    }

                    @Override
                    public void setMessageCompression(boolean enable) {
                      requestStream.setMessageCompression(enable);
                    }

                    @Override
                    public void disableAutoInboundFlowControl() {
                      requestStream.disableAutoInboundFlowControl();
                    }

                    @Override
                    public void disableAutoRequestWithInitial(int request) {
                      requestStream.disableAutoRequestWithInitial(request);
                    }

                    @Override
                    public void onNext(ReqT value) {
                      /*protobuf is not refcounted - ignore*/
                    }

                    @Override
                    public void onError(Throwable t) {
                      requestStream.onError(t);
                    }

                    @Override
                    public void onCompleted() {
                      requestStream.onCompleted();
                    }
                  });
        }
      }
    }
  }

  public static final class Channel {
    private Channel() {}

    public static final class Client {
      private Client() {}

      public static <ReqT, RespT> StreamObserver<Message> decode(
          StreamObserver<RespT> observer,
          Encoder<ReqT> encoder,
          Function<Message, RespT> decoder,
          @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
        if (observer instanceof ClientResponseObserver) {
          return new RequestChannelEncoderObserver<>(
              (ClientResponseObserver<ReqT, RespT>) observer,
              encoder,
              decoder,
              instrumentationListener);
        }
        return new RequestChannelObserver<>(observer, decoder, instrumentationListener);
      }

      public static <ReqT, RespT> StreamObserver<Message> decode(
          StreamObserver<RespT> observer,
          Encoder<ReqT> encoder,
          Function<Message, RespT> decoder,
          @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener,
          @Nullable ScheduledExecutorService scheduler,
          long timeoutMillis) {
        if (observer instanceof ClientResponseObserver) {
          if (timeoutMillis <= 0 || scheduler == null) {
            return new RequestChannelEncoderObserver<>(
                (ClientResponseObserver<ReqT, RespT>) observer,
                encoder,
                decoder,
                instrumentationListener);
          }
          return new RequestChannelTimeoutObserver<>(
              (ClientResponseObserver<ReqT, RespT>) observer,
              encoder,
              decoder,
              instrumentationListener,
              scheduler,
              timeoutMillis);
        }
        return new RequestChannelObserver<>(observer, decoder, instrumentationListener);
      }

      static class RequestChannelObserver<RespT> implements StreamObserver<Message> {
        final StreamObserver<RespT> observer;
        final Function<Message, RespT> decoder;
        final RpcInstrumentation.Listener<RespT> instrumentationListener;

        RequestChannelObserver(
            StreamObserver<RespT> observer,
            Function<Message, RespT> decoder,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
          this.observer = observer;
          this.decoder = decoder;
          this.instrumentationListener = instrumentationListener;
          if (instrumentationListener != null) {
            instrumentationListener.onStart();
          }
        }

        @Override
        public void onNext(Message message) {
          RespT t = decoder.apply(message);
          observer.onNext(t);
          RpcInstrumentation.Listener<RespT> l = instrumentationListener;
          if (l != null) {
            l.onNext(t);
          }
        }

        @Override
        public void onError(Throwable t) {
          observer.onError(t);
          RpcInstrumentation.Listener<RespT> l = instrumentationListener;
          if (l != null) {
            l.onError(t);
          }
        }

        @Override
        public void onCompleted() {
          observer.onCompleted();
          RpcInstrumentation.Listener<RespT> l = instrumentationListener;
          if (l != null) {
            l.onComplete();
          }
        }
      }

      static final class RequestChannelEncoderObserver<ReqT, RespT>
          extends RequestChannelObserver<RespT>
          implements ClientResponseObserver<Message, Message> {
        final Encoder<ReqT> encoder;

        RequestChannelEncoderObserver(
            ClientResponseObserver<ReqT, RespT> observer,
            Encoder<ReqT> encoder,
            Function<Message, RespT> decoder,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener) {
          super(observer, decoder, instrumentationListener);
          this.encoder = encoder;
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<Message> requestStream) {
          ((ClientResponseObserver<ReqT, RespT>) observer)
              .beforeStart(encoder.encodeStream(requestStream, null));
        }
      }

      static final class RequestChannelTimeoutObserver<ReqT, RespT>
          extends RequestChannelObserver<RespT>
          implements ClientResponseObserver<Message, Message> {
        final Encoder<ReqT> encoder;
        final ScheduledExecutorService scheduler;
        final long timeoutMillis;
        volatile ScheduledFuture<?> timeoutHandle;
        boolean timeoutCancelled;

        RequestChannelTimeoutObserver(
            ClientResponseObserver<ReqT, RespT> observer,
            Encoder<ReqT> encoder,
            Function<Message, RespT> decoder,
            @Nullable RpcInstrumentation.Listener<RespT> instrumentationListener,
            ScheduledExecutorService scheduler,
            long timeoutMillis) {
          super(observer, decoder, instrumentationListener);
          this.encoder = encoder;
          this.scheduler = scheduler;
          this.timeoutMillis = timeoutMillis;
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<Message> requestStream) {
          ScheduledFuture<?> h =
              timeoutHandle =
                  scheduler.schedule(
                      () -> {
                        timeoutCancelled = true;
                        TimeoutException e =
                            new TimeoutException("timed out after " + timeoutMillis + " millis");
                        requestStream.cancel(null, e);
                        onError(e);
                      },
                      timeoutMillis,
                      TimeUnit.MILLISECONDS);
          ((ClientResponseObserver<ReqT, RespT>) observer)
              .beforeStart(encoder.encodeStream(requestStream, h));
        }

        @Override
        public void onNext(Message message) {
          cancelTimeout();
          super.onNext(message);
        }

        @Override
        public void onError(Throwable t) {
          cancelTimeout();
          super.onError(t);
        }

        @Override
        public void onCompleted() {
          cancelTimeout();
          super.onCompleted();
        }

        void cancelTimeout() {
          if (!timeoutCancelled) {
            timeoutCancelled = true;
            ScheduledFuture<?> h = timeoutHandle;
            if (h != null) {
              h.cancel(true);
            }
          }
        }
      }

      public abstract static class Encoder<ReqT> {
        final RpcInstrumentation.Listener<?> instrumentationListener;

        public Encoder(@Nullable RpcInstrumentation.Listener<?> instrumentationListener) {
          this.instrumentationListener = instrumentationListener;
        }

        public abstract Message onNext(ReqT message);

        public ClientCallStreamObserver<ReqT> encodeStream(StreamObserver<Message> observer) {
          return encodeStream(observer, null);
        }

        public ClientCallStreamObserver<ReqT> encodeStream(
            StreamObserver<Message> observer, @Nullable ScheduledFuture<?> timeoutHandle) {
          if (!(observer instanceof ClientCallStreamObserver)) {
            throw new IllegalStateException(
                "messageStream.requestChannel() returned value is not ClientCallStreamObserver");
          }
          ClientCallStreamObserver<Message> callObserver =
              (ClientCallStreamObserver<Message>) observer;
          return new ClientCallStreamObserver<ReqT>() {

            @Override
            public void cancel(@Nullable String message, @Nullable Throwable cause) {
              RpcInstrumentation.Listener<?> listener = instrumentationListener;
              if (listener != null) {
                listener.onCancel();
              }
              if (timeoutHandle != null) {
                timeoutHandle.cancel(true);
              }
              callObserver.cancel(message, cause);
            }

            @Override
            public boolean isReady() {
              return callObserver.isReady();
            }

            @Override
            public void setOnReadyHandler(Runnable onReadyHandler) {
              callObserver.setOnReadyHandler(onReadyHandler);
            }

            @Override
            public void disableAutoInboundFlowControl() {
              callObserver.disableAutoInboundFlowControl();
            }

            @Override
            public void disableAutoRequestWithInitial(int request) {
              callObserver.disableAutoRequestWithInitial(request);
            }

            @Override
            public void request(int count) {
              callObserver.request(count);
            }

            @Override
            public void setMessageCompression(boolean enable) {
              callObserver.setMessageCompression(enable);
            }

            @Override
            public void onNext(ReqT value) {
              callObserver.onNext(Encoder.this.onNext(value));
            }

            @Override
            public void onError(Throwable t) {
              callObserver.onError(t);
            }

            @Override
            public void onCompleted() {
              callObserver.onCompleted();
            }
          };
        }
      }
    }
  }

  public static ScheduledExecutorService timeoutScheduler(
      MessageStreams messageStreams, long timeoutMillis) {
    if (timeoutMillis <= 0) {
      return null;
    }
    return messageStreams.scheduler().orElse(null);
  }

  abstract static class AbstractServerEncode<RespT> extends ServerCallStreamObserver<RespT> {
    final ServerCallStreamObserver<Message> upstream;
    final RpcInstrumentation.Listener<Message> instrumentationListener;

    AbstractServerEncode(
        StreamObserver<Message> upstream,
        @Nullable RpcInstrumentation.Listener<Message> instrumentationListener) {
      this.upstream = (ServerCallStreamObserver<Message>) upstream;
      this.instrumentationListener = instrumentationListener;
    }

    @Override
    public final boolean isCancelled() {
      return upstream.isCancelled();
    }

    @Override
    public void setOnCloseHandler(Runnable onCloseHandler) {
      upstream.setOnCloseHandler(onCloseHandler);
    }

    @Override
    public void setOnCancelHandler(Runnable onCancelHandler) {
      upstream.setOnCancelHandler(onCancelHandler);
    }

    @Override
    public final void setCompression(String compression) {
      upstream.setCompression(compression);
    }

    @Override
    public final boolean isReady() {
      return upstream.isReady();
    }

    @Override
    public final void setOnReadyHandler(Runnable onReadyHandler) {
      upstream.setOnReadyHandler(onReadyHandler);
    }

    @Override
    public final void disableAutoInboundFlowControl() {
      upstream.disableAutoInboundFlowControl();
    }

    @Override
    public void disableAutoRequest() {
      upstream.disableAutoRequest();
    }

    @Override
    public final void request(int count) {
      upstream.request(count);
    }

    @Override
    public final void setMessageCompression(boolean enable) {
      upstream.setMessageCompression(enable);
    }
  }
}
