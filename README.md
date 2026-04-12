![Message-Streams](readme/mstreams.png)

![Maven Central](https://img.shields.io/maven-central/v/com.jauntsdn.rsocket/rsocket-bom)
[![Build](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml/badge.svg)](https://github.com/jauntsdn/rsocket-jvm/actions/workflows/ci-build.yml)

# Message-Streams / RSocket-JVM

![RSocket-JVM implementations](readme/impls_stripe.png)

Very fast GRPC-like & GRPC-compatible services on JVM with rich streaming models over multiple data-center and internet transports [[1]](https://jauntsdn.github.io/mstreams/).

## Summary

>multiple APIs: CompletableFuture or virtual threads; traditional streaming with GRPC-API (StreamObserver), or flavor of reactive: smallrye-mutiny, rxjava, reactor;
> 
>pluggable networking: bytestream - TCP / UNIX / VM sockets; internet - GRPC, websockets, websockets-over-http2, multi-protocol; multi-transport;
> 
>service APIs / RPC codegen stubs (Message-Streams) are decoupled from library runtime (RSocket-JVM) - including network transports, load estimators, load balancers and metrics;
> 
> built-in endpoint self load estimation (with requests leasing feature) enables common resilience policies and efficient load balancers;
> 
>operates on hardware ranging from single board computers & mobile (2 cores / 32 MB heap) to cloud hosts (dozens of cores / 32+ GB heap).

`RSocket-JVM` is [very fast](https://jauntsdn.github.io/post/rsocket-summary/) alternative (millions of messages per core with each interaction) to 
projectreactor-only `RSocket/RSocket-java` from now-defunct "Reactive Foundation" -
which is plagued by number of performance and security [problems](https://jauntsdn.github.io/post/rsocket-vs-spring/).

RSocket is low latency/high throughput L5 network protocol intended for high-performance services communication. 
It is transport agnostic, and runs on top of any reliable byte stream transport.

This repository hosts `Message-Streams` - API part of `RSocket-JVM`: suite of libraries for interprocess/network communication over 
multiple transports using multiple streaming & non-streaming APIs.  

`Message-Streams` includes RPC - code-generation based remote procedure call system on top of Protocol Buffers, similar to GRPC.

### CompletableFuture & virtual threads; GRPC StreamObserver; smallrye-mutiny, rxjava, project-reactor

**Multiple vendor libraries**. [Shared protocol core](https://jauntsdn.github.io/post/rsocket-jvm/) with minimal dependencies 
(`netty-buffer` only) streamlines development process for each vendor implementation & reduces cost of maintenance of multiple libraries.

Project supports 3 kinds of APIs: 
* request-response with RSocket-futures (JDK CompletableFuture) or virtual threads; 
* traditional streaming with RSocket-GRPC (GRPC-stubs StreamObserver); 
* flavors of reactive with RSocket-mutiny (smallrye-mutiny), RSocket-rxjava (rxjava3), and RSocket-reactor (project-reactor).

**GRPC compatible**. All implementations are directly compatible with GRPC via `Message-Streams-RPC` & GRPC transport.
GRPC clients can access such services without separate "gateway" binaries and IDL sharing schemes.
 
**Non-intrusive**. [MessageStreams](https://github.com/jauntsdn/rsocket-jvm/blob/1.6.0/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/MessageStreams.java) API & [RSocket-JVM](https://github.com/jauntsdn/rsocket-jvm/blob/1.6.0/rsocket-reactor/src/main/java/com/jauntsdn/rsocket/RSocket.java) runtime are clearly split so from end-user perspective there is 
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

Each vendor library has RPC module accompanied by compiler binary to generate RPC service and client stubs.

### Multiple network transports

Network transports are `netty`-only for compatibility with each vendor library, and are part of RSocket-JVM runtime. 
Comprised of 

* `TCP`, `UNIX domain sockets`, `VM sockets` - known efficient byte stream transports for datacenter / inter-process communication;

and http based transports for interop:

* `GRPC` - internet clients / external services communication;

* `websocket`, `websocket-over-http2` - cross-cloud communication with Internet Standards transports;

* `http/json` to expose auxiliary/control APIs as http endpoint;

* `multiprotocol` transport to serve any combination of above over same port.

### Examples

[messagestreams-interop-examples](https://github.com/jauntsdn/messagestreams-grpc-interop-examples) demonstrate all implementations interop.

[messagestreams-grpc-examples](https://github.com/jauntsdn/messagestreams-grpc-examples) demonstrate GRPC-stub StreamObserver based services.

[messagestreams-virtualthreads-examples](https://github.com/jauntsdn/messagestreams-virtualthreads-examples) demonstrate jdk virtual threads based services.

[messagestreams-futures-examples](https://github.com/jauntsdn/messagestreams-futures-examples) demonstrate jdk CompletableFuture based services.

## Build

Requires jdk21 if virtual threads modules are included (-Pvirtualthreads -Ptoolchains), jdk8-11 otherwise (depends on vendor library).

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
    implementation "com.jauntsdn.rsocket:rsocket-messages:1.6.0"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-idl:1.6.0"
    implementation "com.jauntsdn.rsocket:rsocket-<VENDOR>:1.6.0"
    implementation "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>:1.6.0"
}
```

MessageStreams-RPC compiler binaries are linux, windows(x86) only
```groovy
protobuf {
     plugins {
          rsocketRpc {
              artifact = "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>-compiler:1.6.0"
          }
     }
}
```

### Performance

RSocket-JVM is optimized for small messages < 1KiB in size, typically 0.1 - 0.5 KiB: range covers common use cases
from telemetry to social network chats. The goal is overwhelming throughput advantage 
(Message Streams + RPC, per cpu) over GRPC-java , latency target < 5 millis (typically ~1 ms) 
with datacenter bytestream transports (primarily TCP), request-reply and server-stream interactions ([comparison](https://jauntsdn.github.io/post/rsocket-vs-spring)). 
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
 
