package jsr352.tck.utils;

public class AssertionUtils {
	
	static public void assertWithMessage(String message, Object arg1, Object arg2)
	{
		
		boolean result = arg1.equals(arg2);
		
		if(!result)
		{
			if (message == null)
	            throw new AssertionError();
			else
				throw new AssertionError(message);
		}
	}
	
	static public void assertWithMessage(String message, boolean result)
	{
		if(!result)
		{
			if (message == null)
	            throw new AssertionError();
			else
				throw new AssertionError(message);
		}
	}
	
	static public void assertWithMessage(String message, int arg1, int arg2) {
     boolean result = (arg1 == arg2);
		
		if(!result)
		{
			if (message == null)
	            throw new AssertionError();
			else
				throw new AssertionError(message);
		}
    }
}
