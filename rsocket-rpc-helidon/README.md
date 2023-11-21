## RPC compiler

### compiler options

`metadataless-method-definitions` if present, generated interfaces additionally contain service methods with no metadata parameter, and designate calls with empty metadata. Disabled by default.

`annotations` designate annotations family of generated stubs: one of `javax` (default), `jakarta`, `none`.