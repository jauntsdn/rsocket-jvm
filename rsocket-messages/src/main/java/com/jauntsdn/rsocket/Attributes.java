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

public interface Attributes {
  String EXTERNAL_METADATA_SIZE = "external_metadata_size";
  String EVENT_LOOP_ONLY = "event_loop_only";
  String RPC_CODEC = "rpc_codec";
  String ALLOCATOR = "allocator";

  int intAttr(String key);

  <T> T attr(String key);

  Attributes EMPTY =
      new Attributes() {
        @Override
        public int intAttr(String key) {
          return 0;
        }

        @Override
        public <T> T attr(String key) {
          return null;
        }
      };
}
