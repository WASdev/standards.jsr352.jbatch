/*
 * Appended by build tooling.
 */

public List<com.ibm.jbatch.container.jsl.TransitionElement> getTransitionElements() {
    return new ArrayList<com.ibm.jbatch.container.jsl.TransitionElement>();
}
    
/*
 * Appended by build tooling.
 */
public String toString() {
	StringBuffer buf = new StringBuffer(100);
	buf.append("Split: id=" + id);
	buf.append("\nContained Flows: \n");
	if (flows == null) {
		buf.append("<none>");
	} else {
		int i = 0;
		for ( Flow f : flows) {
			buf.append("flow[" + i + "]:" + f + "\n");
			i++;
		}
	}
	buf.append("\nnextFromAttribute =" + nextFromAttribute);
	return buf.toString();
}