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
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import javax.annotation.Nullable;

/** Channel setup {@link Message} with data & metadata MIME types. */
public abstract class SetupMessage implements ReferenceCounted {
  SetupMessage() {}

  public abstract String metadataMimeType();

  public abstract String dataMimeType();

  public abstract Message message();

  static final class SetupPayload extends SetupMessage {
    private final ByteBuf resumeToken;
    private final boolean leaseEnabled;
    private final int keepAliveInterval;
    private final int keepAliveMaxlifetime;
    private final String metadataMimeType;
    private final String dataMimeType;
    private final Message message;
    private final int version;

    private SetupPayload(
        ByteBuf resumeToken,
        boolean leaseEnabled,
        int version,
        int keepAliveInterval,
        int keepAliveMaxlifetime,
        String metadataMimeType,
        String dataMimeType,
        Message message) {
      this.resumeToken = resumeToken;
      this.leaseEnabled = leaseEnabled;
      this.version = version;
      this.keepAliveInterval = keepAliveInterval;
      this.keepAliveMaxlifetime = keepAliveMaxlifetime;
      this.metadataMimeType = metadataMimeType;
      this.dataMimeType = dataMimeType;
      this.message = message;
    }

    public static SetupPayload create(
        boolean resumeEnabled,
        @Nullable ByteBuf resumeToken,
        boolean leaseEnabled,
        int version,
        int keepAliveTickPeriod,
        int keepAliveTimeout,
        String metadataMimeType,
        String dataMimeType,
        Message setupMessage) {
      return new SetupPayload(
          resumeToken(resumeEnabled, resumeToken),
          leaseEnabled,
          version,
          keepAliveTickPeriod,
          keepAliveTimeout,
          metadataMimeType,
          dataMimeType,
          setupMessage);
    }

    @Override
    public String metadataMimeType() {
      return metadataMimeType;
    }

    @Override
    public String dataMimeType() {
      return dataMimeType;
    }

    @Override
    public Message message() {
      return message;
    }

    public int version() {
      return version;
    }

    public int keepAliveInterval() {
      return keepAliveInterval;
    }

    public int keepAliveMaxLifetime() {
      return keepAliveMaxlifetime;
    }

    public boolean isLeaseEnabled() {
      return leaseEnabled;
    }

    public boolean isResumeEnabled() {
      return resumeToken.readableBytes() > 0;
    }

    public ByteBuf resumeToken() {
      return resumeToken;
    }

    @Override
    public int refCnt() {
      return message.refCnt();
    }

    @Override
    public SetupMessage retain() {
      message.retain();
      return this;
    }

    @Override
    public SetupMessage retain(int increment) {
      message.retain(increment);
      return this;
    }

    @Override
    public SetupMessage touch() {
      message.touch();
      return this;
    }

    @Override
    public SetupMessage touch(Object hint) {
      message.touch(hint);
      return this;
    }

    @Override
    public boolean release() {
      return message.release();
    }

    @Override
    public boolean release(int decrement) {
      return message.release(decrement);
    }

    private static ByteBuf resumeToken(boolean resumeEnabled, @Nullable ByteBuf resumeToken) {
      if (resumeEnabled && resumeToken != null) {
        return resumeToken;
      }
      return Unpooled.EMPTY_BUFFER;
    }
  }
}
