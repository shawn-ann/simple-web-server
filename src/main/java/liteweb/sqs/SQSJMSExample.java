package liteweb.sqs;

import com.amazon.sqs.javamessaging.*;
import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.*;

import javax.jms.*;
import java.net.URI;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQSJMSExample {
    private final SqsClient sqsClient;
    private final SQSConnectionFactory connectionFactory;
    private final String queueName = "example.fifo";

    public SQSJMSExample() {
        sqsClient = SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of("us-east-1"))
                .build();
        connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), sqsClient);
    }


    public void sendMessage(String messageBody) throws JMSException, jakarta.jms.JMSException {
        SQSConnection connection = (SQSConnection) connectionFactory.createConnection();
        SQSSession session = (SQSSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        SQSMessageProducer producer = (SQSMessageProducer) session.createProducer(session.createQueue(queueName));
        SQSTextMessage message = new SQSTextMessage();
        String currentTimeMillis = String.valueOf(System.currentTimeMillis());
        message.setText(currentTimeMillis);
        message.setStringProperty("JMSXGroupID", "Default");
        producer.send(message);

        producer.close();
        session.close();
        connection.close();
    }

    public void receiveAndAcknowledgeMessages() throws JMSException, jakarta.jms.JMSException {
        SQSConnection connection = (SQSConnection) connectionFactory.createConnection();
        SQSSession session = (SQSSession) connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        SQSMessageConsumer consumer = (SQSMessageConsumer) session.createConsumer(session.createQueue(queueName));
        connection.start();

        while (true) {
            System.out.println("receive...");
            SQSTextMessage message = (SQSTextMessage) consumer.receive(2000);
            if (message != null) {
                System.out.println("Received message: " + message.getText());
                processMessage(message);
                message.acknowledge();
            } else {
                System.out.println("No message received");
                break;
            }
        }

        consumer.close();
        session.close();
        connection.close();
    }

    private void processMessage(SQSTextMessage message) throws JMSException, jakarta.jms.JMSException {
        try {
            // 模拟处理消息的过程
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        SQSJMSExample example = new SQSJMSExample();
        String messageBodyPrefix = "Message ";

        // 发送10条消息
        for (int i = 1; i <= 10; i++) {
            String messageBody = messageBodyPrefix + i;
            example.sendMessage(messageBody);
        }
        System.out.println("send done");
        // 接收并确认消息
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (int i = 1; i <= 10; i++) {
            executorService.submit(() -> {
                try {
                    example.receiveAndAcknowledgeMessages();
                } catch (Exception e) {
                    System.err.println("Error");
                }
            });
        }
        executorService.shutdown();
    }
}