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

import java.util.Objects;
import javax.annotation.Nullable;

public abstract class ChannelException extends RuntimeException {

  public ChannelException(String message) {
    this(message, null);
  }

  public ChannelException(String message, @Nullable Throwable cause) {
    this(message, cause, false);
  }

  ChannelException(String message, @Nullable Throwable cause, boolean isLightWeight) {
    super(
        Objects.requireNonNull(message, "message must not be null"),
        cause,
        !isLightWeight,
        !isLightWeight);
  }

  public abstract int errorCode();

  public static final class ErrorCodes {
    private ErrorCodes() {}

    public static final int CONNECTION_ERROR = 0x00000101;
    public static final int CONNECTION_CLOSE = 0x00000102;
    public static final int INVALID_SETUP = 0x00000001;
    public static final int UNSUPPORTED_SETUP = 0x00000002;
    public static final int REJECTED_SETUP = 0x00000003;
    public static final int REJECTED_RESUME = 0x00000004;
    public static final int REJECTED = 0x00000202;
    public static final int CANCELED = 0x00000203;
    public static final int INVALID = 0x00000204;
    public static final int APPLICATION_ERROR = 0x00000201;
  }
}
