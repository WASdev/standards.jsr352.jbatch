package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.jbatch.jsl.model.ItemWriter;


public class ItemWriterPropertyResolverImpl extends AbstractPropertyResolver<ItemWriter> {

	public ItemWriterPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
	public ItemWriter substituteProperties(ItemWriter writer,
			Properties submittedProps, Properties parentProps) {

        //resolve all the properties used in attributes and update the JAXB model
		writer.setRef(this.replaceAllProperties(writer.getRef(), submittedProps, parentProps));

        // Resolve all the properties defined for this artifact
        if (writer.getProperties() != null) {
            this.resolveElementProperties(writer.getProperties().getPropertyList(), submittedProps, parentProps);
        }

        return writer;
		
	}

}
