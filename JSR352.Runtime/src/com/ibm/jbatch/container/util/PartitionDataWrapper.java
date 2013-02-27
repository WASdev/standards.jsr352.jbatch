package com.ibm.jbatch.container.util;

import java.io.Externalizable;

import javax.batch.operations.JobOperator.BatchStatus;

public class PartitionDataWrapper {
    
    private Externalizable collectorData;

    private BatchStatus batchStatus;
    
    private String exitStatus;
    
    private PartitionEventType eventType;

    public enum PartitionEventType { ANALYZE_COLLECTOR_DATA,
        ANALYZE_STATUS,
        STEP_FINISHED;
    }
    
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

    public Externalizable getCollectorData() {
        return collectorData;
    }

    public void setCollectorData(Externalizable collectorData) {
        this.collectorData = collectorData;
    }

    public PartitionEventType getEventType() {
        return eventType;
    }

    public void setEventType(PartitionEventType eventType) {
        this.eventType = eventType;
    }
    
}
