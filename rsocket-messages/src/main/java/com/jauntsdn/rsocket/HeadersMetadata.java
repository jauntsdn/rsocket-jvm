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
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.IntCollections;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.annotation.Nullable;

public final class HeadersMetadata {
  static final int PREFIX_FLAGS_OFFSET = 3;
  static final int PREFIX_NAME_LENGTH_OFFSET = 2;
  static final int FLAG_PREFIX_FOLLOWS = 0b0000_0001;
  static final int FLAG_PREFIX_HEADERS = 0b0000_0010;

  static final int HEADER_NAME_MAX_LENGTH = Byte.MAX_VALUE - Byte.MIN_VALUE;
  static final int HEADER_VALUE_MAX_LENGTH = Short.MAX_VALUE - Short.MIN_VALUE;

  /*
   *
   *
   * [8] FLAGS
   * FLAGS, first message: F - header follows, H - has headers
   *         next message: F - header follows
   * [8] HEADER NAME LENGTH
   * [16] HEADER VALUE LENGTH
   *
   * [HEADER NAME LENGTH] HEADER NAME
   * [HEADER VALUE LENGTH] HEADER VALUE
   */

  private HeadersMetadata() {}

  public static ByteBuf encode(List<? extends CharSequence> headerNameValue) {
    return encode(ByteBufAllocator.DEFAULT, headerNameValue);
  }

  public static ByteBuf encode(
      ByteBufAllocator allocator, List<? extends CharSequence> headerNameValues) {
    Objects.requireNonNull(headerNameValues, "headerNameValues");
    Objects.requireNonNull(allocator, "allocator");
    int nameValuesCount = headerNameValues.size();
    if (nameValuesCount == 0) {
      return Unpooled.EMPTY_BUFFER;
    }
    ByteBuf buffer = allocator.buffer(sizeOf(headerNameValues));
    return encodeHeaders(buffer, headerNameValues);
  }

  public static ByteBuf encode(ByteBuf buffer, List<? extends CharSequence> headerNameValues) {
    Objects.requireNonNull(headerNameValues, "headerNameValues");
    Objects.requireNonNull(buffer, "buffer");
    int nameValuesCount = headerNameValues.size();
    if (nameValuesCount == 0) {
      return Unpooled.EMPTY_BUFFER;
    }
    if (nameValuesCount % 2 != 0) {
      throw new IllegalArgumentException("headerNameValues count must be even: " + nameValuesCount);
    }
    return encodeHeaders(buffer, headerNameValues);
  }

  public static int sizeOf(List<? extends CharSequence> headerNameValues) {
    Objects.requireNonNull(headerNameValues, "headerNameValues");
    int nameValuesCount = headerNameValues.size();
    if (nameValuesCount % 2 != 0) {
      throw new IllegalArgumentException("headerNameValues count must be even: " + nameValuesCount);
    }
    long size = 0;
    for (int i = 0; i < nameValuesCount; i += 2) {
      CharSequence name = headerNameValues.get(i);
      CharSequence value = headerNameValues.get(i + 1);
      if (name == null || value == null) {
        throw new NullPointerException("headerNameValues contents");
      }
      int nameLength = name.length();
      if (nameLength == 0) {
        throw new IllegalArgumentException("header name must be non-empty");
      }
      if (nameLength > HEADER_NAME_MAX_LENGTH) {
        throw new IllegalArgumentException(
            "header name max length exceeded: " + nameLength + " > " + HEADER_NAME_MAX_LENGTH);
      }
      int valueLength = value.length();
      if (valueLength == 0) {
        throw new IllegalArgumentException("header value must be non-empty");
      }
      if (valueLength > HEADER_VALUE_MAX_LENGTH) {
        throw new IllegalArgumentException(
            "header value max length exceeded: " + nameLength + " > " + HEADER_VALUE_MAX_LENGTH);
      }
      size += Integer.BYTES + nameLength + valueLength;
    }
    if (size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Length exceeds Integer.MAX_VALUE");
    }
    return (int) size;
  }

  public static Iterator<String> decode(ByteBuf metadata) {
    return decode(metadata, Projection.ALL);
  }

