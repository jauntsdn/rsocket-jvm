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

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class Headers {
  private static final Headers EMPTY = new Headers(false, Collections.emptyList(), 0);
  private static final Headers DEFAULT_SERVICE = new Headers(true, Collections.emptyList(), 0);

  private final boolean isDefaultService;
  private final int serializedSize;
  private final List<String> nameValues;
  private volatile ByteBuf cache;

  private Headers(boolean isDefaultService, List<String> nameValues, int serializedSize) {
    this.isDefaultService = isDefaultService;
    this.nameValues = nameValues;
    this.serializedSize = serializedSize;
  }

  public boolean isDefaultService() {
    return isDefaultService;
  }

  public String header(String name) {
    requireNonEmpty(name, " name");
    List<String> nv = nameValues;
    int length = nv.size();
    for (int i = 0; i < length; i += 2) {
      if (name.equals(nv.get(i))) {
        return nv.get(i + 1);
      }
    }
    return null;
  }

  public List<String> headers(String name) {
    requireNonEmpty(name, " name");

    List<String> headers = null;
    List<String> nv = nameValues;
    int length = nv.size();
    for (int i = 0; i < length; i += 2) {
      if (name.equals(nv.get(i))) {
        if (headers == null) {
          headers = new ArrayList<>(2);
        }
        headers.add(nv.get(i + 1));
      }
    }
    return headers == null ? Collections.emptyList() : headers;
  }

  public boolean isEmpty() {
    return nameValues.isEmpty();
  }

  public int size() {
    return nameValues.size() / 2;
  }

  public Iterator<Map.Entry<String, String>> iterator() {
    List<String> nv = nameValues;
    if (nv.isEmpty()) {
      return Collections.emptyIterator();
    }
    return new Iterator<Map.Entry<String, String>>() {
      int cur;

      @Override
      public boolean hasNext() {
        return cur < nv.size();
      }

      @Override
      public Map.Entry<String, String> next() {
        if (cur >= nv.size()) {
          throw new NoSuchElementException();
        }
        final int c = cur;
        cur += 2;
        return new Map.Entry<String, String>() {

          @Override
          public String getKey() {
            return nv.get(c);
          }

          @Override
          public String getValue() {
            return nv.get(c + 1);
          }

          @Override
          public String setValue(String value) {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  @Override
  public String toString() {
    return "Headers{" + "isDefaultService=" + isDefaultService + ", nameValues=" + nameValues + '}';
  }

  public Headers.Builder toBuilder() {
    return new Builder(4, nameValues);
  }

  public Headers.Builder toBuilder(int additionalSize) {
    return new Builder(additionalSize, nameValues);
  }

  public static Headers create(String... headers) {
    return create(false, headers);
  }

  public static Headers create(boolean isDefaultService, String... headers) {
    int serializedSize = requireValid(headers, "headers");
    if (headers.length == 0) {
      return isDefaultService ? DEFAULT_SERVICE : EMPTY;
    }
    return new Headers(isDefaultService, Arrays.asList(headers), serializedSize);
  }

  public static Headers empty() {
    return EMPTY;
  }

  public static Headers withDefaultService() {
    return DEFAULT_SERVICE;
  }

  public static Headers.Builder newBuilder() {
    return new Builder(4, Collections.emptyList());
  }

  public static Headers.Builder newBuilder(int size) {
    return new Builder(size, Collections.emptyList());
  }

  static Headers create(List<String> headers) {
    int serializedSize = requireValid(headers, "headers");
    if (headers.isEmpty()) {
      return EMPTY;
    }
    return new Headers(false, headers, serializedSize);
  }

  ByteBuf cache() {
    return cache;
  }

  void cache(ByteBuf cache) {
    if (cache.readableBytes() > 0) {
      this.cache = cache;
    }
  }

  List<String> headers() {
    return nameValues;
  }

  public int serializedSize() {
    return serializedSize;
  }

  public static final class Builder {
    private final List<String> nameValues;
    private boolean isDefaultService;
    private int serializedSize;

    private Builder(int size, List<String> headers) {
      int length = headers.size();
      List<String> nv = nameValues = new ArrayList<>(2 * size + length);
      for (int i = 0; i < length; i++) {
        String name = headers.get(i);
        String value = headers.get(i + 1);
        nv.add(name);
        nv.add(value);
      }
    }

    public Builder() {
      this(4, Collections.emptyList());
    }

    public Builder defaultService(boolean isDefaultService) {
      this.isDefaultService = isDefaultService;
      return this;
    }

    public Builder add(String name, String value) {
      requireNonEmpty(name, " name");
      requireNonEmpty(value, " value");

      List<String> nv = nameValues;
      nv.add(name);
      nv.add(value);
      serializedSize +=
          Rpc.ProtoMetadata.serializedSize(name) + Rpc.ProtoMetadata.serializedSize(value);
      return this;
    }

    public Builder remove(String name) {
      requireNonEmpty(name, " name");
      List<String> nv = nameValues;
      for (int i = nv.size() - 2; i >= 0; i -= 2) {
        if (name.equals(nv.get(i))) {
          String removedValue = nv.remove(i + 1);
          String removedName = nv.remove(i);
          if (removedValue != null) {
            serializedSize -= Rpc.ProtoMetadata.serializedSize(removedValue);
          }
          if (removedName != null) {
            serializedSize -= Rpc.ProtoMetadata.serializedSize(removedName);
          }
        }
      }
      return this;
    }

    public Builder remove(String name, String value) {
      requireNonEmpty(name, " name");
      requireNonEmpty(value, " value");
      List<String> nv = nameValues;
      for (int i = nv.size() - 2; i >= 0; i -= 2) {
        if (name.equals(nv.get(i)) && value.equals(nv.get(i + 1))) {
          String removedValue = nv.remove(i + 1);
          String removedName = nv.remove(i);
          if (removedValue != null) {
            serializedSize -= Rpc.ProtoMetadata.serializedSize(removedValue);
          }
          if (removedName != null) {
            serializedSize -= Rpc.ProtoMetadata.serializedSize(removedName);
          }
        }
      }
      return this;
    }

    public Headers build() {
      return new Headers(isDefaultService, nameValues, serializedSize);
    }
  }

  private static String requireNonEmpty(String seq, String message) {
    Objects.requireNonNull(seq, message);
    if (seq.length() == 0) {
      throw new IllegalArgumentException(message + " must be non-empty");
    }
    return seq;
  }

  private static int requireValid(List<String> keyValues, String message) {
    Objects.requireNonNull(keyValues, "keyValues");
    int length = keyValues.size();
    if (length % 2 != 0) {
      throw new IllegalArgumentException(message + " size must be even");
    }
    int size = 0;
    for (int i = 0; i < length; i++) {
      String kv = keyValues.get(i);
      if (kv == null) {
        throw new IllegalArgumentException(message + " elements must be non-null");
      }
      boolean isKey = i % 2 == 0;
      if (isKey && kv.isEmpty()) {
        throw new IllegalArgumentException(message + " keys must be non-empty");
      }
      size += Rpc.ProtoMetadata.serializedSize(kv);
    }
    return size;
  }

  private static int requireValid(String[] keyValues, String message) {
    Objects.requireNonNull(keyValues, "keyValues");
    int length = keyValues.length;
    if (length % 2 != 0) {
      throw new IllegalArgumentException(message + " size must be even");
    }
    int size = 0;
    for (int i = 0; i < length; i++) {
      String kv = keyValues[i];
      if (kv == null) {
        throw new IllegalArgumentException(message + " elements must be non-null");
      }
      boolean isKey = i % 2 == 0;
      if (isKey && kv.isEmpty()) {
        throw new IllegalArgumentException(message + " keys must be non-empty");
      }
      size += Rpc.ProtoMetadata.serializedSize(kv);
    }
    return size;
  }
}
