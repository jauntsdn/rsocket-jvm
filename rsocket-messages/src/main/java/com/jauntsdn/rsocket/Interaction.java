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

import javax.annotation.Nullable;

public final class Interaction {
  private static final Interaction[][] INTERACTIONS;

  static {
    Type[] interactionTypes = Type.values();
    int requestCount = interactionTypes.length - 1 /*exclude METADATA_PUSH*/;
    int ranksCount = 4;
    INTERACTIONS = new Interaction[requestCount][ranksCount];
    for (Type interactionType : interactionTypes) {
      if (Type.METADATA_PUSH.equals(interactionType)) {
        continue;
      }
      Interaction[] interaction = INTERACTIONS[interactionType.ordinal() - 1];
      for (int rank = 0; rank < interaction.length; rank++) {
        interaction[rank] = new Interaction(interactionType, rank);
      }
    }
  }

  private final Type type;
  private final int rank;

  private Interaction(Type type, int rank) {
    this.type = type;
    this.rank = rank;
  }

  public static Interaction of(Type type, int rank) {
    requireNonNullRequest(type);
    requireRange(rank, 0, 3);

    return INTERACTIONS[type.ordinal() - 1][rank];
  }

  public Type type() {
    return type;
  }

  public int rank() {
    return rank;
  }

  @Override
  public String toString() {
    return "Interaction{" + "type=" + type + ", rank=" + rank + '}';
  }

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

  private static void requireNonNullRequest(@Nullable Type type) {
    if (type == null) {
      throw new NullPointerException("Interaction.Type must be non-null");
    }
    if (Type.METADATA_PUSH.equals(type)) {
      throw new IllegalArgumentException("Interaction.Type.METADATA_PUSH is not request");
    }
  }

  private static void requireRange(int value, int from, int to) {
    if (value < from || value > to) {
      throw new IllegalArgumentException(
          "Rank must be in range [" + from + "; " + to + "], provided: " + value);
    }
  }
}