  public static Iterator<String> decode(ByteBuf metadata, Projection projection) {
    int prefix = metadata.getInt(metadata.readerIndex());
    int flags = prefix >> (PREFIX_FLAGS_OFFSET * 8);
    if ((flags & FLAG_PREFIX_HEADERS) == FLAG_PREFIX_HEADERS) {
      return new StringMetadataIterator(metadata, projection);
    }
    return Collections.emptyIterator();
  }

  public static Iterator<AsciiString> decodeAscii(ByteBuf metadata) {
    return decodeAscii(metadata, Projection.ALL);
  }

  public static Iterator<AsciiString> decodeAscii(ByteBuf metadata, Projection projection) {
    int prefix = metadata.getInt(metadata.readerIndex());
    int flags = prefix >> (PREFIX_FLAGS_OFFSET * 8);
    if ((flags & FLAG_PREFIX_HEADERS) == FLAG_PREFIX_HEADERS) {
      return new AsciiMetadataIterator(metadata, projection);
    }
    return Collections.emptyIterator();
  }

  static ByteBuf encodeHeaders(ByteBuf buffer, List<? extends CharSequence> headerNameValues) {
    int nameValuesCount = headerNameValues.size();
    for (int i = 0; i < nameValuesCount; i += 2) {
      CharSequence name = headerNameValues.get(i);
      int valueIndex = i + 1;
      CharSequence value = headerNameValues.get(valueIndex);
      boolean isFirst = i == 0;
      boolean isLast = valueIndex == nameValuesCount - 1;
      int headers = 0;
      if (isFirst) {
        headers |= FLAG_PREFIX_HEADERS;
      }
      if (!isLast) {
        headers |= FLAG_PREFIX_FOLLOWS;
      }
      buffer.writeByte(headers);
      buffer.writeByte(name.length());
      buffer.writeShort(value.length());
      buffer.writeCharSequence(name, StandardCharsets.US_ASCII);
      buffer.writeCharSequence(value, StandardCharsets.US_ASCII);
    }
    return buffer;
  }

  static final class AsciiMetadataIterator extends MetadataIterator
      implements Iterator<AsciiString> {

    AsciiMetadataIterator(ByteBuf metadata, Projection projection) {
      super(metadata, projection);
    }

    @Override
    public AsciiString next() {
      return nextAscii();
    }
  }

  static final class StringMetadataIterator extends MetadataIterator implements Iterator<String> {

    StringMetadataIterator(ByteBuf metadata, Projection projection) {
      super(metadata, projection);
    }

    @Override
    public String next() {
      return nextAscii().toString();
    }
  }

  static class MetadataIterator {
    private static final int STATE_FINISHED = 0b0001;
    private static final int STATE_RELEASED = 0b0010;

    private final ByteBuf metadata;
    private final Projection projection;
    private int state;

    private AsciiString curName;
    private AsciiString curValue;

    MetadataIterator(ByteBuf metadata, Projection projection) {
      this.metadata = metadata;
      this.projection = projection;
    }

    public boolean hasNext() {
      if (curName != null || curValue != null) {
        return true;
      }
      if ((state & STATE_FINISHED) == STATE_FINISHED) {
        return false;
      }
      return nextHeader();
    }

    AsciiString nextAscii() {
      AsciiString n = curName;
      if (n != null) {
        curName = null;
        return n;
      }
      AsciiString v = curValue;
      if (v != null) {
        curValue = null;
        return v;
      }
      if ((state & STATE_FINISHED) == STATE_FINISHED) {
        releaseOnce(metadata);
        throw new NoSuchElementException("No more elements");
      }

      if (nextHeader()) {
        AsciiString nextName = curName;
        curName = null;
        return nextName;
      }
      releaseOnce(metadata);
      throw new NoSuchElementException("No more elements");
    }

