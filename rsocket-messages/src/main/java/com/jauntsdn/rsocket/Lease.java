/*
 * Copyright 2021 - present Maksym Ostroverkhov.
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

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;

/** Response stats recorder and request lease controller configuration. */
public final class Lease {

  private Lease() {}

  public static final class Metadata {
    private final long[] serviceCallLatencies = new long[2];
    private int allowedFnfRequests;

    private Metadata() {}

    public static Metadata create() {
      return new Metadata();
    }

    public static int serviceCallMaxCount() {
      return 2;
    }

    /**
     * @param allowedFnfRequests number of allowed fire-and-forget calls in addition to provided by
     *     lease itself.
     * @return this Metadata instance
     */
    public Metadata allowedFnfRequests(int allowedFnfRequests) {
      requirePositive(allowedFnfRequests, "allowedFnfRequests");
      this.allowedFnfRequests = allowedFnfRequests;
      return this;
    }

    /**
     * @param serviceCall service call in "service/method" form
     * @param latencyMicros service call p95 latency, micros
     * @return this Metadata instance
     */
    public Metadata serviceCallLatency(String serviceCall, int latencyMicros) {
      requireNotEmpty(serviceCall, "serviceCall");
      requirePositive(latencyMicros, "latencyMicros");
      for (int i = 0; i < serviceCallLatencies.length; i++) {
        if (serviceCallLatencies[i] == 0) {
          serviceCallLatencies[i] = encodeServiceCall(serviceCall, latencyMicros);
          return this;
        }
      }
      throw new IllegalStateException(
          "No more than serviceCallMaxCount() = " + serviceCallMaxCount() + " are allowed");
    }

    Metadata serviceCallLatency(long serviceCallLatency) {
      requirePositive(serviceCallLatency, "serviceCallLatency");
      for (int i = 0; i < serviceCallLatencies.length; i++) {
        if (serviceCallLatencies[i] == 0) {
          serviceCallLatencies[i] = serviceCallLatency;
          return this;
        }
      }
      throw new IllegalStateException(
          "No more than serviceCallMaxCount() = " + serviceCallMaxCount() + " are allowed");
    }

    long[] serviceCallLatencies() {
      return serviceCallLatencies;
    }

    int serviceCallCount() {
      long[] latencies = serviceCallLatencies;
      return latencies[0] == 0 ? 0 : latencies[1] == 0 ? 1 : 2;
    }

    int allowedFnfRequests() {
      return allowedFnfRequests;
    }

    static long encodeServiceCall(String serviceCall, int latency) {
      return (long) serviceCall.hashCode() << 32 | latency;
    }

    static int decodeServiceCall(long serviceCallLatency) {
      return (int) (serviceCallLatency >> 32);
    }

    static int decodeLatency(long serviceCallLatency) {
      return (int) serviceCallLatency;
    }

    static String requireNotEmpty(@Nullable String string, String message) {
      if (string == null || string.isEmpty()) {
        throw new IllegalArgumentException(message + "must be non-empty");
      }
      return string;
    }

    static long requirePositive(long value, String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message + " must be positive");
      }
      return value;
    }
  }

  /** Used to provide request leases to remote peer */
  public interface Controller {

    default void allow(int timeToLiveMillis, int allowedRequests, int rank) {
      allow(timeToLiveMillis, allowedRequests, rank, null);
    }

    default void allow(int timeToLiveMillis, int allowedRequests) {
      allow(timeToLiveMillis, allowedRequests, 0);
    }

    void allow(int timeToLiveMillis, int allowedRequests, int rank, @Nullable Metadata metadata);

    /** @return Eventloop executor */
    ScheduledExecutorService executor();

    Future<Void> onClose();

    /** @return true if associated channel is created as eventloop only, false otherwise */
    boolean isEventLoopOnly();

    /** @return expected keep-alive interval */
    int rttIntervalMillis();
  }

  /**
   * Response stats recorder. Presented to request lease controller for estimation of allowed
   * requests and lease interval
   *
   * @param <T> request name as function of request interaction type and metadata
   */
  public interface StatsRecorder<T> {

    /**
     * Called when new request is received on responder
     *
     * @param requestType interaction type: one of {@link Interaction.Type#FIRE_AND_FORGET}, {@link
     *     Interaction.Type#REQUEST_RESPONSE}, {@link Interaction.Type#REQUEST_STREAM}, {@link
     *     Interaction.Type#REQUEST_CHANNEL}
     * @param metadata request metadata
     * @return logical name of received request. For RPC this may be string of form "service/method"
     */
    T onRequestStarted(Interaction.Type requestType, ByteBuf metadata);

    /**
     * Called on first signal of request handler response - one of ON_NEXT, ON_COMPLETE, ON_ERROR,
     * CANCEL
     *
     * @param requestType interaction type: one of {@link Interaction.Type#FIRE_AND_FORGET}, {@link
     *     Interaction.Type#REQUEST_RESPONSE}, {@link Interaction.Type#REQUEST_STREAM}, {@link
     *     Interaction.Type#REQUEST_CHANNEL}
     * @param request logical name of received request
     * @param firstSignal first signal of response - one of ON_NEXT, ON_COMPLETE, ON_ERROR, CANCEL
     * @param latencyMicros interval between request is received and first response signal is sent.
     *     Is 0 if responder rejected request due to missing lease.
     */
    void onResponseStarted(
        Interaction.Type requestType,
        T request,
        Interaction.StreamSignal firstSignal,
        long latencyMicros);

    /**
     * Called on last signal of request handler response: one of ON_COMPLETE, ON_ERROR, CANCEL
     *
     * @param requestType interaction type: one of {@link Interaction.Type#FIRE_AND_FORGET}, {@link
     *     Interaction.Type#REQUEST_RESPONSE}, {@link Interaction.Type#REQUEST_STREAM}, {@link
     *     Interaction.Type#REQUEST_CHANNEL}
     * @param request logical name of received request
     * @param lastSignal last signal of response - one of ON_COMPLETE, ON_ERROR, CANCEL
     * @param responseDurationMicros interval between request is received and last response signal
     *     is sent. Is 0 if responder rejected request due to missing lease.
     */
    void onResponseTerminated(
        Interaction.Type requestType,
        T request,
        Interaction.StreamSignal lastSignal,
        long responseDurationMicros);

    /** Called on each round-trip time measurement */
    void onRtt(long rttMicros);

    /**
     * Called if stats recorder callbacks throw error
     *
     * @param requestType intercation type
     * @param err error thrown
     */
    void onError(Interaction.Type requestType, Throwable err);

    /** Called after connection is established */
    void onOpen();

    /**
     * Called after connection is terminated
     *
     * @param graceTimeoutMillis grace timeout, 0 if terminated immediately
     */
    void onClose(long graceTimeoutMillis);
  }

  /** Configures requests lease support for endpoint */
  public interface Configurer {

    /**
     * @param leaseController requests lease controller
     * @return optional recorder of response stats and rtt
     */
    Optional<StatsRecorder<?>> configure(Controller leaseController);

    /**
     * server side: option to reject connection; client side: ignored
     *
     * @param setupMessage setup message
     * @param rttMillis client rtt interval, millis
     * @return string denoting connection reject reason. Empty string for accepted connection.
     */
    default String reject(SetupMessage setupMessage, int rttMillis) {
      return "";
    }

    /**
     * @return client side: interval for rtt measurements, 0 means no preferred interval; server
     *     side: ignored
     */
    default int rttIntervalMillis() {
      return 0;
    }
  }
}
