#Signature file v4.1
#Version 

CLSS public abstract interface java.io.Serializable

CLSS public abstract interface java.lang.Comparable<%0 extends java.lang.Object>
meth public abstract int compareTo({java.lang.Comparable%0})

CLSS public abstract java.lang.Enum<%0 extends java.lang.Enum<{java.lang.Enum%0}>>
cons protected <init>(java.lang.String,int)
intf java.io.Serializable
intf java.lang.Comparable<{java.lang.Enum%0}>
meth protected final java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth protected final void finalize()
meth public final boolean equals(java.lang.Object)
meth public final int compareTo({java.lang.Enum%0})
meth public final int hashCode()
meth public final int ordinal()
meth public final java.lang.Class<{java.lang.Enum%0}> getDeclaringClass()
meth public final java.lang.String name()
meth public java.lang.String toString()
meth public static <%0 extends java.lang.Enum<{%%0}>> {%%0} valueOf(java.lang.Class<{%%0}>,java.lang.String)
supr java.lang.Object
hfds name,ordinal

CLSS public java.lang.Exception
cons protected <init>(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Throwable
hfds serialVersionUID

CLSS public java.lang.Object
cons public <init>()
meth protected java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth protected void finalize() throws java.lang.Throwable
meth public boolean equals(java.lang.Object)
meth public final java.lang.Class<?> getClass()
meth public final void notify()
meth public final void notifyAll()
meth public final void wait() throws java.lang.InterruptedException
meth public final void wait(long) throws java.lang.InterruptedException
meth public final void wait(long,int) throws java.lang.InterruptedException
meth public int hashCode()
meth public java.lang.String toString()

CLSS public java.lang.RuntimeException
cons protected <init>(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Exception
hfds serialVersionUID

CLSS public java.lang.Throwable
cons protected <init>(java.lang.String,java.lang.Throwable,boolean,boolean)
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
intf java.io.Serializable
meth public final java.lang.Throwable[] getSuppressed()
meth public final void addSuppressed(java.lang.Throwable)
meth public java.lang.StackTraceElement[] getStackTrace()
meth public java.lang.String getLocalizedMessage()
meth public java.lang.String getMessage()
meth public java.lang.String toString()
meth public java.lang.Throwable fillInStackTrace()
meth public java.lang.Throwable getCause()
meth public java.lang.Throwable initCause(java.lang.Throwable)
meth public void printStackTrace()
meth public void printStackTrace(java.io.PrintStream)
meth public void printStackTrace(java.io.PrintWriter)
meth public void setStackTrace(java.lang.StackTraceElement[])
supr java.lang.Object
hfds CAUSE_CAPTION,EMPTY_THROWABLE_ARRAY,NULL_CAUSE_MESSAGE,SELF_SUPPRESSION_MESSAGE,SUPPRESSED_CAPTION,SUPPRESSED_SENTINEL,UNASSIGNED_STACK,backtrace,cause,detailMessage,serialVersionUID,stackTrace,suppressedExceptions
hcls PrintStreamOrWriter,SentinelHolder,WrappedPrintStream,WrappedPrintWriter

CLSS public abstract interface java.lang.annotation.Annotation
meth public abstract boolean equals(java.lang.Object)
meth public abstract int hashCode()
meth public abstract java.lang.Class<? extends java.lang.annotation.Annotation> annotationType()
meth public abstract java.lang.String toString()

CLSS public abstract interface !annotation java.lang.annotation.Documented
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation

CLSS public abstract interface !annotation java.lang.annotation.Retention
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.annotation.RetentionPolicy value()

CLSS public abstract interface !annotation java.lang.annotation.Target
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation
meth public abstract java.lang.annotation.ElementType[] value()

CLSS public abstract interface !annotation javax.batch.annotation.BatchProperty
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[FIELD, METHOD, PARAMETER])
 anno 0 javax.inject.Qualifier()
intf java.lang.annotation.Annotation
meth public abstract !hasdefault java.lang.String name()
 anno 0 javax.enterprise.util.Nonbinding()

CLSS public abstract javax.batch.api.AbstractBatchlet
cons public <init>()
intf javax.batch.api.Batchlet
meth public abstract java.lang.String process() throws java.lang.Exception
meth public void stop() throws java.lang.Exception
supr java.lang.Object

CLSS public abstract interface javax.batch.api.Batchlet
meth public abstract java.lang.String process() throws java.lang.Exception
meth public abstract void stop() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.Decider
meth public abstract java.lang.String decide(javax.batch.runtime.StepExecution<?>[]) throws java.lang.Exception

CLSS public abstract javax.batch.api.chunk.AbstractCheckpointAlgorithm
cons public <init>()
intf javax.batch.api.chunk.CheckpointAlgorithm
meth public abstract boolean isReadyToCheckpoint() throws java.lang.Exception
meth public int checkpointTimeout() throws java.lang.Exception
meth public void beginCheckpoint() throws java.lang.Exception
meth public void endCheckpoint() throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.chunk.AbstractItemReader<%0 extends java.lang.Object>
cons public <init>()
intf javax.batch.api.chunk.ItemReader<{javax.batch.api.chunk.AbstractItemReader%0}>
meth public abstract {javax.batch.api.chunk.AbstractItemReader%0} readItem() throws java.lang.Exception
meth public java.io.Serializable checkpointInfo() throws java.lang.Exception
meth public void close() throws java.lang.Exception
meth public void open(java.io.Serializable) throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.chunk.AbstractItemWriter<%0 extends java.lang.Object>
cons public <init>()
intf javax.batch.api.chunk.ItemWriter<{javax.batch.api.chunk.AbstractItemWriter%0}>
meth public abstract void writeItems(java.util.List<{javax.batch.api.chunk.AbstractItemWriter%0}>) throws java.lang.Exception
meth public java.io.Serializable checkpointInfo() throws java.lang.Exception
meth public void close() throws java.lang.Exception
meth public void open(java.io.Serializable) throws java.lang.Exception
supr java.lang.Object

CLSS public abstract interface javax.batch.api.chunk.CheckpointAlgorithm
meth public abstract boolean isReadyToCheckpoint() throws java.lang.Exception
meth public abstract int checkpointTimeout() throws java.lang.Exception
meth public abstract void beginCheckpoint() throws java.lang.Exception
meth public abstract void endCheckpoint() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.ItemProcessor<%0 extends java.lang.Object, %1 extends java.lang.Object>
meth public abstract {javax.batch.api.chunk.ItemProcessor%1} processItem({javax.batch.api.chunk.ItemProcessor%0}) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.ItemReader<%0 extends java.lang.Object>
meth public abstract java.io.Serializable checkpointInfo() throws java.lang.Exception
meth public abstract void close() throws java.lang.Exception
meth public abstract void open(java.io.Serializable) throws java.lang.Exception
meth public abstract {javax.batch.api.chunk.ItemReader%0} readItem() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.ItemWriter<%0 extends java.lang.Object>
meth public abstract java.io.Serializable checkpointInfo() throws java.lang.Exception
meth public abstract void close() throws java.lang.Exception
meth public abstract void open(java.io.Serializable) throws java.lang.Exception
meth public abstract void writeItems(java.util.List<{javax.batch.api.chunk.ItemWriter%0}>) throws java.lang.Exception

CLSS public abstract javax.batch.api.chunk.listener.AbstractChunkListener
cons public <init>()
intf javax.batch.api.chunk.listener.ChunkListener
meth public void afterChunk() throws java.lang.Exception
meth public void beforeChunk() throws java.lang.Exception
meth public void onError() throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.chunk.listener.AbstractItemProcessListener<%0 extends java.lang.Object, %1 extends java.lang.Object>
cons public <init>()
intf javax.batch.api.chunk.listener.ItemProcessListener<{javax.batch.api.chunk.listener.AbstractItemProcessListener%0},{javax.batch.api.chunk.listener.AbstractItemProcessListener%1}>
meth public void afterProcess({javax.batch.api.chunk.listener.AbstractItemProcessListener%0},{javax.batch.api.chunk.listener.AbstractItemProcessListener%1}) throws java.lang.Exception
meth public void beforeProcess({javax.batch.api.chunk.listener.AbstractItemProcessListener%0}) throws java.lang.Exception
meth public void onProcessError({javax.batch.api.chunk.listener.AbstractItemProcessListener%0},java.lang.Exception) throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.chunk.listener.AbstractItemReadListener<%0 extends java.lang.Object>
cons public <init>()
intf javax.batch.api.chunk.listener.ItemReadListener<{javax.batch.api.chunk.listener.AbstractItemReadListener%0}>
meth public void afterRead({javax.batch.api.chunk.listener.AbstractItemReadListener%0}) throws java.lang.Exception
meth public void beforeRead() throws java.lang.Exception
meth public void onReadError(java.lang.Exception) throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.chunk.listener.AbstractItemWriteListener<%0 extends java.lang.Object>
cons public <init>()
intf javax.batch.api.chunk.listener.ItemWriteListener<{javax.batch.api.chunk.listener.AbstractItemWriteListener%0}>
meth public void afterWrite(java.util.List<{javax.batch.api.chunk.listener.AbstractItemWriteListener%0}>) throws java.lang.Exception
meth public void beforeWrite(java.util.List<{javax.batch.api.chunk.listener.AbstractItemWriteListener%0}>) throws java.lang.Exception
meth public void onWriteError(java.util.List<{javax.batch.api.chunk.listener.AbstractItemWriteListener%0}>,java.lang.Exception) throws java.lang.Exception
supr java.lang.Object

CLSS public abstract interface javax.batch.api.chunk.listener.ChunkListener
meth public abstract void afterChunk() throws java.lang.Exception
meth public abstract void beforeChunk() throws java.lang.Exception
meth public abstract void onError() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.ItemProcessListener<%0 extends java.lang.Object, %1 extends java.lang.Object>
meth public abstract void afterProcess({javax.batch.api.chunk.listener.ItemProcessListener%0},{javax.batch.api.chunk.listener.ItemProcessListener%1}) throws java.lang.Exception
meth public abstract void beforeProcess({javax.batch.api.chunk.listener.ItemProcessListener%0}) throws java.lang.Exception
meth public abstract void onProcessError({javax.batch.api.chunk.listener.ItemProcessListener%0},java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.ItemReadListener<%0 extends java.lang.Object>
meth public abstract void afterRead({javax.batch.api.chunk.listener.ItemReadListener%0}) throws java.lang.Exception
meth public abstract void beforeRead() throws java.lang.Exception
meth public abstract void onReadError(java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.ItemWriteListener<%0 extends java.lang.Object>
meth public abstract void afterWrite(java.util.List<{javax.batch.api.chunk.listener.ItemWriteListener%0}>) throws java.lang.Exception
meth public abstract void beforeWrite(java.util.List<{javax.batch.api.chunk.listener.ItemWriteListener%0}>) throws java.lang.Exception
meth public abstract void onWriteError(java.util.List<{javax.batch.api.chunk.listener.ItemWriteListener%0}>,java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.RetryProcessListener<%0 extends java.lang.Object>
meth public abstract void onRetryProcessException({javax.batch.api.chunk.listener.RetryProcessListener%0},java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.RetryReadListener<%0 extends java.lang.Object>
meth public abstract void onRetryReadException(java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.RetryWriteListener<%0 extends java.lang.Object>
meth public abstract void onRetryWriteException(java.util.List<{javax.batch.api.chunk.listener.RetryWriteListener%0}>,java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.SkipProcessListener<%0 extends java.lang.Object>
meth public abstract void onSkipProcessItem({javax.batch.api.chunk.listener.SkipProcessListener%0},java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.SkipReadListener
meth public abstract void onSkipReadItem(java.lang.Exception) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.chunk.listener.SkipWriteListener<%0 extends java.lang.Object>
meth public abstract void onSkipWriteItem(java.util.List<{javax.batch.api.chunk.listener.SkipWriteListener%0}>,java.lang.Exception) throws java.lang.Exception

CLSS public abstract javax.batch.api.listener.AbstractJobListener
cons public <init>()
intf javax.batch.api.listener.JobListener
meth public void afterJob() throws java.lang.Exception
meth public void beforeJob() throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.listener.AbstractStepListener
cons public <init>()
intf javax.batch.api.listener.StepListener
meth public void afterStep() throws java.lang.Exception
meth public void beforeStep() throws java.lang.Exception
supr java.lang.Object

CLSS public abstract interface javax.batch.api.listener.JobListener
meth public abstract void afterJob() throws java.lang.Exception
meth public abstract void beforeJob() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.listener.StepListener
meth public abstract void afterStep() throws java.lang.Exception
meth public abstract void beforeStep() throws java.lang.Exception

CLSS public abstract javax.batch.api.partition.AbstractPartitionAnalyzer
cons public <init>()
intf javax.batch.api.partition.PartitionAnalyzer
meth public void analyzeCollectorData(java.io.Serializable) throws java.lang.Exception
meth public void analyzeStatus(javax.batch.operations.JobOperator$BatchStatus,java.lang.String) throws java.lang.Exception
supr java.lang.Object

CLSS public abstract javax.batch.api.partition.AbstractPartitionReducer
cons public <init>()
intf javax.batch.api.partition.PartitionReducer
meth public void afterPartitionedStepCompletion(javax.batch.api.partition.PartitionReducer$PartitionStatus) throws java.lang.Exception
meth public void beforePartitionedStepCompletion() throws java.lang.Exception
meth public void beginPartitionedStep() throws java.lang.Exception
meth public void rollbackPartitionedStep() throws java.lang.Exception
supr java.lang.Object

CLSS public abstract interface javax.batch.api.partition.PartitionAnalyzer
meth public abstract void analyzeCollectorData(java.io.Serializable) throws java.lang.Exception
meth public abstract void analyzeStatus(javax.batch.operations.JobOperator$BatchStatus,java.lang.String) throws java.lang.Exception

CLSS public abstract interface javax.batch.api.partition.PartitionCollector
meth public abstract java.io.Serializable collectPartitionData() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.partition.PartitionMapper
meth public abstract javax.batch.api.partition.PartitionPlan mapPartitions() throws java.lang.Exception

CLSS public abstract interface javax.batch.api.partition.PartitionPlan
intf java.io.Serializable
meth public abstract boolean getPartitionsOverride()
meth public abstract int getPartitions()
meth public abstract int getThreads()
meth public abstract java.util.Properties[] getPartitionProperties()
meth public abstract void setPartitionProperties(java.util.Properties[])
meth public abstract void setPartitions(int)
meth public abstract void setPartitionsOverride(boolean)
meth public abstract void setThreads(int)

CLSS public javax.batch.api.partition.PartitionPlanImpl
cons public <init>()
intf javax.batch.api.partition.PartitionPlan
meth public boolean getPartitionsOverride()
meth public int getPartitions()
meth public int getThreads()
meth public java.util.Properties[] getPartitionProperties()
meth public void setPartitionProperties(java.util.Properties[])
meth public void setPartitions(int)
meth public void setPartitionsOverride(boolean)
meth public void setThreads(int)
supr java.lang.Object
hfds override,partitionProperties,partitions,threads

CLSS public abstract interface javax.batch.api.partition.PartitionReducer
innr public final static !enum PartitionStatus
meth public abstract void afterPartitionedStepCompletion(javax.batch.api.partition.PartitionReducer$PartitionStatus) throws java.lang.Exception
meth public abstract void beforePartitionedStepCompletion() throws java.lang.Exception
meth public abstract void beginPartitionedStep() throws java.lang.Exception
meth public abstract void rollbackPartitionedStep() throws java.lang.Exception

CLSS public final static !enum javax.batch.api.partition.PartitionReducer$PartitionStatus
 outer javax.batch.api.partition.PartitionReducer
fld public final static javax.batch.api.partition.PartitionReducer$PartitionStatus COMMIT
fld public final static javax.batch.api.partition.PartitionReducer$PartitionStatus ROLLBACK
meth public static javax.batch.api.partition.PartitionReducer$PartitionStatus valueOf(java.lang.String)
meth public static javax.batch.api.partition.PartitionReducer$PartitionStatus[] values()
supr java.lang.Enum<javax.batch.api.partition.PartitionReducer$PartitionStatus>

CLSS public javax.batch.operations.BatchOperationsRuntimeException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
meth public java.lang.String getMessage()
meth public java.lang.Throwable getCause()
meth public void setCause(java.lang.Throwable)
meth public void setMessage(java.lang.String)
supr java.lang.RuntimeException
hfds cause,message,serialVersionUID

CLSS public javax.batch.operations.JobExecutionAlreadyCompleteException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException

CLSS public javax.batch.operations.JobExecutionIsRunningException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException
hfds serialVersionUID

CLSS public javax.batch.operations.JobExecutionNotMostRecentException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException

CLSS public javax.batch.operations.JobExecutionNotRunningException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException
hfds serialVersionUID

CLSS public abstract interface javax.batch.operations.JobOperator
innr public final static !enum BatchStatus
meth public abstract int getJobInstanceCount(java.lang.String) throws javax.batch.operations.JobSecurityException
meth public abstract java.util.List<java.lang.Long> getRunningExecutions(java.lang.String) throws javax.batch.operations.JobSecurityException
meth public abstract java.util.List<javax.batch.runtime.JobExecution> getJobExecutions(javax.batch.runtime.JobInstance) throws javax.batch.operations.JobSecurityException
meth public abstract java.util.List<javax.batch.runtime.JobInstance> getJobInstances(java.lang.String,int,int) throws javax.batch.operations.JobSecurityException
meth public abstract java.util.List<javax.batch.runtime.StepExecution<?>> getStepExecutions(long) throws javax.batch.operations.JobSecurityException
meth public abstract java.util.Properties getParameters(long) throws javax.batch.operations.JobSecurityException
meth public abstract java.util.Set<java.lang.String> getJobNames() throws javax.batch.operations.JobSecurityException
meth public abstract javax.batch.runtime.JobExecution getJobExecution(long) throws javax.batch.operations.JobSecurityException
meth public abstract javax.batch.runtime.JobInstance getJobInstance(long) throws javax.batch.operations.JobSecurityException
meth public abstract long restart(long,java.util.Properties) throws javax.batch.operations.JobRestartException,javax.batch.operations.JobSecurityException
meth public abstract long start(java.lang.String,java.util.Properties) throws javax.batch.operations.JobSecurityException,javax.batch.operations.JobStartException
meth public abstract void abandon(long) throws javax.batch.operations.JobSecurityException
meth public abstract void stop(long) throws javax.batch.operations.JobSecurityException

CLSS public final static !enum javax.batch.operations.JobOperator$BatchStatus
 outer javax.batch.operations.JobOperator
fld public final static javax.batch.operations.JobOperator$BatchStatus ABANDONED
fld public final static javax.batch.operations.JobOperator$BatchStatus COMPLETED
fld public final static javax.batch.operations.JobOperator$BatchStatus FAILED
fld public final static javax.batch.operations.JobOperator$BatchStatus STARTED
fld public final static javax.batch.operations.JobOperator$BatchStatus STARTING
fld public final static javax.batch.operations.JobOperator$BatchStatus STOPPED
fld public final static javax.batch.operations.JobOperator$BatchStatus STOPPING
meth public static javax.batch.operations.JobOperator$BatchStatus valueOf(java.lang.String)
meth public static javax.batch.operations.JobOperator$BatchStatus[] values()
supr java.lang.Enum<javax.batch.operations.JobOperator$BatchStatus>

CLSS public javax.batch.operations.JobRestartException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Exception
hfds serialVersionUID

CLSS public javax.batch.operations.JobSecurityException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Exception
hfds serialVersionUID

CLSS public javax.batch.operations.JobStartException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.String,java.lang.Throwable)
cons public <init>(java.lang.Throwable)
supr java.lang.Exception
hfds serialVersionUID

CLSS public javax.batch.operations.NoSuchJobException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException
hfds serialVersionUID

CLSS public javax.batch.operations.NoSuchJobExecutionException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException
hfds serialVersionUID

CLSS public javax.batch.operations.NoSuchJobInstanceException
cons public <init>()
cons public <init>(java.lang.String)
cons public <init>(java.lang.Throwable)
cons public <init>(java.lang.Throwable,java.lang.String)
supr javax.batch.operations.BatchOperationsRuntimeException
hfds serialVersionUID

CLSS public javax.batch.runtime.BatchRuntime
cons public <init>()
meth public static javax.batch.operations.JobOperator getJobOperator()
supr java.lang.Object
hfds logger,sourceClass

CLSS public abstract interface javax.batch.runtime.JobExecution
meth public abstract java.lang.String getExitStatus()
meth public abstract java.lang.String getJobName()
meth public abstract java.util.Date getCreateTime()
meth public abstract java.util.Date getEndTime()
meth public abstract java.util.Date getLastUpdatedTime()
meth public abstract java.util.Date getStartTime()
meth public abstract java.util.Properties getJobParameters()
meth public abstract javax.batch.operations.JobOperator$BatchStatus getBatchStatus()
meth public abstract long getExecutionId()

CLSS public abstract interface javax.batch.runtime.JobInstance
meth public abstract java.lang.String getJobName()
meth public abstract long getInstanceId()

CLSS public abstract interface javax.batch.runtime.Metric
innr public final static !enum MetricType
meth public abstract javax.batch.runtime.Metric$MetricType getType()
meth public abstract long getValue()

CLSS public final static !enum javax.batch.runtime.Metric$MetricType
 outer javax.batch.runtime.Metric
fld public final static javax.batch.runtime.Metric$MetricType COMMIT_COUNT
fld public final static javax.batch.runtime.Metric$MetricType FILTER_COUNT
fld public final static javax.batch.runtime.Metric$MetricType PROCESS_SKIP_COUNT
fld public final static javax.batch.runtime.Metric$MetricType READ_COUNT
fld public final static javax.batch.runtime.Metric$MetricType READ_SKIP_COUNT
fld public final static javax.batch.runtime.Metric$MetricType ROLLBACK_COUNT
fld public final static javax.batch.runtime.Metric$MetricType WRITE_COUNT
fld public final static javax.batch.runtime.Metric$MetricType WRITE_SKIPCOUNT
meth public static javax.batch.runtime.Metric$MetricType valueOf(java.lang.String)
meth public static javax.batch.runtime.Metric$MetricType[] values()
supr java.lang.Enum<javax.batch.runtime.Metric$MetricType>

CLSS public abstract interface javax.batch.runtime.StepExecution<%0 extends java.io.Serializable>
meth public abstract java.lang.String getExitStatus()
meth public abstract java.lang.String getStepName()
meth public abstract java.lang.String[] getStepContainment()
meth public abstract java.util.Date getEndTime()
meth public abstract java.util.Date getStartTime()
meth public abstract javax.batch.operations.JobOperator$BatchStatus getBatchStatus()
meth public abstract javax.batch.runtime.Metric[] getMetrics()
meth public abstract long getExecutionId()
meth public abstract {javax.batch.runtime.StepExecution%0} getUserPersistentData()

CLSS public abstract interface javax.batch.runtime.context.JobContext<%0 extends java.lang.Object>
meth public abstract java.lang.String getExitStatus()
meth public abstract java.lang.String getJobName()
meth public abstract java.util.Properties getProperties()
meth public abstract javax.batch.operations.JobOperator$BatchStatus getBatchStatus()
meth public abstract long getExecutionId()
meth public abstract long getInstanceId()
meth public abstract void setExitStatus(java.lang.String)
meth public abstract void setTransientUserData({javax.batch.runtime.context.JobContext%0})
meth public abstract {javax.batch.runtime.context.JobContext%0} getTransientUserData()

CLSS public abstract interface javax.batch.runtime.context.StepContext<%0 extends java.lang.Object, %1 extends java.io.Serializable>
meth public abstract java.lang.Exception getException()
meth public abstract java.lang.String getExitStatus()
meth public abstract java.lang.String getStepName()
meth public abstract java.util.Properties getProperties()
meth public abstract javax.batch.operations.JobOperator$BatchStatus getBatchStatus()
meth public abstract javax.batch.runtime.Metric[] getMetrics()
meth public abstract long getStepExecutionId()
meth public abstract void setExitStatus(java.lang.String)
meth public abstract void setPersistentUserData({javax.batch.runtime.context.StepContext%1})
meth public abstract void setTransientUserData({javax.batch.runtime.context.StepContext%0})
meth public abstract {javax.batch.runtime.context.StepContext%0} getTransientUserData()
meth public abstract {javax.batch.runtime.context.StepContext%1} getPersistentUserData()

CLSS public abstract interface !annotation javax.inject.Qualifier
 anno 0 java.lang.annotation.Documented()
 anno 0 java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy value=RUNTIME)
 anno 0 java.lang.annotation.Target(java.lang.annotation.ElementType[] value=[ANNOTATION_TYPE])
intf java.lang.annotation.Annotation