    boolean nextHeader() {
      ByteBuf m = metadata;
      /*read next key-value pair*/
      boolean follows;
      do {
        int readableBytes = m.readableBytes();
        if (readableBytes < Integer.BYTES) {
          releaseOnce(m);
          throw new IllegalStateException(
              "Unexpected metadata layout: available bytes is less than prefix size: "
                  + readableBytes);
        }
        int prefix = m.readInt();
        int flags = prefix >> (PREFIX_FLAGS_OFFSET * 8);
        follows = (flags & FLAG_PREFIX_FOLLOWS) == FLAG_PREFIX_FOLLOWS;
        int nameLength = (prefix >> (PREFIX_NAME_LENGTH_OFFSET * 8)) & 0x0F;
        int valueLength = prefix & 0xFF;
        int headerLength = nameLength + valueLength;
        if (m.readableBytes() < headerLength) {
          releaseOnce(metadata);
          throw new IllegalStateException(
              "Unexpected metadata layout: header length : "
                  + headerLength
                  + " exceeds readable bytes: "
                  + m.readableBytes());
        }
        if (!follows) {
          state |= STATE_FINISHED;
        }
        IntObjectMap<List<AsciiString>> namesProjection = projection.headerNames();
        if (Projection.isAll(namesProjection)) {
          curName = (AsciiString) m.readCharSequence(nameLength, StandardCharsets.US_ASCII);
          curValue = (AsciiString) m.readCharSequence(valueLength, StandardCharsets.US_ASCII);
          return true;
        } else if (Projection.isNone(namesProjection)) {
          m.skipBytes(nameLength + valueLength);
        } else {
          List<AsciiString> acceptedNames = namesProjection.get(nameLength);
          if (acceptedNames != null) {
            AsciiString name =
                (AsciiString) m.readCharSequence(nameLength, StandardCharsets.US_ASCII);
            if (acceptedNames.contains(name)) {
              curName = name;
              curValue = (AsciiString) m.readCharSequence(valueLength, StandardCharsets.US_ASCII);
              return true;
            } else {
              m.skipBytes(valueLength);
            }
          } else {
            m.skipBytes(headerLength);
          }
        }
      } while (follows);
      return false;
    }

    void releaseOnce(ByteBuf metadata) {
      if ((state & STATE_RELEASED) != STATE_RELEASED) {
        state |= STATE_RELEASED;
        ReferenceCountUtil.safeRelease(metadata);
      }
    }
  }

  public static final class Projection {
    private static final Projection NONE = new Projection(IntCollections.emptyMap());
    private static final Projection ALL = new Projection(null);
    private final IntObjectMap<List<AsciiString>> sizeToHeaderNames;

    private Projection(@Nullable IntObjectMap<List<AsciiString>> sizeToHeaderNames) {
      this.sizeToHeaderNames = sizeToHeaderNames;
    }

    public static Projection all() {
      return ALL;
    }

    public static Projection none() {
      return NONE;
    }

    public static Projection of(List<? extends CharSequence> headerNames) {
      Objects.requireNonNull(headerNames, "headerNames");
      int headerNamesSize = headerNames.size();
      switch (headerNamesSize) {
        case 0:
          {
            return NONE;
          }
        case 1:
          {
            IntObjectHashMap<List<AsciiString>> sizeToHeaderNames = new IntObjectHashMap<>(1);
            CharSequence headerName = headerNames.get(0);
            sizeToHeaderNames.put(
                headerName.length(), Collections.singletonList(AsciiString.of(headerName)));
            return new Projection(sizeToHeaderNames);
          }
        default:
          {
            IntObjectHashMap<List<AsciiString>> sizeToHeaderNames =
                new IntObjectHashMap<>(headerNamesSize);
            for (int i = 0; i < headerNamesSize; i++) {
              CharSequence name = headerNames.get(i);
              if (name == null) {
                throw new IllegalArgumentException("headerName");
              }
              AsciiString asciiName = AsciiString.of(name);
              int nameLength = asciiName.length();
              List<AsciiString> headers = sizeToHeaderNames.get(nameLength);
              if (headers == null) {
                headers = new ArrayList<>(1);
                headers.add(asciiName);
                sizeToHeaderNames.put(nameLength, headers);
              } else {
                if (!headers.contains(asciiName)) {
                  headers.add(asciiName);
                }
              }
            }
            return new Projection(sizeToHeaderNames);
          }
      }
    }

    static boolean isAll(@Nullable IntObjectMap<List<AsciiString>> sizeToHeaderNames) {
      return sizeToHeaderNames == null;
    }

    static boolean isNone(@Nullable IntObjectMap<List<AsciiString>> sizeToHeaderNames) {
      return sizeToHeaderNames != null && sizeToHeaderNames.isEmpty();
    }

    /*
     * null -> all
     * empty -> none
     * otherwise accept projection
     **/
    @Nullable
    IntObjectMap<List<AsciiString>> headerNames() {
      return sizeToHeaderNames;
    }
  }
}
