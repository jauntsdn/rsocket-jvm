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

import javax.annotation.Nullable;

public final class RejectedException extends ChannelException {
  private static final long serialVersionUID = 5570698024416426891L;

  public RejectedException(String message) {
    super(message);
  }

  public RejectedException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  RejectedException(String message, @Nullable Throwable cause, boolean isLightWeight) {
    super(message, cause, isLightWeight);
  }

  @Override
  public int errorCode() {
    return ErrorCodes.REJECTED;
  }
}
