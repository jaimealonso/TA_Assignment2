package com.santi.jaime.TA;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

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
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class ClientApp {

	private AWSCredentials credentials;
	private AmazonSQS sqs;
	private Region usWest2;
	private AmazonS3 s3client;
	private String bucketName;
	
	public ClientApp(){
		/*
		 * The ProfileCredentialsProvider will return your [default] credential profile by reading from the credentials file located at (C:\\Users\\Santi\\.aws\\credentials).
		 */
		credentials = null;

		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(e.getMessage());
		}

		sqs = new AmazonSQSClient(credentials);
		usWest2 = Region.getRegion(Regions.EU_WEST_1);
		sqs.setRegion(usWest2);
		s3client = new AmazonS3Client(new ProfileCredentialsProvider());
		bucketName = "g3-bucket-2";
	}
	
	public static void main(String[] args) throws Exception {
		
		ClientApp thisOne = new ClientApp();

		String sqsInbox = thisOne.getQueueUrl("g3-inbox");
		String sqsOutbox = thisOne.getQueueUrl("g3-outbox");;

		String input = "";

		// We ask for user input
		Scanner sc = new Scanner(System.in);
		System.out.println("Please choose an image (Cracovia, Delft, Pontevedra, Vigo): ");
		input = sc.nextLine();
		sc.close();
		String input_lowercase = input.toLowerCase();
		File photoFile = new File("images/" + input_lowercase + ".jpg");
		if (!photoFile.exists()) {
			throw new Exception("Image not found.");
		}

		thisOne.putObjectInBucket(input_lowercase, photoFile);
		
		thisOne.sendMessage(sqsInbox, input_lowercase);

		String fileKey = thisOne.receiveMessage(sqsOutbox);
		S3ObjectInputStream fileContent = thisOne.getObjectFromBucket(fileKey);
		BufferedImage imageFetched = ImageIO.read(fileContent);
		File outPutImage = new File(fileKey + ".jpg");
		ImageIO.write(imageFetched, "jpg", outPutImage);
		System.out.println("All jobs finished");
	}
	
	public String getQueueUrl(String queueName){
		boolean exists = false;
		String queue = "";
		for (String queueUrl : sqs.listQueues().getQueueUrls()) {
			String [] pieces = queueUrl.split("/");
			if (pieces[pieces.length-1].equals(queueName)) {
				exists = true;
				queue = queueUrl;
				System.out.println(queueName+" queue found");
				break;
			}
		}
		if (!exists) {
			// It doesn't exist so we create it
			CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
			queue = sqs.createQueue(createQueueRequest).getQueueUrl();
			System.out.println("Queue created");
		}

		return queue;
	}
	
	
	public String receiveMessage(String queue){
		String body = null;
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queue);
		int message_number = 0;
		List<Message> messages = null;
		do {
			messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			message_number = messages.size();
		} while (message_number == 0);

		// As soon as we get the message we delete it so that the other queue doesn't get it
		String messageRecieptHandle = messages.get(0).getReceiptHandle();
		sqs.deleteMessage(new DeleteMessageRequest(queue, messageRecieptHandle));
		
		body = messages.get(0).getBody();
		
		return body;
		
	}
	
	public int sendMessage(String queueUrl, String body){
		SendMessageRequest message_out = new SendMessageRequest(queueUrl, body);
		sqs.sendMessage(message_out);
		
		return 0;
	}
	
	public int putObjectInBucket(String key, File bucketFile) throws Exception{

		if (!s3client.doesBucketExist(bucketName)) {
			s3client.createBucket(bucketName, com.amazonaws.services.s3.model.Region.EU_Ireland);
		}

		s3client.putObject(bucketName, key, bucketFile);
		
		return 0;
	}
	
	public S3ObjectInputStream getObjectFromBucket(String keyObject) throws Exception{
		S3Object file = s3client.getObject(bucketName, keyObject);
		S3ObjectInputStream fileContent = file.getObjectContent();
		
		return fileContent;
	}

}
