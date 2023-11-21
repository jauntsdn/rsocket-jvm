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

/**
 * API to compare entities availability (higher is better). In channel/RSocket context value of 1.0
 * corresponds to fully available, 0.0 to unavailable
 */
public interface Availability {

  /** Represents availability of all interactions */
  double availability();

  /** Represents availability of all interactions with rank equal or higher than given rank */
  double availability(int rank);

  /** Represents availability of given interaction type with rank equal or higher than given rank */
  default double availability(Interaction interaction) {
    return availability(interaction.rank());
  }
}
