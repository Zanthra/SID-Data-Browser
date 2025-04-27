package edu.stanford.sid;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.stanford.sid.database.DataFileIndex;
import edu.stanford.sid.database.MonitorInfo;
import edu.stanford.sid.database.MonitorComparators;

/**
 * This servlet modifies the user session to modify the selected monitors.
 */
public class AddMonitorToSessionServlet
extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Start text for the page.
	private final String PAGE_TEXT = "<script language=\"JavaScrip\" type=\"text/javascript\">\n"
    + "opener.location.reload(true);\n"
    + "self.close();\n"
    + "</script>";
	
	/**
	 * This is called when the servlet is loaded by the server.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException
    {
		response.setContentType("text/html");
		response.getWriter().write("<HTML>");
		
		if(request.getParameterValues("monitor") != null)
		{
			HashSet<MonitorInfo> newMonitors = new HashSet<MonitorInfo>();
			
			List<String> toAdd = Arrays.asList(request.getParameterValues("monitor"));
			
			Collection<MonitorInfo> monitors = DataFileIndex.getInstance(this.getServletContext()).getMonitors(MonitorComparators.SITE);
			
			for(MonitorInfo mi : monitors)
			{
				if(toAdd.contains(mi.getIdentifier()))
				{
					newMonitors.add(mi);
				}
			}
			
			if(request.getParameter("submit") != null && 
				request.getParameter("submit").equals("add") && 
				request.getSession().getAttribute("monitors") != null)
				newMonitors.addAll((Collection<MonitorInfo>)request.getSession().getAttribute("monitors"));
			
			request.getSession().setAttribute("monitors", newMonitors);
			
			if(request.getParameter("submit") != null	&&
				request.getParameter("submit").equals("redirect"))
				if(request.getSession().getAttribute("redirect") != null)
					response.sendRedirect(request.getSession().getAttribute("redirect").toString());
				else
					response.sendRedirect("http://sid.stanford.edu/database-browser/browse.jsp");
		}
		else
		{
			if(request.getParameter("submit") == null || !request.getParameter("submit").equals("add"))
			request.getSession().removeAttribute("monitors");
		}
		
		response.getWriter().write(PAGE_TEXT);
		response.getWriter().write("</HTML>");
    }
	
}
