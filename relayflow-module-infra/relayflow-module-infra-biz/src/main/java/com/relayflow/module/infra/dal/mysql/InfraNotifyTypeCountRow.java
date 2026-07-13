package com.relayflow.module.infra.dal.mysql;

import lombok.Data;

@Data
public class InfraNotifyTypeCountRow {

    private String type;

    private Long cnt;
}
