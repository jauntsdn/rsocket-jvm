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

package com.jauntsdn.rsocket.exceptions;

public final class Exceptions {

  private Exceptions() {}

  public static RuntimeException from(int errorCode, String message) {
    switch (errorCode) {
      case ChannelException.ErrorCodes.CONNECTION_ERROR:
        return new ConnectionErrorException(message);
      case ChannelException.ErrorCodes.CONNECTION_CLOSE:
        return new ConnectionCloseException(message);
      case ChannelException.ErrorCodes.INVALID_SETUP:
        return new InvalidSetupException(message);
      case ChannelException.ErrorCodes.UNSUPPORTED_SETUP:
        return new UnsupportedSetupException(message);
      case ChannelException.ErrorCodes.REJECTED_SETUP:
        return new RejectedSetupException(message);
      case ChannelException.ErrorCodes.REJECTED_RESUME:
        return new RejectedResumeException(message);
      case ChannelException.ErrorCodes.REJECTED:
        return new RejectedException(message);
      case ChannelException.ErrorCodes.CANCELED:
        return new CanceledException(message);
      case ChannelException.ErrorCodes.INVALID:
        return new InvalidException(message);
      case ChannelException.ErrorCodes.APPLICATION_ERROR:
        return new ApplicationErrorException(message);
      default:
        return new IllegalArgumentException(
            "Invalid error code: " + errorCode + ", message: " + message);
    }
  }

  public static void throwIfJvmFatal(Throwable t) {
    if (t instanceof Error) {
      throw (Error) t;
    }
  }

  public static boolean isLeaseError(Throwable t) {
    return t == LEASE_EXHAUST_EXCEPTION || t == LEASE_EXPIRE_EXCEPTION;
  }

  public static final String LEASE_EXPIRED_MESSAGE = "lease_expired";
  public static final String LEASE_EXHAUSTED_MESSAGE = "lease_exhausted";

  public static final RejectedException LEASE_EXPIRE_EXCEPTION =
      new RejectedException(LEASE_EXPIRED_MESSAGE, null, true);
  public static final RejectedException LEASE_EXHAUST_EXCEPTION =
      new RejectedException(LEASE_EXHAUSTED_MESSAGE, null, true);
}
