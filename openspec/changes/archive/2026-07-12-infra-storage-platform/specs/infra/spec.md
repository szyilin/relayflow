## ADDED Requirements

### Requirement: Object storage client bootstrap module

The framework OSS starter SHALL provide an `ObjectStorageClient` strategy with a MinIO implementation and a factory that rejects unimplemented provider types without silent fallback.

#### Scenario: MinIO client available

- **WHEN** provider type is `minio` and configuration is valid
- **THEN** `ObjectStorageClientFactory` returns a client capable of presigned PUT/GET and head object operations

#### Scenario: Unimplemented provider at factory

- **WHEN** provider type is `oss` and no implementation is registered
- **THEN** the factory throws `UnsupportedStorageProviderException`

### Requirement: Bootstrap storage properties binding

The application SHALL bind `relayflow.storage.*` properties and expose a bootstrap `StorageProviderConfig` bean for the configured default provider.

#### Scenario: Properties bound at startup

- **WHEN** `application.yml` contains complete `relayflow.storage.minio` settings
- **THEN** `StorageProperties` exposes endpoint, credentials, bucket, and path prefix
