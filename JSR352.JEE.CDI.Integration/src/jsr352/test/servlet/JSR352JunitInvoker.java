package jsr352.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Servlet implementation class JSR352JunitInvoker
 */
@WebServlet("/JSR352JunitInvoker")
public class JSR352JunitInvoker extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public JSR352JunitInvoker() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    PrintWriter out = response.getWriter();
	    out.println("Starting JSR352 Junit Run");
		
	    Result result = JUnitCore.runClasses(jsr352.tck.tests.JSR352JUnitTestSuite.class);
	    
	    out.println("FailureCount: " + result.getFailureCount());
	    out.println("IgnoreCount: " + result.getIgnoreCount());
	    out.println("RunCount: " + result.getRunCount());
	    out.println("RunTime (ms): " + result.getRunTime());
	    
	    List<Failure> fails = result.getFailures();
	    
	    for (Failure fail : fails) {
	    	out.println(fail.getTestHeader());
	    	out.println(fail.getMessage());
	    	out.println(fail.getTrace());
	    	out.println();
	    	
	    }
	    
	    

	    
	    
	    out.println("End JSR352 Junit Run");
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
