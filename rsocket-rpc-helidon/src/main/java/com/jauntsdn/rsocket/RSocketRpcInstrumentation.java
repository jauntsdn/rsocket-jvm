/*
 * Copyright 2020 - present Maksym Ostroverkhov.
 */

package com.jauntsdn.rsocket;

import io.helidon.common.reactive.Single;
import java.util.concurrent.Flow;
import java.util.function.Function;

public interface RSocketRpcInstrumentation {

  <T> Function<? super Flow.Publisher<T>, ? extends Flow.Publisher<T>> instrumentMulti(
      String role, String service, String method, boolean isStream);

  <T> Function<? super Single<T>, ? extends Single<T>> instrumentSingle(
      String role, String service, String method);
}
