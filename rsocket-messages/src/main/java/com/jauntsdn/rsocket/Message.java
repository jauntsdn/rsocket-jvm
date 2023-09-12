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
import io.netty.util.AbstractReferenceCounted;
import javax.annotation.Nullable;

/** Reference-counted message containing binary data and optionally binary metadata */
public abstract class Message extends AbstractReferenceCounted {

  Message() {}

  public abstract boolean hasMetadata();

  public abstract ByteBuf metadata();

  public abstract boolean hasData();

  public abstract ByteBuf data();

  public abstract boolean hasContent();

  public abstract ByteBuf content();

  public interface Factory {

    Message create(ByteBuf data, @Nullable ByteBuf metadata);

    default Message create(ByteBuf data) {
      return create(data, null);
    }

    Message createMetadata(ByteBuf metadata);

    Message empty();
  }
}
