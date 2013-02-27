    /*
     * Appended by build tooling.
     */
	public String toString() {
	    StringBuffer buf = new StringBuffer(100);
	    buf.append("Step: id=" + id);
	    buf.append(", startLimit=" + startLimit);
	    buf.append(", allowStartIfComplete=" + allowStartIfComplete);
	    buf.append("\n");
	    buf.append("Properties = " + com.ibm.jbatch.jsl.util.PropertiesToStringHelper.getString(properties));
	    buf.append("\n");
	    if (batchlet != null) {
	    	buf.append("Contains batchlet=" + batchlet);
	    }
	    if (chunk != null) {
	    	buf.append("Contains chunk=" + chunk);
	    }
	    return buf.toString();
    }