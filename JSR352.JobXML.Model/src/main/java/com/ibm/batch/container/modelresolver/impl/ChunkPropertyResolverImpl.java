/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.batch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.batch.container.modelresolver.PropertyResolverFactory;

import jsr352.batch.jsl.Chunk;

public class ChunkPropertyResolverImpl extends AbstractPropertyResolver<Chunk> {



    @Override
    public Chunk substituteProperties(final Chunk chunk, final Properties submittedProps, final Properties parentProps) {
        /*
        <xs:attribute name="reader" use="required" type="xs:string"/>
        <xs:attribute name="processor" use="required" type="xs:string"/>
        <xs:attribute name="writer" use="required" type="xs:string"/>
        <xs:attribute name="checkpoint-policy" use="optional" type="xs:string" default="item" />
        <xs:attribute name="commit-interval" use="optional" type="xs:string" default="10"/>
        <xs:attribute name="buffer-reads" use="optional" type="xs:string" default="true"/>
        <xs:attribute name="chunk-size" use="required" type="xs:string" />
        <xs:attribute name="skip-limit" use="optional" type="xs:string"/>
        <xs:attribute name="retry-limit" use="optional" type="xs:string"/>
        */
        
        //resolve all the properties used in attributes and update the JAXB model
        chunk.setReader(this.replaceAllProperties(chunk.getReader(), submittedProps, parentProps));
        chunk.setProcessor(this.replaceAllProperties(chunk.getProcessor(), submittedProps, parentProps));
        chunk.setWriter(this.replaceAllProperties(chunk.getWriter(), submittedProps, parentProps));
        chunk.setCheckpointPolicy(this.replaceAllProperties(chunk.getCheckpointPolicy(), submittedProps, parentProps));
        chunk.setCommitInterval(this.replaceAllProperties(chunk.getCommitInterval(), submittedProps, parentProps));
        chunk.setBufferSize(this.replaceAllProperties(chunk.getBufferSize(), submittedProps, parentProps));
        chunk.setSkipLimit(this.replaceAllProperties(chunk.getSkipLimit(), submittedProps, parentProps));
        chunk.setRetryLimit(this.replaceAllProperties(chunk.getRetryLimit(), submittedProps, parentProps));

        // Resolve all the properties defined for this chunk
        if (chunk.getProperties() != null) {
            this.resolveElementProperties(chunk.getProperties().getPropertyList(), submittedProps, parentProps);
        }
        
        // Resolve CheckpointAlgorithm properties
        if (chunk.getCheckpointAlgorithm() != null) {
            PropertyResolverFactory.createCheckpointAlgorithmPropertyResolver().substituteProperties(chunk.getCheckpointAlgorithm(), submittedProps, parentProps);
        }

        return chunk;

    }

}
