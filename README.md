![Message-Streams](readme/mstreams.png)

![Maven Central](https://img.shields.io/maven-central/v/com.jauntsdn.rsocket/rsocket-bom)
[![Build](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml/badge.svg)](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml)

# Message-Streams / RSocket-JVM

![RSocket-JVM implementations](readme/impls_stripe.png)

Very fast GRPC-like & GRPC-compatible services on JVM with rich streaming models over multiple data-center and internet transports [[1]](https://jauntsdn.github.io/mstreams/).

## Summary

>multiple APIs: CompletableFuture or virtual threads; traditional streaming with GRPC-API (StreamObserver), or flavor of reactive: smallrye-mutiny, rxjava, reactor;
> 
>pluggable networking: TCP / UNIX / VM sockets; GRPC, websockets, websockets-over-http2, multiprotocol;
> 
>service APIs / RPC codegen stubs (Message-Streams) are decoupled from library runtime (RSocket-JVM, including network transports, load estimators, metrics);
> 
>transparent origin (RPC) & proxy load estimation for cpu-efficient load balancers;
> 
>operates on hardware ranging from single board computers & mobile (2 cores / 32 MB heap) to cloud hosts (dozens of cores / 32+ GB heap).

`RSocket-JVM` is [very fast](https://jauntsdn.github.io/post/rsocket-summary/) alternative (millions of messages per core with each interaction) to 
projectreactor-only `RSocket/RSocket-java` from now-defunct "Reactive Foundation" -
which is plagued by number of performance and security [problems](https://jauntsdn.github.io/post/rsocket-vs-spring/).

RSocket is low latency/high throughput L5 network protocol intended for high-performance services communication. 
It is transport agnostic, and runs on top of any reliable byte stream transport.

This repository hosts `Message-Streams` - API part of `RSocket-JVM`: suite of libraries for interprocess/network communication over 
multiple transports using multiple APIs.  

`Message-Streams` includes RPC: code-generation based remote procedure call system on top of Protocol Buffers.

### CompletableFuture & virtual threads; GRPC StreamObserver; smallrye-mutiny, rxjava, project-reactor

**Multiple vendor libraries**. [Shared protocol core](https://jauntsdn.github.io/post/rsocket-jvm/) with minimal dependencies 
(`netty-buffer` only) streamlines development process for each vendor implementation & reduces cost of maintenance of multiple libraries.

Project supports 3 kinds of APIs: 
* request-response with RSocket-futures (JDK CompletableFuture) or virtual threads; 
* traditional streaming with RSocket-GRPC (GRPC-stubs StreamObserver); 
* flavors of reactive with RSocket-mutiny (smallrye-mutiny), RSocket-rxjava (rxjava3), and RSocket-reactor (project-reactor).

**GRPC compatible**. All implementations are directly compatible with GRPC via `Message-Streams-RPC` & GRPC transport.
GRPC clients can access such services without separate "gateway" binaries and IDL sharing schemes.
 
**Non-intrusive**. [MessageStreams](https://github.com/jauntsdn/rsocket-jvm/blob/1.5.4/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/MessageStreams.java) API & [RSocket-JVM](https://github.com/jauntsdn/rsocket-jvm/blob/1.5.4/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/RSocket.java) runtime are clearly split so from end-user perspective there is 
only set of streaming & non-streaming interactions on buffers/messages:

**traditional streaming**
```groovy
  void requestResponse(Message message, StreamObserver<Message> responseObserver); // one-to-one
  void requestStream(Message message, StreamObserver<Message> responseObserver);   // one-to-many 
  StreamObserver<Message> requestChannel(StreamObserver<Message> responseObserver);// many-to-many
  void fireAndForget(Message message, StreamObserver<Message> responseObserver);   // one-to-zero 
```

**reactive streams**
```groovy
  Publisher<Message> requestResponse(Message message);            // one-to-one
  Publisher<Message> requestStream(Message message);              // one-to-many
  Publisher<Message> requestChannel(Publisher<Message> messages); // many-to-many
  Publisher<Void> fireAndForget(Message message);                 // one-to-zero
```

### RPC 

[MessageStreams-RPC](https://jauntsdn.github.io/post/rsocket-grpc/) is reflection-free, codegen based remote procedure call system 
relying on single data format - protocol buffers. This combination opened many optimization opportunities and enabled 
GRPC interop via respective GRPC transport.

Each vendor library has RPC module accompanied by compiler binary.

### Multiple network transports

Network transports are based on `Netty` only for compatibility with each vendor library, and are part of RSocket-JVM runtime.

Currently comprised of 

* `TCP`, `UNIX domain sockets` & `VM sockets` - known efficient byte stream protocols for datacenter / inter-process communication;

and Http/Http2 based transports for interop:

* `GRPC-RSocket-RPC` for communication with internet clients / external services;

* `websocket` & `websocket-over-http2` for cross-cloud communication with Internet Standards transport;

* `http/json` to expose auxiliary/control APIs as http endpoint;

* `multiprotocol` transport to serve any combination of above over same port.

### Examples

[messagestreams-interop-examples](https://github.com/jauntsdn/messagestreams-grpc-interop-examples) demonstrate all implementations interop.

[messagestreams-grpc-examples](https://github.com/jauntsdn/messagestreams-grpc-examples) demonstrate GRPC-stub StreamObserver based services.

[messagestreams-virtualthreads-examples](https://github.com/jauntsdn/messagestreams-virtualthreads-examples) demonstrate jdk virtual threads based services.

[messagestreams-futures-examples](https://github.com/jauntsdn/messagestreams-futures-examples) demonstrate jdk CompletableFuture based services.

## Build

Requires java20 if virtual threads modules are included (-Pvirtualthreads -Ptoolchains), java8-11 otherwise (depends on vendor library).

```
./gradlew
```

Building & installing artifacts into local maven repository
```
./gradlew clean build publishToMavenLocal
```

## Binaries

Binary releases are published on MavenCentral: virtualthreads (RPC only), futures (CompletableFuture), grpc (GRPC-stub), reactor, rxjava, mutiny.

```groovy

repositories {
    mavenCentral()
}

dependencies {
    implementation "com.jauntsdn.rsocket:rsocket-messages:1.5.4"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-idl:1.5.4"
    implementation "com.jauntsdn.rsocket:rsocket-<VENDOR>:1.5.4"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>:1.5.4"
}
```

MessageStreams-RPC compiler binaries are linux, windows(x86) only
```groovy
protobuf {
     plugins {
          rsocketRpc {
              artifact = "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>-compiler:1.5.4"
          }
     }
}
```

### Message streams. Design goals & scope

**Fast transparent networking with practically useful set of streams libraries, for JVM based serverside, mobile & IOT applications**

Currently smallrye-mutiny, rxjava3, projectreactor; CompletableFutures (plus virtual threads); GRPC-stub (StreamObserver).

Languages and platforms other than JVM lack framework ecosystem (and most lack single reactive streams compatible library),
so there are no substantial reasons for commitment.   

**Message Streams**

Service APIs (Message-Streams plus RPC) and runtime (RSocket-JVM, transports) are explicitly separated so latter may be extended
without affecting end-user application services, and services may be compiled separately from runtime.

**Codegen based RPC on top of Protocol Buffers, compatible with GRPC** 

Protocol Buffers demonstrate acceptable performance with RSocket-JVM implementations, provide framework to extend 
its codegenerator for custom RPC system, and protobuf-java allows efficient usage of netty memory buffers. 

GRPC is dominant RPC based on Protocol Buffers over http2 for both server-side applications and mobile clients (except browsers) - 
second only after http REST APIs. It is supported on each commercially viable language/os/arch, so direct compatibility 
is essential.

**Shared transports**

Transports are shared, and considered part of runtime due to tight contract with RSocket-JVM for performance reasons. 
This project offers strictly few highly optimized transports for interprocess/datacenter (TCP, UNIX sockets, VM sockets) 
and cross-datacenter/internet (GRPC, websocket, websocket-over-http2) communication, instead of user-friendly APIs for
external implementors. This way if supported transports are extended or replaced, transport contract
is free to change to accommodate new needs.   

**Performance**

RSocket-JVM is optimized for small messages < 1KiB in size, typically 0.1 - 0.5 KiB: range covers common use cases
from telemetry to social network chats. The goal is overwhelming throughput advantage 
(Message Streams + RPC, per cpu) over GRPC-java for additional latency < 5 millis (typically ~1 ms) 
with TCP transport, particularly request-response interaction ([comparison](https://jauntsdn.github.io/post/rsocket-vs-spring)). 
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
 
