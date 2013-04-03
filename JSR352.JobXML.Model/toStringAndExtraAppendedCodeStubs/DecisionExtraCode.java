    /*
     * Appended by build tooling.
     */
	public String toString() {
	    StringBuffer buf = new StringBuffer(100);
	    buf.append("Decision: id=" + id);
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
	    buf.append("Contains decider =" + ref);
	    return buf.toString();
    }