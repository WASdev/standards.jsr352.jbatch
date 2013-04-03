    /*
     * Appended by build tooling.
     */
	public String toString() {
	    StringBuffer buf = new StringBuffer(100);
	    buf.append("Step: id=" + id);
	    buf.append(", startLimit=" + startLimit);
	    buf.append(", allowStartIfComplete=" + allowStartIfComplete);
	    buf.append("\nnextFromAttribute =" + nextFromAttribute);
		buf.append("\nTransition elements: \n");
		if (transitionElements == null) {
			buf.append("<none>");
		} else {
			int j = 0;
			for ( com.ibm.jbatch.container.jsl.TransitionElement e : transitionElements) {
				buf.append("transition element[" + j + "]:" + e + "\n");
				j++;
			}
		}
	    buf.append("\nProperties = " + com.ibm.jbatch.jsl.util.PropertiesToStringHelper.getString(properties));
	    buf.append("\n");
	    if (batchlet != null) {
	    	buf.append("Contains batchlet=" + batchlet);
	    }
	    if (chunk != null) {
	    	buf.append("Contains chunk=" + chunk);
	    }
	    return buf.toString();
    }