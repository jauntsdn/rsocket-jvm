![Message-Streams](readme/mstreams.png)

![Maven Central](https://img.shields.io/maven-central/v/com.jauntsdn.rsocket/rsocket-bom)
[![Build](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml/badge.svg)](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml)

# jauntsdn.com Message-Streams / RSocket-JVM

![RSocket-JVM implementations](readme/impls_stripe.png)

## TL;DR

>lean & very fast GRPC-like [services](https://github.com/jauntsdn/rsocket-jvm-interop-examples/tree/feature/oss/jaunt-rsocket-reactor-service/src/generated/main/rsocketRpc/trisocket) on JVM with rich streaming model;
> 
>multiple APIs: CompletableFuture; streaming with reactor, rxjava, mutiny, helidon;
> 
>pluggable networking: tcp, unix sockets, grpc, websockets-over-http2;
> 
>service APIs / codegen stubs (Message-Streams) are split from library runtime (including network transports, load estimators, metrics);
> 
>transparent origin (RPC) & proxy load estimation which enables cpu-efficient load balancers;
> 
>native image support with graalvm

RSocket-JVM is [very fast](https://jauntsdn.com/post/rsocket-summary/) (millions of messages per core with each interaction) alternative to 
projectreactor-only RSocket/RSocket-java from "Reactive Foundation" -
which is plagued by number of performance and security [problems](https://jauntsdn.com/post/rsocket-vs-spring/).

RSocket is low latency/high throughput L5 network protocol 
intended for high-performance services communication. It is transport agnostic, and runs on top 
of any reliable byte stream transport.

This repository hosts Message-Streams - API part of RSocket-JVM, suite of libraries for fast interprocess/network communication using CompletableFutures & major
Reactive Streams implementations.  

RSocket-JVM includes RSocket-RPC: remote procedure call system on top of Protocol Buffers.

### CompletableFuture, project-reactor, rxjava, helidon, smallrye-mutiny

RSocket-JVM is currently comprised of RSocket-futures(CompletableFuture), RSocket-rxjava (rxjava3), RSocket-reactor (project-reactor), RSocket-helidon (helidon-commons-reactive)
and smallrye-mutiny (mutiny).

**Multiple vendor libraries**. [Shared protocol core](https://jauntsdn.com/post/rsocket-jvm/) with minimal dependencies 
(netty-buffer only) streamline development process for each next vendor library.   
  
**Non-intrusive**. API ([MessageStreams](https://github.com/jauntsdn/rsocket-jvm/blob/1.1.3/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/MessageStreams.java)) & runtime ([RSocket](https://github.com/jauntsdn/rsocket-jvm/blob/1.1.3/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/RSocket.java)) are clearly split so from end-user perspective there is 
only defined set of basic interactions on buffers/messages:
```groovy
  Publisher<Message> requestResponse(Message message);
  Publisher<Message> requestStream(Message message);
  Publisher<Message> requestChannel(Publisher<Message> messages);
  Publisher<Void> fireAndForget(Message message);
```

**GRPC compatible**. All implementations are directly compatible with GRPC via RSocket-RPC & GRPC transport.
GRPC clients can access such services without separate "gateway" binaries and awkward IDL sharing schemes.

### Shared transports

Message transports are based on `netty` only so usable by each vendor library, and are considered part 
of (not yet publicly available) RSocket-JVM runtime.

Currently they are comprised of 

* `TCP` & `UNIX domain sockets` - known efficient byte stream protocols for datacenter / inter-process communication,

and Http2 streams based transports for interop:

* `GRPC-RSocket-RPC` for communication with internet clients / external services;

* `Websocket-over-http2` for cross-cloud communication with Internet Standards transport.

### RSocket-RPC 

[RSocket-RPC](https://jauntsdn.com/post/rsocket-grpc/) is reflection-free, codegen based remote procedure call system 
relying on single data format - protocol buffers. This combination opened many optimization opportunities and enabled 
GRPC interop via respective GRPC transport.

Each vendor library has RSocket-RPC API module accompanied by compiler binary.

### Examples

[RSocket-jvm-interop-examples](https://github.com/jauntsdn/rsocket-jvm-interop-examples).

## Build

Building `jauntsdn/RSocket-jvm` requires java11 for helidon, and java8 for rxjava/reactor/mutiny. 
```
./gradlew
```

Building & installing artifacts into local maven repository
```
./gradlew clean build publishToMavenLocal
```

## Binaries

Binary releases are published on Maven Central for java futures (CompletableFuture), reactor, rxjava, helidon & mutiny libraries.

```groovy

repositories {
    mavenCentral()
}

dependencies {
    implementation "com.jauntsdn.rsocket:rsocket-messages:1.1.3"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-idl:1.1.3"
    implementation "com.jauntsdn.rsocket:rsocket-<VENDOR>:1.1.3"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>:1.1.3"
}
```

RSocket-RPC compiler binaries are linux, windows(x86) only
```groovy
protobuf {
     plugins {
          rsocketRpc {
              artifact = "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>-compiler:1.1.3"
          }
     }
}
```

### Message streams. Design goals & scope

**Fast transparent networking with practically useful set of reactive streams libraries, solely for JVM serverside applications**

Currently rxjava3, projectreactor, smallrye-mutiny, helidon-reactive-streams.

Languages and platforms other than JVM lack framework ecosystem (and most lack single reactive streams compatible library),
so there are no substantial (except populist) reasons for commitment.   

**Message Streams**

Service APIs (Message Streams + RPC) and runtime (RSocket-JVM-runtime) are explicitly separated so latter may be extended
without affecting end-user application services, and services may be compiled separately from runtime.

**Codegen based RPC on top of Protocol Buffers, compatible with GRPC** 

Protocol Buffers demonstrate acceptable performance with RSocket-JVM impls, provide framework to extend 
its codegenerator for custom RPC system, plus Protobuf-java allows very efficient usage of netty memory buffers. 

GRPC is dominant RPC based on Protocol Buffers over http2 for both server-side applications and mobile clients (except browsers) - 
second only after http REST APIs. It is supported on each commercially viable language/os/arch, so direct compatibility 
is essential for JVM-only Message Streams (RSocket-JVM) libraries.

**Shared transports**

Transports are shared, and considered part of runtime due to tight contract with RSocket-JVM for performance reasons. 
This project offers strictly few highly optimized transports for interprocess/datacenter (TCP, UNIX sockets) 
and cross-datacenter (GRPC-RSocketRPC, websocket-over-htp2) communication, instead of user-friendly APIs for
external implementors. This way if supported transports are extended or replaced, transport contract
is free to change to accommodate new needs.   

**Targeting proxies**

Important goal is performance loss minimization for proxy/intermediary case as there is no need to access/expose user data -
only rewrite frame's header & non-user metadata in place.

**Performance**

RSocket-JVM is optimized for small messages < 1KiB in size, typically 0.1 - 0.5 KiB: range covers common use cases
from telemetry to social network chats. The goal is overwhelming throughput advantage 
(Message Streams + RPC, per cpu) over GRPC-java for additional latency < 5 millis (typically ~1 ms) 
with TCP transport, particularly request-response interaction ([comparison](https://jauntsdn.com/post/rsocket-vs-spring)). 
Advantage < 2x would probably make project non-competitive against GRPC due to new network software stack and different programming
model (as in case of RSocket/RSocket-java from "reactive foundation" which somehow is even slower than GRPC-java on streaming interactions).

## LICENSE

Copyright 2020-Present Maksym Ostroverkhov.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 
