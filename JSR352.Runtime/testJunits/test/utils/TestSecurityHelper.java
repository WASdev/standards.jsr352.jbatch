package test.utils;

import com.ibm.jbatch.spi.BatchSecurityHelper;

public class TestSecurityHelper implements BatchSecurityHelper {

	public final static String defaultTag = "internal.default.tag.for.TestSecurityHelper";
	
	private String currentTag;
	private boolean isAdmin = false;
	
	public TestSecurityHelper(boolean isAdmin, String currentTag) {
		this.isAdmin = isAdmin;
		this.currentTag = currentTag;
	}
	
	public TestSecurityHelper(String currentTag) {
		this(false, currentTag);
	}
	
	@Override
	public String getCurrentTag() {
		if (currentTag == null) {
			return defaultTag;
		} else {
			return currentTag;
		}
	}

	@Override
	public boolean isAdmin(String tag) {
		return isAdmin;
	}

}
