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

public final class Interaction {

  private Interaction() {}

  public enum Type {
    METADATA_PUSH,
    FIRE_AND_FORGET,
    REQUEST_RESPONSE,
    REQUEST_STREAM,
    REQUEST_CHANNEL
  }

  public abstract static class StreamSignal {
    private final Type type;

    StreamSignal(Type type) {
      this.type = type;
    }

    public Type type() {
      return type;
    }

    @SuppressWarnings("unchecked")
    public <T extends StreamSignal> T cast() {
      return (T) this;
    }

    public enum Type {
      ON_NEXT,
      ON_COMPLETE,
      ON_ERROR,
      CANCEL
    }

    public static final class Next extends StreamSignal {
      static final StreamSignal INSTANCE = new Next();

      private Next() {
        super(Type.ON_NEXT);
      }
    }

    public static final class Complete extends StreamSignal {
      static final StreamSignal INSTANCE = new Complete();

      private Complete() {
        super(Type.ON_COMPLETE);
      }
    }

    public static final class Cancel extends StreamSignal {
      static final StreamSignal INSTANCE = new Cancel();

      private Cancel() {
        super(Type.CANCEL);
      }
    }

    public static final class Error extends StreamSignal {
      private final Throwable error;

      static Error create(Throwable error) {
        return new Error(error);
      }

      private Error(Throwable error) {
        super(Type.ON_ERROR);
        this.error = error;
      }

      public Throwable value() {
        return error;
      }
    }
  }
}
