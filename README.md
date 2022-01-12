![Maven Central](https://img.shields.io/maven-central/v/com.jauntsdn.rsocket/rsocket-bom)

# jauntsdn.com / RSocket-JVM

![RSocket-JVM implementations](readme/impls_stripe.png)

RSocket-JVM is [very fast](https://jauntsdn.com/post/rsocket-summary/) (millions of messages per core with each interaction) alternative to 
projectreactor-only RSocket/RSocket-java from "Reactive Foundation" -
which is plagued by number of performance and security [problems](https://jauntsdn.com/post/rsocket-vs-spring/).

RSocket is low latency/high throughput L5 network protocol 
intended for high-performance services communication. It is transport agnostic, and runs on top 
of any reliable byte stream transport.

This repository hosts API part of RSocket-JVM - suite of libraries for fast interprocess/network communication using major
Reactive Streams implementations.  

RSocket-JVM includes RSocket-RPC: remote procedure call system on top of Protocol Buffers.

### Project-reactor, rxjava, helidon, smallrye-mutiny

RSocket-JVM is currently comprised of RSocket-rxjava (rxjava3), RSocket-reactor (project-reactor), RSocket-helidon (helidon-commons-reactive)
and smallrye-mutiny (mutiny).

**Multiple vendor libraries**. [Shared protocol core](https://jauntsdn.com/post/rsocket-jvm/) with minimal dependencies 
(netty-buffer only) streamline development process for each next vendor library.   
  
**Shared transports**. Message byte transports are based on `rsocket-messages` and netty only 
so usable by each vendor library. 

Currently transports are comprised of `TCP, UNIX, GRPC & websocket-over-http2`, and are considered part 
of (not yet publicly available) RSocket-JVM runtime.

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

Binary releases are published on Maven Central for reactor, rxjava, helidon & mutiny libraries.

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

RSocket-RPC compiler binaries are for linux only
```groovy
protobuf {
     plugins {
          rsocketRpc {
              artifact = "com.jauntsdn.rsocket:rsocket-rpc-<VENDOR>-compiler:1.1.3"
          }
     }
}
```

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
 
