package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.jbatch.jsl.model.ItemProcessor;


public class ItemProcessorPropertyResolverImpl extends AbstractPropertyResolver<ItemProcessor> {
	
	
	public ItemProcessorPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ItemProcessor substituteProperties(ItemProcessor processor,
			Properties submittedProps, Properties parentProps) {

        //resolve all the properties used in attributes and update the JAXB model
		processor.setRef(this.replaceAllProperties(processor.getRef(), submittedProps, parentProps));

        // Resolve all the properties defined for this artifact
        if (processor.getProperties() != null) {
            this.resolveElementProperties(processor.getProperties().getPropertyList(), submittedProps, parentProps);
        }

        return processor;
		
	}

}
