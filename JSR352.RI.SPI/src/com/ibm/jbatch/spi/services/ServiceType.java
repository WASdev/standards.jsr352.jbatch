package com.ibm.jbatch.spi.services;

/**
 * 
 * Note that the batch runtime is using a common "namespace" (in the form of this enum)
 * for both internal services and the more-pluggable SPI services whose interfaces
 * are defined in this package, com.ibm.jbatch.spi.services.
 * Additionally, there is an entry here for the TCK SPI (CallbackManager).
 * 
 * @see com.ibm.jbatch.container.services
 * @see com.ibm.jbatch.tck.spi
 *
 */
public enum ServiceType {
	TRANSACTION_SERVICE,
	PERSISTENCE_MANAGEMENT_SERVICE,
	JOB_STATUS_MANAGEMENT_SERVICE,
	BATCH_THREADPOOL_SERVICE,
	CONTAINER_ARTIFACT_FACTORY_SERVICE,
	DELEGATING_ARTIFACT_FACTORY_SERVICE,
	BATCH_KERNEL_SERVICE,
	JOB_ID_MANAGEMENT_SERVICE,
	CALLBACK_SERVICE,
	JAVA_EDITION_IS_SE_DUMMY_SERVICE,
	DELEGATING_JOBXML_LOADER_SERVICE,
	JOBXML_LOADER_SERVICE
}
