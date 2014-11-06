package com.santi.jaime.TA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletAppMassive extends HttpServlet{

	
	private static final long	serialVersionUID	= 1L;
	private static final String inboxURL = "g3-inbox";
	private static final String outboxURL = "g3-outbox";
	private static final String dummyRequest = "vigo.jpg"; //We use vigo to do the test

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		PrintWriter pw = resp.getWriter();
		resp.setContentType("text/html");
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>TA Assignment 2 Servlet Service</title>");
		pw.println("</head><body bgcolor=\"yellow\">");
		pw.println("<h1>Welcome to the MASSIVE Assignment 2 Servlet!</h1><br /><br />");
		pw.println("<h3>Please enter the number of requests you want to perform:</h3>");
		
		pw.println("<form action='ServletAppMassive' method='post' name='requests'>");
		pw.println("<input type='text' name='number'><br/>");
		pw.println("<input type='submit' value='Submit'>");
		pw.println("</form>");
		
		pw.println("</body></html>");
		
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		long startTime = System.currentTimeMillis();
		String sessionID = req.getSession().getId();
		Integer number = Integer.parseInt(req.getParameter("number"));
		
		ClientApp clientApp = new ClientApp();
		String urlInbox = clientApp.getQueueUrl(inboxURL);
		String urlOutbox = clientApp.getQueueUrl(outboxURL);
		
		File file = new File("/home/ubuntu/images/" + dummyRequest);
		if (!file.exists()) {
			try {
				throw new Exception("Image not found.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		InputStream photoFile = new FileInputStream(file);
		
		//We only need to upload the file once, as it won't be deleted between requests.
		try {
			clientApp.putObjectInBucket(dummyRequest, photoFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < number; i++){
			clientApp.sendMessage(urlInbox, dummyRequest, sessionID, "brighter");
		}
		
		//CLEANING UP
		for(int i = 0; i < number; i++){
			String key = clientApp.receiveMessage(urlOutbox, sessionID);
			clientApp.deleteObjectFromBucket(key);
		}
		long finishTime = System.currentTimeMillis();
		long milliseconds = (finishTime - startTime);
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000*60)) % 60);
		PrintWriter pw = resp.getWriter();
		resp.setContentType("text/html");
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>TA Assignment 2 Servlet Service</title>");
		pw.println("</head><body bgcolor=\"yellow\">");
		pw.println("<h3>All done! Now, check Amazon CloudWatch Alarms to check if everything went OK.</h3><br /><br />");
		pw.println("<h3>Time elapsed: "+minutes +"m "+"and "+seconds+"s</h3><br /><br />");
		pw.println("</body></html>");
	
	}	
}
