package com.santi.jaime.TA;

import java.io.File;
import java.util.Scanner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class ClientApp {

	public static void main(String[] args) throws Exception {
		/*
		 * The ProfileCredentialsProvider will return your [default] credential profile by reading from the credentials file located at (C:\\Users\\Santi\\.aws\\credentials).
		 */
		AWSCredentials credentials = null;

		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(e.getMessage());
		}

		AmazonSQS sqs = new AmazonSQSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.EU_WEST_1);
		sqs.setRegion(usWest2);
		AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());        
		String bucketName = "success-bucket-2";

		// Now we look for our queue inbox

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
			System.out.println("Queue created");
		}
		
		String input = "";

		// We ask for user input
		Scanner sc = new Scanner(System.in);
		System.out.println("Please choose an image (Cracovia, Delft, Pontevedra, Vigo): ");
		input = sc.nextLine();
		sc.close();
		String input_lowercase = input.toLowerCase();
		File photoFile = new File("images/"+input_lowercase+".jpg");
		if(!photoFile.exists()){
			throw new Exception("Image not found.");
		}
		
		if(!s3client.doesBucketExist(bucketName)){
			s3client.createBucket(bucketName, com.amazonaws.services.s3.model.Region.EU_Ireland);
		}
		
		s3client.putObject(bucketName, input_lowercase, photoFile);

		sqs.sendMessage(new SendMessageRequest(sqsInbox, input_lowercase));
	}

}
