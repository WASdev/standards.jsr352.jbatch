    /*
     * Appended by build tooling.
     */  
	public String toString() {
	    StringBuffer buf = new StringBuffer(250);
	    String chkAlgStr = checkpointAlgorithm == null ? "null" : checkpointAlgorithm.getRef();
	    buf.append("Chunk: checkpointAlgorithm = " + chkAlgStr);
	    buf.append(", skippableExceptions = " + skippableExceptionClasses);
	    buf.append(", retryableExceptions = " + retryableExceptionClasses);
	    buf.append(", reader = " + reader);
	    buf.append(", processor = " + processor);
	    buf.append(", writer = " + writer);
	    buf.append(", checkpointPolicy = " + checkpointPolicy);
	    buf.append(", commitInterval = " + commitInterval);
	    buf.append(", bufferSize = " + bufferSize);
	    buf.append(", retryLimit = " + retryLimit);
	    buf.append(", skipLimit = " + skipLimit);
	    buf.append("\n");
	    buf.append("Properties = " + com.ibm.batch.xjcl.PropertiesToStringHelper.getString(properties));
	    return buf.toString();
    }