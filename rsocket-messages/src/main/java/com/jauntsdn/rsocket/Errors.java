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

import com.jauntsdn.rsocket.exceptions.ChannelException;
import javax.annotation.Nullable;

public final class Errors {
  private Errors() {}

  public static final class Configurer {
    Connection.SendErrors connectionSendErrors;
    Connection.ReceiveErrors connectionReceiveErrors;
    Stream.SendErrors streamSendErrors;
    Stream.ReceiveErrors streamReceiveErrors;

    Configurer() {}

    public Configurer connectionSendErrors(Connection.SendErrors connectionSendErrors) {
      this.connectionSendErrors = connectionSendErrors;
      return this;
    }

    public Configurer connectionReceiveErrors(Connection.ReceiveErrors connectionReceiveErrors) {
      this.connectionReceiveErrors = connectionReceiveErrors;
      return this;
    }

    public Configurer streamSendErrors(Stream.SendErrors streamSendErrors) {
      this.streamSendErrors = streamSendErrors;
      return this;
    }

    public Configurer streamReceiveErrors(Stream.ReceiveErrors streamReceiveErrors) {
      this.streamReceiveErrors = streamReceiveErrors;
      return this;
    }
  }

  public static final class Connection {

    private Connection() {}

    public interface SendErrors {

      String translate(int errorCode, @Nullable String errorMessage);
    }

    public interface ReceiveErrors {

      Exception translate(int errorCode, @Nullable String errorMessage);
    }
  }

  public static final class Stream {

    private Stream() {}

    public enum StreamType {
      REQUEST,
      RESPONSE
    }

    /** Converts stream {@link Throwable} error to RSocket error code and message */
    public interface SendErrors {

      /**
       * @param streamType type of stream: request or response
       * @param t {@link Throwable} that should be converted to {@link Error}
       * @return one of stream errors (code and message): reject, cancel, invalid, application. Null
       *     if default conversion should be applied.
       */
      @Nullable
      Error translate(StreamType streamType, Throwable t);

      /** Represents one of stream errors: reject, cancel, invalid, application */
      final class Error {
        private final int errorCode;
        private final String message;

        private Error(int errorCode, String message) {
          this.errorCode = errorCode;
          this.message = message;
        }

        public static Error reject(String errorMessage) {
          return new Error(ChannelException.ErrorCodes.REJECTED, errorMessage);
        }

        public static Error cancel(String errorMessage) {
          return new Error(ChannelException.ErrorCodes.CANCELED, errorMessage);
        }

        public static Error invalid(String errorMessage) {
          return new Error(ChannelException.ErrorCodes.INVALID, errorMessage);
        }

        public static Error application(String errorMessage) {
          return new Error(ChannelException.ErrorCodes.APPLICATION_ERROR, errorMessage);
        }

        public String message() {
          return message;
        }

        public int code() {
          return errorCode;
        }
      }
    }

    /**
     * Converts RSocket error code {@link ErrorType} and message to stream {@link Throwable} error
     */
    public interface ReceiveErrors {

      /**
       * @param streamType type of stream: request or response
       * @param errorType one of stream errors: reject, cancel, invalid, application
       * @param errorMessage stream error message
       * @return stream Throwable converted from error code and error message. Null if default
       *     conversion should be applied
       */
      @Nullable
      Throwable translate(StreamType streamType, ErrorType errorType, String errorMessage);

      enum ErrorType {
        REJECTED,
        CANCELED,
        INVALID,
        APPLICATION;

        static ErrorType fromCode(int code) {
          switch (code) {
            case ChannelException.ErrorCodes.REJECTED:
              return REJECTED;
            case ChannelException.ErrorCodes.CANCELED:
              return CANCELED;
            case ChannelException.ErrorCodes.INVALID:
              return INVALID;
            default:
              return APPLICATION;
          }
        }
      }
    }
  }
}
