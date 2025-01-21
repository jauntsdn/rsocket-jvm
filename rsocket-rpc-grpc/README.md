## RPC compiler

### compiler options

`metadataless-method-definitions` if present, generated interfaces additionally contain service methods with no metadata parameter, and designate calls with empty metadata. Disabled by default.

`annotations` designate annotations family of generated stubs: one of `javax` (default), `jakarta`, `none`.

`generate-codec-suffix` defines suffix of package containing generated `ProtobufCodec`: one of `none` (default), `random`, or provided value. Helps to avoid duplicate classes if module depends on jars containing generated RPC stubs.

`typed-metadata` if present, generated stubs will contain metadata as Protocol Buffers defined in [rsocket-rpc-metadata-idl](https://github.com/jauntsdn/rsocket-jvm/tree/develop/rsocket-rpc-metadata-idl) instead of raw byte buffers; one of `internal` (default),
`external` (depends on `rsocket-rpc-metadata-idl`). Disabled by default (metadata is represented as raw byte buffers).

`timeouts` if present, generated client stubs will contain handler for response timeout configured with `Headers.timeout`. Requires `typed-metadata` option.

`generate-service-descriptors` if present, generated server stubs will contain service descriptors for transcoding Protocol Buffers into another representation (e.g. http/json).  Value format is "service_foo.proto;service_bar.proto", or empty to generate for all services.

`instrumentation` defines sources of instrumentation listeners in generated stubs: one of `all` (default) - both external (constructor) and provided (via MessageStreams implementation attribute),
`mstreams` - provided only (via MessageStreams implementation attribute).