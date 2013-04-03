    /*
     * Appended by build tooling.
     */ 
	public String toString() {
	    StringBuffer buf = new StringBuffer(100);
	    buf.append("Job: id=" + id);
	    buf.append(", restartable=" + restartable);
	    buf.append("\n");
	    buf.append("Properties = " + com.ibm.jbatch.jsl.util.PropertiesToStringHelper.getString(properties));
	    return buf.toString();
    }