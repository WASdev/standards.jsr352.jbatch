package com.ibm.batch.container.modelresolver.impl;

import java.util.Properties;

import jsr352.batch.jsl.ItemReader;

public class ItemReaderPropertyResolverImpl extends AbstractPropertyResolver<ItemReader> {


	public ItemReaderPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}


	@Override
	public ItemReader substituteProperties(ItemReader reader,
			Properties submittedProps, Properties parentProps) {

        //resolve all the properties used in attributes and update the JAXB model
		reader.setRef(this.replaceAllProperties(reader.getRef(), submittedProps, parentProps));

        // Resolve all the properties defined for this artifact
        if (reader.getProperties() != null) {
            this.resolveElementProperties(reader.getProperties().getPropertyList(), submittedProps, parentProps);
        }

        return reader;
		
	}

}
