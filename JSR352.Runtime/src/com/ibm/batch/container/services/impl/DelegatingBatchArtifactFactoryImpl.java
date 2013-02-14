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
package com.ibm.batch.container.services.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.IBatchArtifactFactory;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.util.DependencyInjectionUtility;

public class DelegatingBatchArtifactFactoryImpl implements IBatchArtifactFactory, XMLStreamConstants {

	private final static Logger logger = Logger.getLogger(DelegatingBatchArtifactFactoryImpl.class.getName());
	private final static String CLASSNAME = DelegatingBatchArtifactFactoryImpl.class.getName();
	
    protected static IBatchArtifactFactory preferredArtifactFactory = (IBatchArtifactFactory) ServicesManager.getInstance().getService(ServiceType.CONTAINER_ARTIFACT_FACTORY_SERVICE);
  

	// TODO - surface constants
	private final static String BATCH_XML = "META-INF/batch.xml";
	private final static QName BATCH_ROOT_ELEM = new QName("http://jcp.org.batch/jsl", "batch-artifacts");

	// Uses TCCL
	@Override
	public Object load(String batchId) {
        String methodName = "load";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, methodName, "Loading batch artifact id = " + batchId);
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Delegating to preferred artifact factory" + preferredArtifactFactory);
        }

        //If preferred artifact factory is different from this one, use the preferred factory.
        if (preferredArtifactFactory.getClass() != this.getClass()) {
            Object artifact = preferredArtifactFactory.load(batchId);
            if (artifact != null) {
                return artifact;
            }
        }
        
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Preferred artifact factory failed to load artifact " + batchId +". Defaulting to batch.xml.");
        }
        
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("TCCL = " + tccl);
        }

        ArtifactMap artifactMap = initArtifactMapFromClassLoader(tccl);

		Object loadedArtifact = artifactMap.getArtifactById(batchId);

		if (loadedArtifact == null) {
			throw new IllegalArgumentException("Could not load any artifacts with batch id=" + batchId);
		}

		
		DependencyInjectionUtility.injectReferences(loadedArtifact, ProxyFactory.getInjectionReferences());
		
		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(CLASSNAME, methodName, "For batch artifact id = " + batchId + ", loaded artifact instance: " +
					loadedArtifact + " of type: " + loadedArtifact.getClass().getCanonicalName());
		}
		return loadedArtifact;
	}

    private ArtifactMap initArtifactMapFromClassLoader(ClassLoader loader) {

        ArtifactMap artifactMap = new ArtifactMap();
        
        InputStream is = getBatchXMLStreamFromClassLoader(loader);
        artifactMap = populateArtifactMapFromStream(artifactMap, is);
        
        return artifactMap;
    }

	protected InputStream getBatchXMLStreamFromClassLoader(ClassLoader loader) {
		InputStream is = loader.getResourceAsStream(BATCH_XML);

		if (is == null) {
			throw new IllegalStateException("Unable to load batch.xml");
		}

		return is;
	}

	/*
	 * Non-validating (e.g. that the artifact type is correct) load
	 * 
	 * TODO - add some logging to the parsing
	 */
	protected ArtifactMap populateArtifactMapFromStream(ArtifactMap tempMap, InputStream is) {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();


		try {
			XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(is);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Loaded XMLStreamReader = " + xmlStreamReader);
			}

			boolean processedRoot = false;

			// We are going to take advantage of the simplified structure of a
			// line
			// E.g.:
			// <batch-artifacts>
			//   <item-processor id=MyItemProcessor class=jsr352/sample/MyItemProcessorImpl/>
			//   ..
			// </batch-artifacts>
			//
			// and have much simpler logic than general-purpose parsing would
			// require.
			while (xmlStreamReader.hasNext()) {
				int event = xmlStreamReader.next();

				// Until we reach end of document
				if (event == END_DOCUMENT) {
					break;
				}

				// At this point we have either:
				//    A) just passed START_DOCUMENT, and are at START_ELEMENT for the root, 
				//       <batch-artifacts>, or 
				//    B) we have just passed END_ELEMENT for one of the artifacts which is a child of
				//       <batch-artifacts>.
				//   
				//  Only handle START_ELEMENT now so we can skip whitespace CHARACTERS events.
				//
				if (event == START_ELEMENT) {
					if (!processedRoot) {
						QName rootQName = xmlStreamReader.getName();
						if (!rootQName.equals(BATCH_ROOT_ELEM)) {
							throw new IllegalStateException("Expecting document with root element QName: " + BATCH_ROOT_ELEM
									+ ", but found root element with QName: " + rootQName);
						} else {
							processedRoot = true;
						}
					} else {

						// Should only need localName
						String annotationShortName = xmlStreamReader.getLocalName();
						String id = xmlStreamReader.getAttributeValue(null, "id");
						String className = xmlStreamReader.getAttributeValue(null, "class");
						tempMap.addEntry(annotationShortName, id, className);

						// Ignore anything else (text/whitespace) within this
						// element
						while (event != END_ELEMENT) {
							event = xmlStreamReader.next();
						}
					}
				}
			}
			xmlStreamReader.close();
			is.close();
			return tempMap;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private class ArtifactMap {

		private Map<String, Class<?>> idToArtifactClassMap = new HashMap<String, Class<?>>();

		// Maps to a list of types not a single type since there's no reason a single artifact couldn't be annotated
		// with >1 batch artifact annotation type.
		private Map<String, List<String>> idToArtifactTypeListMap = new HashMap<String, List<String>>();

		/*
		 * Init already synchronized, so no need to synch further
		 */
		private void addEntry(String batchTypeName, String id, String className) {
			try {
				if (!idToArtifactClassMap.containsKey(id)) {
					Class<?> artifactClass = Thread.currentThread().getContextClassLoader().loadClass(className);

					idToArtifactClassMap.put(id, artifactClass);
					List<String> typeList = new ArrayList<String>();
					typeList.add(batchTypeName);                    
					idToArtifactTypeListMap.put(id, typeList);                    
				} else {

					Class<?> artifactClass = Thread.currentThread().getContextClassLoader().loadClass(className);

					// Already contains entry for this 'id', let's make sure it's the same Class
					// which thus must implement >1 batch artifact "type" (i.e. contains >1 batch artifact annotation).
					if (!idToArtifactClassMap.get(id).equals(artifactClass)) {
						if (logger.isLoggable(Level.SEVERE)) {
							Class<?> alreadyLoaded = idToArtifactClassMap.get(id); 
							logger.severe("Attempted to load batch artifact with id: " + id + ", and className: " + className + 
									".   Found: " + artifactClass + ", however the artifact id: " + id + 
									" is already associated with: " + alreadyLoaded + ", of className: " +
									alreadyLoaded.getCanonicalName());

						}
						throw new IllegalArgumentException("Already loaded a different class for id = " + id);
					}
					List<String> typeList = idToArtifactTypeListMap.get(id);
					typeList.add(batchTypeName);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Object getArtifactById(String id) {

			Object artifactInstance = null;

			try {
				Class<?> clazz = idToArtifactClassMap.get(id);
				if (clazz != null) {
					artifactInstance = (idToArtifactClassMap.get(id)).newInstance();	
				}
			} catch (IllegalAccessException e) {
				throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + id, e);
			} catch (InstantiationException e) {
				throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + id, e);
			}


			return artifactInstance;
		}

		private List<String> getBatchTypeList(String id) {
			return idToArtifactTypeListMap.get(id);
		}

	}

	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}
	
	
}
