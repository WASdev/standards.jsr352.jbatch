    /*
     * Appended by build tooling.
     */
	public String toString() {
	    StringBuffer buf = new StringBuffer(40);
	    buf.append("Stop: restart =" + restart + ", on = " + on + ", exit-status = " + exitStatus);
	    return buf.toString();
    }