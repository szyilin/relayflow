package com.relayflow.framework.oss.core;

/**
 * Thrown when a provider type is declared but has no client implementation.
 */
public class UnsupportedStorageProviderException extends RuntimeException {

    private final ObjectStorageProviderType providerType;

    public UnsupportedStorageProviderException(ObjectStorageProviderType providerType) {
        super("Object storage provider is not implemented: " + providerType);
        this.providerType = providerType;
    }

    public ObjectStorageProviderType getProviderType() {
        return providerType;
    }
}
