    /*
     * Appended by build tooling.
     */  
	public String toString() {
	    StringBuffer buf = new StringBuffer(100);
	    buf.append("Batchlet: ref=" + ref);
	    buf.append("\n");
	    buf.append("Properties = " + com.ibm.batch.xjcl.PropertiesToStringHelper.getString(properties));
	    return buf.toString();
    }
