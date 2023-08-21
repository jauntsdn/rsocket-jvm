![Message-Streams](readme/mstreams.png)

![Maven Central](https://img.shields.io/maven-central/v/com.jauntsdn.rsocket/rsocket-bom)
[![Build](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml/badge.svg)](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml)

# jauntsdn.com Message-Streams / RSocket-JVM

![RSocket-JVM implementations](readme/impls_stripe.png)

Very fast GRPC-like & GRPC-compatible services on JVM with rich streaming models over multiple data-center and internet transports [[1]](https://jauntsdn.com/mstreams/).

## Summary

>multiple APIs: CompletableFuture or virtual threads; traditional streaming with GRPC-API (StreamObserver), or flavor of reactive: smallrye-mutiny, rxjava, reactor;
> 
>pluggable networking: TCP, unix sockets, VM sockets, GRPC, websockets, websockets-over-http2;
> 
>service APIs / RPC codegen stubs (Message-Streams) are split from library runtime (RSocket-JVM, including network transports, load estimators, metrics);
> 
>transparent origin (RPC) & proxy load estimation which enables cpu-efficient load balancers;
> 
>native image support with graalvm

`RSocket-JVM` is [very fast](https://jauntsdn.com/post/rsocket-summary/) (millions of messages per core with each interaction) alternative to 
projectreactor-only RSocket/RSocket-java from "Reactive Foundation" -
which is plagued by number of performance and security [problems](https://jauntsdn.com/post/rsocket-vs-spring/).

This repository hosts `Message-Streams` - API part of `RSocket-JVM`: suite of libraries for interprocess/network communication over 
multiple transports using multiple APIs.  

`Message-Streams` includes RPC: code-generation based remote procedure call system on top of Protocol Buffers.

RSocket is low latency/high throughput L5 network protocol intended for high-performance services communication. 
It is transport agnostic, and runs on top of any reliable byte stream transport.

### CompletableFuture; virtual threads; GRPC StreamObserver; smallrye-mutiny, rxjava, project-reactor

**Multiple vendor libraries**. [Shared protocol core](https://jauntsdn.com/post/rsocket-jvm/) with minimal dependencies 
(`netty-buffer` only) streamlined development process for each vendor library & reduced maintenance cost of multiple libraries to feasible level.

Project supports 3 kinds of APIs: 
* request-response with RSocket-futures (JDK CompletableFuture) or virtual threads; 
* traditional streaming with RSocket-GRPC (GRPC-stubs StreamObserver); 
* flavors of reactive with RSocket-mutiny (smallrye-mutiny), RSocket-rxjava (rxjava3), and RSocket-reactor (project-reactor).

**GRPC compatible**. All implementations are directly compatible with GRPC via MessageStreams-RPC & GRPC transport.
GRPC clients can access such services without separate "gateway" binaries and IDL sharing schemes.
 
**Non-intrusive**. [MessageStreams](https://github.com/jauntsdn/rsocket-jvm/blob/1.5.0/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/MessageStreams.java) API & [RSocket-JVM](https://github.com/jauntsdn/rsocket-jvm/blob/1.5.0/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/RSocket.java) runtime are clearly split so from end-user perspective there is 
only set of streaming & non-streaming interactions on buffers/messages:

```groovy
  void requestResponse(Message message, StreamObserver<Message> responseObserver);
  void requestStream(Message message, StreamObserver<Message> responseObserver);
  StreamObserver<Message> requestChannel(StreamObserver<Message> responseObserver);
  void fireAndForget(Message message, StreamObserver<Message> responseObserver);
```

```groovy
  Publisher<Message> requestResponse(Message message);
  Publisher<Message> requestStream(Message message);
  Publisher<Message> requestChannel(Publisher<Message> messages);
  Publisher<Void> fireAndForget(Message message);
```

### RPC 

[MessageStreams-RPC](https://jauntsdn.com/post/rsocket-grpc/) is reflection-free, codegen based remote procedure call system 
relying on single data format - protocol buffers. This combination opened many optimization opportunities and enabled 
GRPC interop via respective GRPC transport.

Each vendor library has RPC module accompanied by compiler binary.

### Multiple network transports

Network transports are based on `Netty` only for compatibility with each vendor library, and are part of RSocket-JVM runtime.

Currently comprised of 

* `TCP`, `UNIX domain sockets` & `VM sockets` - known efficient byte stream protocols for datacenter / inter-process communication;

and Http2 streams based transports for interop:

* `GRPC-RSocket-RPC` for communication with internet clients / external services;

* `WebSocket` & `Websocket-over-http2` for cross-cloud communication with Internet Standards transport.

### Examples

[messagestreams-interop-examples](https://github.com/jauntsdn/messagestreams-grpc-interop-examples) demonstrate all implementations interop.

[messagestreams-grpc-examples](https://github.com/jauntsdn/messagestreams-grpc-examples) demonstrate GRPC-stub StreamObserver based services.

[messagestreams-futures-examples](https://github.com/jauntsdn/messagestreams-futures-examples) demonstrate jdk CompletableFuture based services.

## Build

Building `jauntsdn/RSocket-jvm` requires java20 for virtual threads API, java11+ for smallrye-mutiny; 
futures / grpc-stubs / rxjava / reactor are java8+. 
```
./gradlew
```

Building & installing artifacts into local maven repository
```
./gradlew clean build publishToMavenLocal
```

## Binaries

Binary releases are published on Maven Central for virtual threads, futures (CompletableFuture), grpc stubs, reactor, rxjava & mutiny libraries.

```groovy

repositories {
    mavenCentral()
}

dependencies {
    implementation "com.jauntsdn.rsocket:rsocket-messages:1.5.0"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-idl:1.5.0"
    implementation "com.jauntsdn.rsocket:rsocket-<VENDOR>:1.5.0"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>:1.5.0"
}
```

MessageStreams-RPC compiler binaries are linux, windows(x86) only
```groovy
protobuf {
     plugins {
          rsocketRpc {
              artifact = "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>-compiler:1.5.0"
          }
     }
}
```

For `virtualthreads` APIs binaries are available for RPC only.

### Message streams. Design goals & scope

**Fast transparent networking with practically useful set of streams libraries, solely for JVM serverside applications**

Currently CompletableFutures (plus virtual threads), GRPC-stubs (StreamObserver), smallrye-mutiny, rxjava3, projectreactor.

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
This project offers strictly few highly optimized transports for interprocess/datacenter (TCP, UNIX sockets, VM sockets) 
and cross-datacenter (GRPC-RSocketRPC, websocket, websocket-over-http2) communication, instead of user-friendly APIs for
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
 
