/*
 * Appended by build tooling.
 */
public String toString() {
	StringBuffer buf = new StringBuffer(100);
	buf.append("Flow: id=" + id);
	buf.append("\nExecution elements: \n");
	if (executionElements == null) {
		buf.append("<none>");
	} else {
		int i = 0;
		for ( com.ibm.jbatch.container.jsl.ExecutionElement e : executionElements) {
			buf.append("element[" + i + "]:" + e + "\n");
			i++;
		}
	}
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
	buf.append("\n");
	return buf.toString();
}