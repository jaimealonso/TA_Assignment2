package com.santi.jaime.TA;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;

import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class ServletApp extends HttpServlet{

	
	private static final long	serialVersionUID	= 1L;
	private final String inboxURL = "g3-inbox";
	private final String outboxURL = "g3-outbox";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String sessionID = req.getSession().getId();
		PrintWriter pw = resp.getWriter();
		resp.setContentType("text/html");
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>TA Assignment 2 Servlet Service</title>");
		pw.println("</head><body bgcolor=\"yellow\">");
		pw.println("<h1>Welcome to our Assignment 2 Servlet!</h1><br /><br />");
		pw.println("<h3>Please select one the following options</h3>");
		
		pw.println("<form enctype='multipart/form-data' action='ServletApp' method='post' name='choice'>");

		pw.println("<input type='radio' name='city' value='cracovia.jpg'>Cracovia<br/>");
		pw.println("<input type='radio' name='city' value='delft.jpg'>Delft<br/>");
		pw.println("<input type='radio' name='city' value='pontevedra.jpg'>Pontevedra<br/>");
		pw.println("<input type='radio' name='city' value='vigo.jpg'>Vigo<br/>");
		pw.println("<input type='hidden' name='sessionid' value='"+sessionID+"'/>");
		pw.println("<h3>Please choose the transformation you want to perform:</h3>");
		pw.println("<input type='radio' name='action' value='brighter'>Brighter<br/>");
		pw.println("<input type='radio' name='action' value='darker'>Darker<br/>");
		pw.println("<input type='radio' name='action' value='black_white'>Black & White<br/>");
		pw.println("<input type='submit' value='Submit'>");
		pw.println("</form>");
		
		pw.println("<h3>You can also upload a file of your own:</h3>");
		pw.println("<form enctype='multipart/form-data' action='ServletApp' method='post' name='file'>");
		pw.println("<input type='file' name='image'/><br/>");
		pw.println("<input type='hidden' name='sessionid' value='"+sessionID+"'/>");
		
		pw.println("<h3>Please choose the transformation you want to perform:</h3>");
		pw.println("<input type='radio' name='action' value='brighter'>Brighter<br/>");
		pw.println("<input type='radio' name='action' value='darker'>Darker<br/>");
		pw.println("<input type='radio' name='action' value='black_white'>Black & White<br/>");

		pw.println("<input type='submit' value='Submit'>");
		pw.println("</form>");
		
		pw.println("</body></html>");
		
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String sessionID = req.getSession().getId();

		PrintWriter pw = resp.getWriter();
		resp.setContentType("text/html");
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>TA Assignment 2 Servlet Service</title>");
		pw.println("</head><body bgcolor=\"yellow\">");
		pw.println("<h1>Welcome to our Assignment 2 Servlet!</h1><br /><br />");
		
		String key = null;
		String action = null;
		InputStream photoFile = null;
		
        try {
			List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
			for(FileItem item : items){
				if(item.isFormField()){
					if("city".equals(item.getFieldName())){
						String input = item.getString();
						key = input.toLowerCase();
						File file = new File("/home/ubuntu/images/" + key);
						if (!file.exists()) {
							try {
								throw new Exception("Image not found.");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						photoFile = new FileInputStream(file);
					}
					else if("action".equals(item.getFieldName())){
						action = item.getString();
					}
				}
				else{
					key = FilenameUtils.getName(item.getName());
					photoFile = item.getInputStream();
				}
			}
        } catch (FileUploadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		
		ClientApp clientApp = new ClientApp();
		String urlInbox = clientApp.getQueueUrl(inboxURL);
		String urlOutbox = clientApp.getQueueUrl(outboxURL);
		


		try {
			clientApp.putObjectInBucket(key, photoFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		clientApp.sendMessage(urlInbox, key, sessionID, action);

		String responseKey = clientApp.receiveMessage(urlOutbox, sessionID);
		S3ObjectInputStream fileContent = null;
		try {
			fileContent = clientApp.getObjectFromBucket(responseKey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedImage imageFetched = ImageIO.read(fileContent);
		String path = "/"+responseKey;
		FileOutputStream outPutImage = new FileOutputStream("/var/lib/tomcat7/webapps/ROOT/"+path);
		String extension = FilenameUtils.getExtension(path);
		ImageIO.write(imageFetched, extension, outPutImage);
		
		pw.println("<img src='"+"/"+responseKey+"'/>");
		pw.println("</body></html>");

		
	}
}