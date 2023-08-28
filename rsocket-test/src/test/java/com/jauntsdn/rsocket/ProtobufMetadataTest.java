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

import com.jauntsdn.rsocket.exceptions.ApplicationErrorException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.rpc.RpcCallMetadata;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProtobufMetadataTest {

  @Test
  void encodeLength() {
    for (int l = 1; l < 8192; l++) {
      String str = "s".repeat(l);
      Headers headers = Headers.create(str, str);
      int lenSize = l <= 127 ? 2 : 3;
      ByteBuf expectedLenBuf = encodeProtobufJava(headers).slice(0, lenSize);
      ByteBuf actualLenBuf = encodeProtobufHeadersLen(str.length());
      try {
        Assertions.assertThat(actualLenBuf.readableBytes()).isEqualTo(lenSize);
        Assertions.assertThat((actualLenBuf)).isEqualTo((expectedLenBuf));
      } finally {
        expectedLenBuf.release();
        actualLenBuf.release();
      }
    }
  }

  @Test
  void encodeSmallHeaders() {
    Headers headers = Headers.create("k", "v", "a", "b");
    ByteBuf expectedBuf = encodeProtobufJava(headers);
    ByteBuf actualBuf = encodeProtobufHeaders(headers);
    try {
      Assertions.assertThat(actualBuf).isEqualTo(expectedBuf);
    } finally {
      expectedBuf.release();
      actualBuf.release();
    }
  }

  @Test
  void encodeLargeHeaders() {
    String k = "k".repeat(300);
    String v = "v".repeat(300);
    String a = "a".repeat(300);
    String b = "b".repeat(300);
    Headers headers = Headers.create(k, v, a, b);
    ByteBuf expectedBuf = encodeProtobufJava(headers);
    ByteBuf actualBuf = encodeProtobufHeaders(headers);
    try {
      Assertions.assertThat(actualBuf).isEqualTo(expectedBuf);
    } finally {
      expectedBuf.release();
      actualBuf.release();
    }
  }

  @Test
  void decodeHeaders() {
    for (int l = 1; l < 8192; l++) {
      String key1 = "k".repeat(l);
      String value1 = "v".repeat(l);
      String key2 = "a".repeat(l);
      String value2 = "b".repeat(l);
      Headers expected = Headers.create(key1, value1, key2, value2);
      ByteBuf metadata = encodeProtobufJava(expected);
      int actualSerializedSize = metadata.readableBytes();
      Headers actual = Rpc.ProtoMetadata.decodeHeaders(metadata);
      try {
        Assertions.assertThat(expected.serializedSize()).isEqualTo(actualSerializedSize);
        Assertions.assertThat(expected.headers()).isEqualTo(actual.headers());
      } finally {
        metadata.release();
      }
    }
  }

  @Test
  void decodeTooLargeHeaders() {
    String key = "k".repeat(42_000);
    String value = "v".repeat(22_000);
    ByteBuf metadata = encodeProtobufJava(Arrays.asList(key, value));
    try {
      org.junit.jupiter.api.Assertions.assertThrows(
          ApplicationErrorException.class,
          () -> {
            Headers headers = Rpc.ProtoMetadata.decodeHeaders(metadata);
          });
    } finally {
      metadata.release();
    }
  }

  @Test
  void decodeEmptyHeaders() {
    Headers actual = Rpc.ProtoMetadata.decodeHeaders(Unpooled.EMPTY_BUFFER);
    Assertions.assertThat(actual).isSameAs(Headers.empty());
  }

  public static ByteBuf encodeProtobufHeadersLen(int len) {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(3);
    Rpc.ProtoMetadata.encodeLen(buffer, len);
    return buffer;
  }

  public static ByteBuf encodeProtobufHeaders(Headers headers) {
    return Rpc.ProtoMetadata.encodeHeaders(headers);
  }

  public static ByteBuf encodeProtobufJava(Headers headers) {
    return encodeProtobufJava(Rpc.ProtoMetadata.getHeaders(headers));
  }

  public static ByteBuf encodeProtobufJava(List<String> headers) {
    RpcCallMetadata message = RpcCallMetadata.newBuilder().addAllNameValues(headers).build();

    int length = message.getSerializedSize();
    ByteBuf content = ByteBufAllocator.DEFAULT.buffer(length, length);
    try {
      message.writeTo(
          com.google.protobuf.CodedOutputStream.newInstance(content.internalNioBuffer(0, length)));
      content.writerIndex(length);
      return content;
    } catch (Throwable t) {
      content.release();
      throw new com.jauntsdn.rsocket.exceptions.SerializationException(
          "Message serialization error", t);
    }
  }
}
