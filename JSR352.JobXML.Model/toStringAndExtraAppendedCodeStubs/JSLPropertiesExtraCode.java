    /*
     * Appended by build tooling.
     */
	public String toString() {
	    StringBuffer buf = new StringBuffer(140);
	    buf.append("JSL Properties: ");
	    List<Property> propList = getPropertyList();
	    if (propList.size() == 0) {
	    	buf.append("<no properties>");
	    } else {
	    	for (Property p : propList) {
	    		buf.append(p.toString() + "\n");
	    	}
	    }
	    return buf.toString();
    }