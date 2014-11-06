package com.santi.jaime.TA;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
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
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class ClientApp {

	private AWSCredentials	credentials;
	private AmazonSQS		sqs;
	private Region			usWest2;
	private AmazonS3		s3client;
	private static final String	bucketName	= "g3-bucket-2";

	public ClientApp() {

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
	}

	public static void main(String[] args) throws Exception {

		String sessionID = String.valueOf(new Date().getTime());
		ClientApp thisOne = new ClientApp();

		String sqsInbox = thisOne.getQueueUrl("g3-inbox");
		String sqsOutbox = thisOne.getQueueUrl("g3-outbox");
		;

		String input = "";
		String input2 = "";

		// We ask for user input
		Scanner sc = new Scanner(System.in);
		System.out.println("Please choose an image (Cracovia, Delft, Pontevedra, Vigo): ");
		input = sc.nextLine();
		System.out.println("Please choose the action you want to perform (brighter, darker, black_white): ");
		input2 = sc.nextLine();
		sc.close();
		String input_lowercase = input.toLowerCase();
		String input_lowercase2 = input2.toLowerCase();
		File photoFile = new File("images/" + input_lowercase + ".jpg");
		if (!photoFile.exists()) {
			throw new Exception("Image not found.");
		}

		InputStream is = new FileInputStream(photoFile);

		thisOne.putObjectInBucket(input_lowercase, is);
		thisOne.sendMessage(sqsInbox, input_lowercase, sessionID, input_lowercase2);

		String fileKey = thisOne.receiveMessage(sqsOutbox, sessionID);
		S3ObjectInputStream fileContent = thisOne.getObjectFromBucket(fileKey);
		BufferedImage imageFetched = ImageIO.read(fileContent);
		File outPutImage = new File(fileKey + ".jpg");
		ImageIO.write(imageFetched, "jpg", outPutImage);
		System.out.println("All jobs finished");
	}

	public void deleteObjectFromBucket(String key) {
		s3client.deleteObject(bucketName, key);
	}

	public String getQueueUrl(String queueName) {
		boolean exists = false;
		String queue = "";
		for (String queueUrl : sqs.listQueues().getQueueUrls()) {
			String[] pieces = queueUrl.split("/");
			if (pieces[pieces.length - 1].equals(queueName)) {
				exists = true;
				queue = queueUrl;
				System.out.println(queueName + " queue found");
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

	public String receiveMessage(String queue, String sessionID) {
		String body = null;
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queue).withMessageAttributeNames("sessionID");
		List<Message> messages = null;
		boolean sessionID_found = false;
		do {
			messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			if (messages.size() > 0) {
				for (Message m : messages) {
					String sessionIDAttribute = m.getMessageAttributes().get("sessionID").getStringValue();
					if (sessionID_found = sessionID.equals(sessionIDAttribute)) {
						String messageRecieptHandle = messages.get(0).getReceiptHandle();
						sqs.deleteMessage(new DeleteMessageRequest(queue, messageRecieptHandle));

						body = messages.get(0).getBody();
						return body;
					}
				}
			}
		} while (!sessionID_found);

		// It will never get to this point; we should refactor this in some way...
		return null;
	}

	public int sendMessage(String queueUrl, String body, String sessionID, String action) {
		MessageAttributeValue sessionIDAttribute = new MessageAttributeValue();
		MessageAttributeValue actionAttribute = new MessageAttributeValue();

		sessionIDAttribute.setStringValue(sessionID);
		sessionIDAttribute.setDataType("String");
		actionAttribute.setStringValue(action);
		actionAttribute.setDataType("String");

		SendMessageRequest message_out = new SendMessageRequest(queueUrl, body).addMessageAttributesEntry("sessionID", sessionIDAttribute).addMessageAttributesEntry("action", actionAttribute);
		sqs.sendMessage(message_out);

		return 0;
	}

	public int putObjectInBucket(String key, InputStream bucketFile) throws Exception {

		if (!s3client.doesBucketExist(bucketName)) {
			s3client.createBucket(bucketName, com.amazonaws.services.s3.model.Region.EU_Ireland);
		}

		s3client.putObject(bucketName, key, bucketFile, null);

		return 0;
	}

	public S3ObjectInputStream getObjectFromBucket(String keyObject) throws Exception {
		S3Object file = s3client.getObject(bucketName, keyObject);
		S3ObjectInputStream fileContent = file.getObjectContent();

		return fileContent;
	}

}
