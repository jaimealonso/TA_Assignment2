package com.santi.jaime.TA;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class ServletApp extends HttpServlet{

	
	private static final long	serialVersionUID	= 1L;
	private final String inboxURL = "g3-inbox";
	private final String outboxURL = "g3-outbox";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		PrintWriter pw = resp.getWriter();
		resp.setContentType("text/html");
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>TA Assignment 2 Servlet Service</title>");
		pw.println("</head><body bgcolor=\"yellow\">");
		pw.println("<h1>Welcome to our Assignment 2 Servlet!</h1><br /><br />");
		pw.println("<h3>Please select one the following options</h3>");
		
		pw.println("<form action='ServletApp' method='post' name='choice'>");

		pw.println("<input type='radio' name='city' value='Cracovia'>Cracovia<br/>");
		pw.println("<input type='radio' name='city' value='Delft'>Delft<br/>");
		pw.println("<input type='radio' name='city' value='Pontevedra'>Pontevedra<br/>");
		pw.println("<input type='radio' name='city' value='Vigo'>Vigo<br/>");
		pw.println("<input type='submit' value='Submit'>");
		pw.println("</form>");
		pw.println("</body></html>");
		
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		PrintWriter pw = resp.getWriter();
		resp.setContentType("text/html");
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>TA Assignment 2 Servlet Service</title>");
		pw.println("</head><body bgcolor=\"yellow\">");
		pw.println("<h1>Welcome to our Assignment 2 Servlet!</h1><br /><br />");
		String key = req.getParameter("city");
		ClientApp clientApp = new ClientApp();
		String urlInbox = clientApp.getQueueUrl(inboxURL);
		String urlOutbox = clientApp.getQueueUrl(outboxURL);
		
		String input_lowercase = key.toLowerCase();
		File photoFile = new File("/home/ubuntu/images/" + input_lowercase + ".jpg");
		if (!photoFile.exists()) {
			try {
				throw new Exception("Image not found.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			clientApp.putObjectInBucket(input_lowercase, photoFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		clientApp.sendMessage(urlInbox, input_lowercase);

		String responseKey = clientApp.receiveMessage(urlOutbox);
		S3ObjectInputStream fileContent = null;
		try {
			fileContent = clientApp.getObjectFromBucket(responseKey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedImage imageFetched = ImageIO.read(fileContent);
		String path = "/"+responseKey + ".jpg";
		FileOutputStream outPutImage = new FileOutputStream("/var/lib/tomcat7/webapps/ROOT/"+path);
		ImageIO.write(imageFetched, "jpg", outPutImage);
		
		pw.println("<img src='"+"/"+responseKey+".jpg"+"'/>");
		pw.println("</body></html>");

		
	}
}