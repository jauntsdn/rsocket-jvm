/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import java.util.function.Function;

/**
 * {@code RSocket} is a full duplex protocol where a client and server are identical in terms of
 * both having the capability to initiate requests to their peer. This interface provides the
 * contract where a client accepts a new {@code RSocket} for sending requests to the peer and
 * returns a new {@code RSocket} that will be used to accept requests from it's peer.
 */
public interface ClientAcceptor {

  /**
   * Accepts a new {@code RSocket} used to send requests to the peer and returns another {@code
   * RSocket} that is used for accepting requests from the peer.
   *
   * @param setup Setup as sent by the client.
   * @param requesterRSocket RSocket used to send requests to the peer.
   * @return RSocket to accept requests from the peer.
   */
  RSocket accept(SetupMessage setup, RSocket requesterRSocket);

  /**
   * Contract to decorate a {@link ClientAcceptor}, providing access to connection {@code setup}
   * information and the ability to also decorate the sockets for requesting and responding.
   *
   * <p>This can be used as an alternative to individual requester and responder {@link
   * RSocket.Interceptor}
   */
  @FunctionalInterface
  interface Interceptor extends Function<ClientAcceptor, ClientAcceptor> {}
}
