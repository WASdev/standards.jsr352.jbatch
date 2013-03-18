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