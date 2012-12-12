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

import java.util.List;
import java.util.Properties;

import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Property;

import com.ibm.batch.container.modelresolver.PropertyResolver;

public abstract class AbstractPropertyResolver<B> implements
		PropertyResolver<B> {

	// FIXME maybe make this class stateful
	/*
	 * protected Properties submittedProps;
	 * 
	 * protected AbstractPropertyResolver(Properties props) {
	 * this.submittedProps = props; }
	 */

	/*
	 * Convenience method that is the same as calling substituteProperties(job,
	 * null, null)
	 */
	public B substituteProperties(final B b) {

		return this.substituteProperties(b, null, null);
	}

	/*
	 * Convenience method that is the same as calling substituteProperties(job,
	 * submittedProps, null)
	 */
	public B substituteProperties(final B b, final Properties submittedProps) {

		return this.substituteProperties(b, submittedProps, null);

	}

	private enum PROPERTY_TYPE {
		JOB_PARAMETERS, SYSTEM_PROPERTIES, JOB_PROPERTIES
	}

	/**
	 * 
	 * @param elementProperties
	 *            xml properties that are direct children of the current element
	 * @param submittedProps
	 *            submitted job properties
	 * @param parentProps
	 *            resolved parent properties
	 * @return the properties associated with this elements scope
	 */
	protected Properties resolveElementProperties(
			final List<Property> elementProperties,
			final Properties submittedProps, final Properties parentProps) {

		Properties currentXMLProperties = new Properties();

		currentXMLProperties = this.inheritProperties(parentProps, currentXMLProperties);

		for (final Property prop : elementProperties) {
			String name = prop.getName();

			name = this.replaceAllProperties(name, submittedProps, currentXMLProperties);

			String value = prop.getValue();
			value = this.replaceAllProperties(value, submittedProps,currentXMLProperties);

			// add resolved properties to current properties
			currentXMLProperties.setProperty(name, value);

			// update JAXB model
			prop.setName(name);
			prop.setValue(value);
		}
		return currentXMLProperties;

	}

	/**
	 * Replace all the properties in String str.
	 * 
	 * @param str
	 * @param submittedProps
	 * @param xmlProperties
	 * @return
	 */
	protected String replaceAllProperties(String str,
			final Properties submittedProps, final Properties xmlProperties) {

		int startIndex = 0;
		NextProperty nextProp = this.findNextProperty(str, startIndex);
		
		
		while (nextProp != null) {

			// get the start index past this property for the next property in
			// the string
			//startIndex = this.getEndIndexOfNextProperty(str, startIndex);
			startIndex = nextProp.endIndex;

			// resolve the property
			final String nextPropValue = this.resolvePropertyValue(nextProp.propName, nextProp.propType,
					submittedProps, xmlProperties);

			// After we get this value the lenght of the string might change so
			// we need to reset the start index
			int lengthDifference = 0;
			switch(nextProp.propType) {
			
				case JOB_PARAMETERS:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{jobParameters['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{jobParameters['" + nextProp.propName + "']}", nextPropValue);
					break;
				case JOB_PROPERTIES:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{jobProperties['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{jobProperties['" + nextProp.propName + "']}", nextPropValue);
					break;
				case SYSTEM_PROPERTIES:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{systemProperties['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{systemProperties['" + nextProp.propName + "']}", nextPropValue);
					break;

			}

			// find the next property
			nextProp = this.findNextProperty(str, startIndex);
		}

		return str;
	}

	/**
	 * Gets the value of a property using the property type
	 * 
	 * If the property 'propname' is not defined  the String 'null' (without quotes) is returned as the
	 * value
	 * 
	 * @param name
	 * @return
	 */
	private String resolvePropertyValue(final String name, PROPERTY_TYPE propType,
			final Properties submittedProperties, final Properties xmlProperties) {

		String value = null;

		switch(propType) {
		
			case JOB_PARAMETERS:
				if (submittedProperties != null) {
					value = submittedProperties.getProperty(name);
				}
				if (value != null){
					return value;
				}
				break;
			case JOB_PROPERTIES:
				if (xmlProperties != null){
					value = xmlProperties.getProperty(name);
				}
				if (value != null) {
					return value;
				}
				break;
			case SYSTEM_PROPERTIES:
				value = System.getProperty(name);
				if (value != null) {
					return value;
				}
				break;
		}
		
		
		return "null";

	}

	/**
	 * Merge the parent properties that are already set into the child
	 * properties. Child properties always override parent values.
	 * 
	 * @param parentProps
	 *            A set of already resolved parent properties
	 * @param childProps
	 *            A set of already resolved child properties
	 * @return
	 */
	private Properties inheritProperties(final Properties parentProps,
			final Properties childProps) {
		if (parentProps == null) {
			return childProps;
		}

		if (childProps == null) {
			return parentProps;
		}

		for (final String parentKey : parentProps.stringPropertyNames()) {

			// Add the parent property to the child if the child does not
			// already define it
			if (!!!childProps.containsKey(parentKey)) {
				childProps.setProperty(parentKey, parentProps
						.getProperty(parentKey));
			}
		}

		return childProps;

	}

	/**
	 * A helper method to the get the index of the '}' character in the given
	 * String str with a valid property substitution. A valid property looks
	 * like ${batch.property}
	 * 
	 * @param str
	 *            The string to search.
	 * @param startIndex
	 *            The index in str to start the search.
	 * @return The index of the '}' character or -1 if no valid property is
	 *         found in str.
	 */
	private int getEndIndexOfNextProperty(final String str, final int startIndex) {
		if (str == null) {
			return -1;
		}

		final int startPropIndex = str.indexOf("${", startIndex);

		// we didn't find a property in this string
		if (startPropIndex == -1) {
			return -1;
		}

		final int endPropIndex = str.indexOf("}", startPropIndex);
		// This check allows something like this "Some filename is ${}"
		// Maybe we should require "${f}" ???
		if (endPropIndex > startPropIndex) {
			return endPropIndex;
		}

		// if no variables like ${prop1} are in string, return null
		return -1;
	}

	/**
	 * A helper method to the get the next property in the given String str with
	 * a valid property substitution. A valid property looks like
	 * #{jobParameter['batch.property']}. This method will return only the name
	 * of the property found without the surrounding metadata.
	 * 
	 * Example:
	 * 
	 * @param str
	 *            The string to search.
	 * @param startIndex
	 *            The index in str to start the search.
	 * @return The name of the next property found without the starting
	 *         #{<propertyType>[' or ending ']}
	 */
	private NextProperty findNextProperty(final String str, final int startIndex) {

        if (str == null) {
            return null;
        }
        
        
        final int startPropIndex = str.indexOf("#{", startIndex);
        if (startPropIndex == -1) {
        	return null;        	
        }
        
        
        //FIXME We may want to throw a more helpful exception here to say there was probably a typo.
        PROPERTY_TYPE type = null;
        if (str.startsWith("#{jobParameters['", startPropIndex)) {
        	type = PROPERTY_TYPE.JOB_PARAMETERS;
        } else if (str.startsWith("#{systemProperties['", startPropIndex)) {
        	type = PROPERTY_TYPE.SYSTEM_PROPERTIES;
        } else if (str.startsWith("#{jobProperties['", startPropIndex)) {
        	type = PROPERTY_TYPE.JOB_PROPERTIES;
        }
        
        if (type == null) {
        	return null;
        }


        final int endPropIndex = str.indexOf("']}");
        // This check allows something like this "Some filename is ${jobParameters['']}"
        // Maybe we should require "${f}" ???
        
        String propName = null;
        if (endPropIndex > startPropIndex) {
        	
        	if (type.equals(PROPERTY_TYPE.JOB_PARAMETERS)) {
        		propName = str.substring(startPropIndex + 17, endPropIndex);
        	}
        	
        	if (type.equals(PROPERTY_TYPE.JOB_PROPERTIES)) {
        		propName = str.substring(startPropIndex + 17, endPropIndex);
        	}
        	
        	if (type.equals(PROPERTY_TYPE.SYSTEM_PROPERTIES)) {
        		propName = str.substring(startPropIndex + 20, endPropIndex);
        	}
        	
        	return new NextProperty(propName, type, startPropIndex, endPropIndex ) ;
        			
        }

        // if no variables like #{jobProperties['prop1']} are in string, return null
        return null;
    }

	/**
	 * private String replaceProperty(final String input, final String propName,
	 * final String propValue) {
	 * 
	 * final String propertyStr = new
	 * StringBuilder("${").append(propName).append("}").toString();
	 * 
	 * return input.replace(propertyStr, propValue);
	 * 
	 * }
	 */

	/**
	 * Creates a java.util.Properties map from a jsr352.batch.jsl.Properties
	 * object.
	 * 
	 * @param xmlProperties
	 * @return
	 */
	private Properties xmlPropertiesToJavaProperties(
			final JSLProperties xmlProperties) {

		final Properties props = new Properties();

		for (final Property prop : xmlProperties.getPropertyList()) {
			props.setProperty(prop.getName(), prop.getValue());
		}

		return props;

	}
	
	
	class NextProperty {
		
		final String propName;
		final PROPERTY_TYPE propType;
		final int startIndex;
		final int endIndex;
		
		NextProperty(String propName, PROPERTY_TYPE propType, int startIndex, int endIndex){
			this.propName = propName;
			this.propType = propType;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
		
	}
}
