package com.ibm.jbatch.container.util;

import java.io.Serializable;

import javax.batch.runtime.BatchStatus;

public class PartitionDataWrapper {
    
    private Serializable collectorData;

    private BatchStatus batchStatus;
    
    private String exitStatus;
    
    private PartitionEventType eventType;

    public enum PartitionEventType { ANALYZE_COLLECTOR_DATA, ANALYZE_STATUS }
    
    public BatchStatus getBatchstatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public Serializable getCollectorData() {
        return collectorData;
    }

    public void setCollectorData(Serializable collectorData) {
        this.collectorData = collectorData;
    }

    public PartitionEventType getEventType() {
        return eventType;
    }

    public void setEventType(PartitionEventType eventType) {
        this.eventType = eventType;
    }
    
}
