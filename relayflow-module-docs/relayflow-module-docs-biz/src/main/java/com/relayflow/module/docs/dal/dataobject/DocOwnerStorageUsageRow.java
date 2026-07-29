package com.relayflow.module.docs.dal.dataobject;

import lombok.Data;

@Data
public class DocOwnerStorageUsageRow {

    private Long ownerUserId;
    private Long usedBytes;
}
