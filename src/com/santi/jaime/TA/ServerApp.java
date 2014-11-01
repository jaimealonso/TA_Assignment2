package com.santi.jaime.TA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class ServerApp {

	public static void main(String[] args) throws Exception {
		/*
		 * The ProfileCredentialsProvider will return your [default] credential profile by reading from the credentials file located at (C:\\Users\\Santi\\.aws\\credentials).
		 */
		AWSCredentials credentials = null;

		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
			// credentials = new InstanceProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(e.getMessage());
		}

		AmazonSQS sqs = new AmazonSQSClient(credentials);
		Region euWest1 = Region.getRegion(Regions.EU_WEST_1);
		sqs.setRegion(euWest1);
		AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());        
		String bucketName = "g3-bucket-2";
		
		// We attemp to receive the request, first of all we check if the outbox queue is already created
		boolean exists = false;
		String sqsInbox = "";
		for (String queueUrl : sqs.listQueues().getQueueUrls()) {
			String [] pieces = queueUrl.split("/");
			if (pieces[pieces.length-1].equals("g3-inbox")) {
				exists = true;
				sqsInbox = queueUrl;
				System.out.println("Inbox queue found");
				break;
			}
		}
		if (!exists) {
			// It doesn't exist so we create it
			CreateQueueRequest createQueueRequest = new CreateQueueRequest("g3-inbox");
			sqsInbox = sqs.createQueue(createQueueRequest).getQueueUrl();
			System.out.println("Queue created, beginning work!");
		}
		
		if(!s3client.doesBucketExist(bucketName)){
			s3client.createBucket(bucketName, com.amazonaws.services.s3.model.Region.EU_Ireland);
		}
		
		while (true) {
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(sqsInbox);
			int message_number = 0;
			List<Message> messages = null;
			do {
				messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
				message_number = messages.size();
			} while (message_number == 0);
			
			// As soon as we get the message we delete it so that the other queue doesn't get it
			String messageRecieptHandle = messages.get(0).getReceiptHandle();
			sqs.deleteMessage(new DeleteMessageRequest(sqsInbox, messageRecieptHandle));
			
			String fileKey = messages.get(0).getBody();
			S3Object file = s3client.getObject(bucketName, fileKey);
			S3ObjectInputStream fileContent = file.getObjectContent();
			FileOutputStream newFile = new FileOutputStream(fileKey+".jpg");
			
		      byte[] buffer = new byte[4096]; 
		      int bytes_read;

		      while ((bytes_read = fileContent.read(buffer)) != -1)
		        newFile.write(buffer, 0, bytes_read);
		      
		      System.out.println("File written :D");
			
		}
		
		
	}

}
