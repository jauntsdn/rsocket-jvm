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
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.util.Objects;
import java.util.function.Consumer;

public final class MessageMetadata {
  public static final int HEADER_SIZE = Long.BYTES;

  private final ByteBufAllocator allocator;
  private int metadataSize;
  private boolean isDefaultService;
  private Consumer<ByteBuf> metadataWriter;

  private MessageMetadata(ByteBufAllocator allocator) {
    this.allocator = Objects.requireNonNull(allocator, "allocator");
  }

  public static int header(ByteBuf metadata) {
    return MessageMetadataFlyweight.header(metadata);
  }

  public static boolean defaultService(int header) {
    return MessageMetadataFlyweight.defaultService(header);
  }

  public static MessageMetadata allocator(ByteBufAllocator allocator) {
    return new MessageMetadata(allocator);
  }

  public static MessageMetadata defaultAllocator() {
    return new MessageMetadata(ByteBufAllocator.DEFAULT);
  }

  public static MessageMetadata heapAllocator() {
    return new MessageMetadata(UnpooledHeapByteBufAllocator.DEFAULT);
  }

  public MessageMetadata defaultService(boolean defaultService) {
    this.isDefaultService = defaultService;
    return this;
  }

  public MessageMetadata metadataSize(int size) {
    this.metadataSize = requirePositive(size, "size");
    return this;
  }

  public MessageMetadata metadata(Consumer<ByteBuf> metadataWriter) {
    this.metadataWriter = Objects.requireNonNull(metadataWriter, "metadataWriter");
    return this;
  }

  public ByteBuf build() {
    ByteBuf metadata = MessageMetadataFlyweight.encode(allocator, metadataSize, isDefaultService);
    Consumer<ByteBuf> mw = metadataWriter;
    if (mw != null) {
      mw.accept(metadata);
    }
    return metadata;
  }

  static int requirePositive(int val, String message) {
    if (val <= 0) {
      throw new IllegalArgumentException(message + " must be positive, provided: " + val);
    }
    return val;
  }

  static final class UnpooledHeapByteBufAllocator implements ByteBufAllocator {
    static final UnpooledHeapByteBufAllocator DEFAULT = new UnpooledHeapByteBufAllocator();

    UnpooledHeapByteBufAllocator() {}

    @Override
    public ByteBuf buffer() {
      return UnpooledByteBufAllocator.DEFAULT.heapBuffer();
    }

    @Override
    public ByteBuf buffer(int initialCapacity) {
      return UnpooledByteBufAllocator.DEFAULT.heapBuffer(initialCapacity);
    }

    @Override
    public ByteBuf buffer(int initialCapacity, int maxCapacity) {
      return UnpooledByteBufAllocator.DEFAULT.heapBuffer(initialCapacity, maxCapacity);
    }

    @Override
    public ByteBuf ioBuffer() {
      throw unsupportedOperationException("ioBuffer");
    }

    @Override
    public ByteBuf ioBuffer(int initialCapacity) {
      throw unsupportedOperationException("ioBuffer");
    }

    @Override
    public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
      throw unsupportedOperationException("ioBuffer");
    }

    @Override
    public ByteBuf heapBuffer() {
      throw unsupportedOperationException("heapBuffer");
    }

    @Override
    public ByteBuf heapBuffer(int initialCapacity) {
      throw unsupportedOperationException("heapBuffer");
    }

    @Override
    public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
      throw unsupportedOperationException("heapBuffer");
    }

    @Override
    public ByteBuf directBuffer() {
      throw unsupportedOperationException("directBuffer");
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity) {
      throw unsupportedOperationException("directBuffer");
    }

    @Override
    public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
      throw unsupportedOperationException("directBuffer");
    }

    @Override
    public CompositeByteBuf compositeBuffer() {
      throw unsupportedOperationException("compositeBuffer");
    }

    @Override
    public CompositeByteBuf compositeBuffer(int maxNumComponents) {
      throw unsupportedOperationException("compositeBuffer");
    }

    @Override
    public CompositeByteBuf compositeHeapBuffer() {
      throw unsupportedOperationException("compositeBuffer");
    }

    @Override
    public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
      throw unsupportedOperationException("compositeBuffer");
    }

    @Override
    public CompositeByteBuf compositeDirectBuffer() {
      throw unsupportedOperationException("compositeBuffer");
    }

    @Override
    public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
      throw unsupportedOperationException("compositeBuffer");
    }

    @Override
    public boolean isDirectBufferPooled() {
      throw unsupportedOperationException("isDirectBufferPooled");
    }

    @Override
    public int calculateNewCapacity(int minNewCapacity, int maxCapacity) {
      throw unsupportedOperationException("calculateNewCapacity");
    }

    private static RuntimeException unsupportedOperationException(String methodName) {
      return new UnsupportedOperationException(
          "not implemented: " + UnpooledHeapByteBufAllocator.class + "." + methodName);
    }
  }

  /*
   * Client metadata
   *
   * [48] magic string
   * [16] header
   * */
  static class MessageMetadataFlyweight {
    /*RSRPC1 ascii string, 48 bit*/
    static final long HEADER_MAGIC = 0b01010010_01010011_01010010_01010000_01000011_00110001L;
    /*header default service flag*/
    static final int FLAG_DEFAULT_SERVICE = 0b1000_0000_0000_0000;

    static ByteBuf encode(ByteBufAllocator allocator, int metadataSize, boolean defaultService) {
      ByteBuf buffer = allocator.buffer(HEADER_SIZE + metadataSize);

      buffer.writeShort((int) (HEADER_MAGIC >> 32));
      buffer.writeShort((int) ((HEADER_MAGIC >> 16) & 0xFFFF));
      buffer.writeShort((int) (HEADER_MAGIC & 0xFFFF));

      int header = 0;
      if (defaultService) {
        header |= FLAG_DEFAULT_SERVICE;
      }
      buffer.writeShort(header);
      return buffer;
    }

    /** @return 16 bit header from metadata, or -1 if metadata does not have header */
    static int header(ByteBuf metadata) {
      if (metadata.readableBytes() < HEADER_SIZE) {
        return -1;
      }
      long header = metadata.getLong(0);
      if (((header >> 16) & HEADER_MAGIC) != HEADER_MAGIC) {
        return -1;
      }
      return (int) (header & 0xFFFF);
    }

    static boolean defaultService(int header) {
      if (header < 0) {
        return false;
      }
      return (header & FLAG_DEFAULT_SERVICE) == FLAG_DEFAULT_SERVICE;
    }
  }
}
