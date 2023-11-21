## RPC compiler

### compiler options

`metadataless-method-definitions` if present, generated interfaces additionally contain service methods with no metadata parameter, and designate calls with empty metadata. Disabled by default.

`annotations` designate annotations family of generated stubs: one of `javax` (default), `jakarta`, `none`.

`generate-codec-suffix` defines suffix of package containing generated `ProtobufCodec`: one of `none` (default), `random`, or provided value. Helps to avoid duplicate classes if module depends on jars containing generated RPC stubs.

`typed-metadata` if present, generated stubs will contain metadata as Protocol Buffers defined in [rsocket-rpc-metadata-idl](https://github.com/jauntsdn/rsocket-jvm/tree/develop/rsocket-rpc-metadata-idl) instead of raw byte buffers; one of `internal` (default),
`external` (depends on `rsocket-rpc-metadata-idl`). Disabled by default (metadata is represented as raw byte buffers).