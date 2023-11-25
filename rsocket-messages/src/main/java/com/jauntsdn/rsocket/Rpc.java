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

import com.google.protobuf.CodedInputStream;
import com.jauntsdn.rsocket.exceptions.ApplicationErrorException;
import com.jauntsdn.rsocket.exceptions.SerializationException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Rpc {

  private Rpc() {}

  @Target(ElementType.ANNOTATION_TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Annotation {}

  @Annotation
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Generated {

    Role role();

    Class<?> service();
  }

  @Annotation
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface GeneratedMethod {

    Class<?> returnType();
  }

  public enum Role {
    CLIENT,
    SERVICE
  }

  public interface InstrumentationListener {

    void doOnSubscribe();

    void doOnNext();

    void doOnComplete();

    void doOnError(Throwable t);

    void doOnCancel();
  }

  public interface Codec {

    ByteBuf encodeContent(
        ByteBufAllocator allocator,
        ByteBuf metadata,
        int localHeader,
        String service,
        String method,
        boolean hasRequestN,
        boolean isIdempotent,
        int dataSize,
        int externalMetadataSize);

    default ByteBuf encodeContent(
        ByteBufAllocator allocator,
        ByteBuf metadata,
        String service,
        String method,
        boolean hasRequestN,
        boolean isIdempotent,
        int dataSize,
        int externalMetadataSize) {
      return encodeContent(
          allocator,
          metadata,
          -1,
          service,
          method,
          hasRequestN,
          isIdempotent,
          dataSize,
          externalMetadataSize);
    }

    Message encodeMessage(ByteBuf content, int rank);

    ByteBuf encodeContent(ByteBufAllocator allocator, int dataSize);

    Message encodeMessage(ByteBuf content);

    String decodeMessageService(ByteBuf metadata, long header, int flags);

    String decodeMessageMethod(ByteBuf metadata, long header, int flags);

    boolean isDisposable();

    void dispose();
  }

  /*
   *
   * HEADER
   *
   * [8] VERSION
   * [8] FLAGS: COMPACT ENCODING, DEFAULT SERVICE, FOREIGN_CALL, TRACE METADATA
   * [8] SERVICE LENGTH ? (!DEFAULT SERVICE)
   * [8] METHOD LENGTH
   * [16] TRACE LENGTH ? (TRACE_METADATA)
   *
   * PAYLOAD
   *
   * [SERVICE LENGTH] SERVICE ? (!DEFAULT SERVICE)
   * [METHOD LENGTH] METHOD
   * [TRACE LENGTH] TRACE ? (TRACE_METADATA)
   * METADATA
   */
  public static final class RpcMetadata {

    private RpcMetadata() {}

    /*version*/
    public static final byte VERSION = 1;
    static final long MASK_VERSION = 0xFF00_0000_0000_0000L;
    static final long MASK_VERSION_SHIFT = 56;
    /*flags*/
    static final long MASK_FLAGS = 0x00FF_0000_0000_0000L;
    static final long MASK_FLAGS_SHIFT = 48;
    static final int FLAG_COMPACT = 0b1000_0000;
    static final int FLAG_DEFAULT_SERVICE = 0b0100_0000;
    static final int FLAG_FOREIGN_CALL = 0b0010_0000;
    static final int FLAG_IDEMPOTENT_CALL = 0b0001_0000;
    static final int FLAG_TRACE = 0b0000_1000;

    /*service*/
    static final long MASK_SERVICE_LENGTH = 0x0000_FF00_0000_0000L;
    static final int MASK_SERVICE_SHIFT = 40;

    /*method*/
    static final long MASK_METHOD_DEFAULT_SERVICE_LENGTH = MASK_SERVICE_LENGTH;
    static final int MASK_METHOD_DEFAULT_SERVICE_SHIFT = MASK_SERVICE_SHIFT;
    static final long MASK_METHOD_LENGTH = 0x0000_00FF_0000_0000L;
    static final int MASK_METHOD_SHIFT = 32;

    public static long header(ByteBuf rpcMetadata) {
      return rpcMetadata.getLong(0);
    }

    public static int version(long header) {
      return (int) ((header & MASK_VERSION) >> MASK_VERSION_SHIFT);
    }

    public static int flags(long header) {
      return (int) ((header & MASK_FLAGS) >> MASK_FLAGS_SHIFT);
    }

    public static boolean flagForeignCall(int flags) {
      return (flags & FLAG_FOREIGN_CALL) == FLAG_FOREIGN_CALL;
    }

    public static boolean flagIdempotentCall(int flags) {
      return (flags & FLAG_IDEMPOTENT_CALL) == FLAG_IDEMPOTENT_CALL;
    }

    public static String service(ByteBuf metadata, long header, int flags) {
      if (!((flags & FLAG_COMPACT) == FLAG_COMPACT)) {
        throw new IllegalArgumentException("RSocket-RPC default encoding is not implemented");
      }

      boolean defaultService = (flags & FLAG_DEFAULT_SERVICE) == FLAG_DEFAULT_SERVICE;
      if (defaultService) {
        return "";
      }
      boolean tracing = (flags & FLAG_TRACE) == FLAG_TRACE;
      int serviceOffset = 4;
      if (tracing) {
        serviceOffset += Short.BYTES;
      }
      int serviceLength = (int) ((header & MASK_SERVICE_LENGTH) >> MASK_SERVICE_SHIFT);
      return metadata.toString(serviceOffset, serviceLength, StandardCharsets.US_ASCII);
    }

    public static String defaultService() {
      return "";
    }

    public static String method(ByteBuf metadata, long header, int flags) {
      if (!((flags & FLAG_COMPACT) == FLAG_COMPACT)) {
        throw new IllegalArgumentException("RSocket-RPC default encoding is not implemented");
      }

      boolean defaultService =
          (flags & RpcMetadata.FLAG_DEFAULT_SERVICE) == RpcMetadata.FLAG_DEFAULT_SERVICE;
      boolean tracing = (flags & RpcMetadata.FLAG_TRACE) == RpcMetadata.FLAG_TRACE;
      int methodOffset = 3;
      if (!defaultService) {
        int serviceLength = (int) ((header & MASK_SERVICE_LENGTH) >> MASK_SERVICE_SHIFT);
        methodOffset += Byte.BYTES + serviceLength;
      }
      if (tracing) {
        methodOffset += Short.BYTES;
      }
      int methodLength =
          defaultService
              ? (int)
                  ((header & MASK_METHOD_DEFAULT_SERVICE_LENGTH)
                      >> MASK_METHOD_DEFAULT_SERVICE_SHIFT)
              : (int) ((header & MASK_METHOD_LENGTH) >> MASK_METHOD_SHIFT);

      return metadata.toString(methodOffset, methodLength, StandardCharsets.US_ASCII);
    }

    /*
     *
     *   * HEADER
     *
     * [8] VERSION
     * [8] FLAGS: COMPACT ENCODING, DEFAULT SERVICE, FOREIGN_CALL, TRACE METADATA
     * [8] SERVICE LENGTH ? (!DEFAULT SERVICE)
     * [8] METHOD LENGTH
     * [16] TRACE LENGTH ? (TRACE_METADATA)
     * */
    public static String serviceMethod(
        int flags, long header, ByteBuf rSocketRpcMetadata, List<String> serviceMethods) {
      if (!((flags & FLAG_COMPACT) == FLAG_COMPACT)) {
        throw new IllegalArgumentException("RSocket-RPC default encoding is not implemented");
      }

      if (serviceMethods.isEmpty()) {
        return null;
      }
      boolean defaultService =
          (flags & RpcMetadata.FLAG_DEFAULT_SERVICE) == RpcMetadata.FLAG_DEFAULT_SERVICE;
      boolean hasTrace = (flags & RpcMetadata.FLAG_TRACE) == RpcMetadata.FLAG_TRACE;
      if (defaultService) {
        int methodLengthOffset = 2;
        int methodLength = rSocketRpcMetadata.getByte(methodLengthOffset);
        /*method length, trace length*/
        int methodOffset =
            hasTrace
                ? methodLengthOffset + Byte.BYTES + Short.BYTES
                : methodLengthOffset + Byte.BYTES;
        for (int i = 0; i < serviceMethods.size(); i++) {
          String serviceMethod = serviceMethods.get(i);
          if (serviceMethod.length() == methodLength) {
            boolean isEqual = true;
            for (int j = 0; j < methodLength; j++) {
              if (rSocketRpcMetadata.getByte(methodOffset + j) != serviceMethod.charAt(j)) {
                isEqual = false;
                break;
              }
            }
            if (isEqual) {
              return serviceMethod;
            }
          }
        }
        return null;
      }
      /*non-default service*/
      int serviceLengthOffset = 2;
      int methodLengthOffset = 3;
      int serviceLength = rSocketRpcMetadata.getByte(serviceLengthOffset);
      int methodLength = rSocketRpcMetadata.getByte(methodLengthOffset);
      int serviceMethodLength = serviceLength + methodLength;

      /*method length, trace length*/
      int serviceMethodOffset =
          hasTrace
              ? methodLengthOffset + Byte.BYTES + Short.BYTES
              : methodLengthOffset + Byte.BYTES;

      for (int i = 0; i < serviceLength; i++) {
        String serviceMethod = serviceMethods.get(i);
        if (serviceMethod.length() == serviceMethodLength) {
          boolean isEqual = true;
          for (int j = 0; j < serviceMethodLength; j++) {
            if (rSocketRpcMetadata.getByte(serviceMethodOffset + j) != serviceMethod.charAt(j)) {
              isEqual = false;
              break;
            }
          }
          if (isEqual) {
            return serviceMethod;
          }
        }
      }
      return null;
    }
  }

  public static final class ProtoMetadata {

    private ProtoMetadata() {}

    public static Headers createHeaders(List<String> headers) {
      return Headers.create(headers);
    }

    public static List<String> getHeaders(Headers headers) {
      return headers.headers();
    }

    public static ByteBuf getCache(Headers headers) {
      return headers.cache();
    }

    public static void setCache(Headers headers, ByteBuf cache) {
      headers.cache(cache);
    }

    private static final int LEN_TAG = /*field*/ 1 << 3 | /*wire type LEN*/ 2;
    private static final int VARINT_BYTE_MAX = 128;

    public static ByteBuf encodeHeaders(Headers headers) {
      Objects.requireNonNull(headers, "headers");
      if (headers.isEmpty()) {
        return Unpooled.EMPTY_BUFFER;
      }
      ByteBuf cache = headers.cache();
      if (cache != null) {
        return cache;
      }
      int serializedSize = headers.serializedSize();
      ByteBuf byteBuf =
          new UnpooledHeapByteBuf(UnpooledByteBufAllocator.DEFAULT, serializedSize, serializedSize);

      List<String> asciiHeaders = headers.headers();
      for (int i = 0; i < asciiHeaders.size(); i++) {
        String asciiHeader = asciiHeaders.get(i);
        encodeLen(byteBuf, asciiHeader.length());
        ByteBufUtil.writeAscii(byteBuf, asciiHeader);
      }
      headers.cache(byteBuf);
      return byteBuf;
    }

    static void encodeLen(ByteBuf byteBuf, int len) {
      if (len < VARINT_BYTE_MAX) {
        byteBuf.writeShort(LEN_TAG << 8 | len);
      } else {
        byteBuf.writeByte(LEN_TAG);
        int varintLen = (len & 0x7F | /*cont bit*/ 0x80) << 8 | ((len >> 7) & 0x7F);
        byteBuf.writeShort(varintLen);
      }
    }

    static int serializedSize(String asciiString) {
      int headerLength = asciiString.length();
      return headerLength < VARINT_BYTE_MAX ? headerLength + 2 : headerLength + 3;
    }

    public static Headers decodeHeaders(ByteBuf metadata) {
      Objects.requireNonNull(metadata, "metadata");
      if (metadata.readableBytes() == 0) {
        return Headers.empty();
      }
      List<String> headers = null;
      int remaining = metadata.readableBytes();
      do {
        if (remaining < 2) {
          throw new ApplicationErrorException("unexpected metadata structure");
        }
        remaining -= Short.BYTES;
        short tagLenStart = metadata.readShort();
        int tag = tagLenStart >> 8;
        if (tag != LEN_TAG) {
          throw new ApplicationErrorException("unexpected protobuf metadata message tag: " + tag);
        }
        int lenStart = tagLenStart & 0xFF;
        int len;
        if ((lenStart & /*cont bit*/ 0x80) == 0) {
          len = lenStart & 0x7F;
        } else {
          remaining -= Byte.BYTES;
          byte lenEnd = metadata.readByte();
          if ((lenEnd & /*cont bit*/ 0x80) != 0) {
            throw new ApplicationErrorException(
                "unexpected protobuf metadata header length, exceeds " + Headers.HEADER_LENGTH_MAX);
          }
          len = lenStart & 0x7F | lenEnd << 7;
          if (len > Headers.HEADER_LENGTH_MAX) {
            throw new ApplicationErrorException(
                "unexpected protobuf metadata header length, exceeds "
                    + Headers.HEADER_LENGTH_MAX
                    + ": "
                    + len);
          }
        }
        if (headers == null) {
          headers = new ArrayList<>(4);
        }
        remaining -= len;
        headers.add(metadata.readCharSequence(len, StandardCharsets.US_ASCII).toString());
      } while (remaining > 0);
      return Headers.create(headers);
    }
  }

  /**
   * Service descriptor used to transcode RPC / Protocol Buffers calls into another representation
   * (e.g. http/json)
   */
  public static class ServiceDescriptor {
    private final List<Call> serviceCalls;

    public ServiceDescriptor(List<Call> serviceCalls) {
      this.serviceCalls = Objects.requireNonNull(serviceCalls, "serviceCalls");
    }

    public final List<Call> serviceCalls() {
      return serviceCalls;
    }

    @Override
    public String toString() {
      return "ServiceDescriptor{" + "serviceCalls=" + serviceCalls + '}';
    }

    public static class Call {
      final String name;
      final InboundMessageFactory inMessageFactory;
      final OutboundMessageFactory outMessageFactory;

      private Call(
          String name,
          InboundMessageFactory inMessageFactory,
          OutboundMessageFactory outMessageFactory) {
        this.name = name;
        this.inMessageFactory = inMessageFactory;
        this.outMessageFactory = outMessageFactory;
      }

      @Override
      public String toString() {
        return "Call{" + "name='" + name + '\'' + '}';
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Call call = (Call) o;

        return name.equals(call.name);
      }

      @Override
      public int hashCode() {
        return name.hashCode();
      }

      public static Call of(
          String name,
          InboundMessageFactory inMessageFactory,
          OutboundMessageFactory outMessageFactory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(inMessageFactory, "inMessageFactory");
        Objects.requireNonNull(outMessageFactory, "outMessageFactory");
        return new Call(name, inMessageFactory, outMessageFactory);
      }

      public static Call of(
          String service,
          String method,
          InboundMessageFactory inMessageFactory,
          OutboundMessageFactory outMessageFactory) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(inMessageFactory, "inMessageFactory");
        Objects.requireNonNull(outMessageFactory, "outMessageFactory");
        return new Call('/' + service + '/' + method, inMessageFactory, outMessageFactory);
      }
    }

    public interface InboundMessageFactory extends Supplier<com.google.protobuf.Message.Builder> {}

    public interface OutboundMessageFactory
        extends Function<CodedInputStream, com.google.protobuf.Message> {

      @Override
      default com.google.protobuf.Message apply(CodedInputStream codedInputStream) {
        try {
          return create(codedInputStream);
        } catch (IOException e) {
          throw new SerializationException("Protobuf deserialization error", e);
        }
      }

      com.google.protobuf.Message create(CodedInputStream codedInputStream) throws IOException;
    }
  }
}
