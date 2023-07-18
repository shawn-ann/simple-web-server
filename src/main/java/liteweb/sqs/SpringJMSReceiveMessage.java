package liteweb.sqs;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSMessagingClientConstants;
import com.amazon.sqs.javamessaging.acknowledge.AcknowledgeMode;
import com.amazon.sqs.javamessaging.message.SQSMessage;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class SpringJMSReceiveMessage {
    private final String queueName = "example.fifo";
    private final String serviceEndpoint = "http://localhost:4566";
    private final String queueURL = serviceEndpoint + "/000000000000/" + queueName;
    private final AmazonSQSAsync sqsClient = AmazonSQSAsyncClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, "us-east-1"))
            .build();
    ;
    private final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), sqsClient);


    public static void main(String[] args) throws Exception {
        SpringJMSReceiveMessage example = new SpringJMSReceiveMessage();
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        for (int i = 0; i < 5; i++) {
//            example.receive2();
//        }
        example.receive();
//        example.receive2();
    }

    public void receive() throws InterruptedException {
        MessageListener messageListener = new ReceiverCallback();
        DefaultMessageListenerContainer listenerContainer = new DefaultMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.setDestinationName(queueName);

        listenerContainer.setSessionTransacted(false);
        listenerContainer.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        listenerContainer.setMessageListener(messageListener);
        listenerContainer.setConcurrentConsumers(5); // 设置并发消费者线程数
        listenerContainer.setAutoStartup(true);
        listenerContainer.initialize();
        listenerContainer.start();
        System.out.println("start receive");
//        CountDownLatch latch = new CountDownLatch(1);
//        latch.await();
    }

    public static class ReceiverCallback implements MessageListener {
        // Used to listen for message silence
        private final Map<String, List<String>> messageGroups = new ConcurrentHashMap<>();

        @Override
        public void onMessage(Message message) {
            try {
                String groupId = message.getStringProperty("JMSXGroupID");
                String messageId = message.getJMSMessageID();
                List<String> messageIds = messageGroups.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>());
                if (messageIds.isEmpty() || messageIds.contains(messageId)) {
                    messageIds.add(messageId);
                    System.out.println(String.format("%s %s receive,Group ID: %s",
                            Thread.currentThread().getName(),
                            System.currentTimeMillis(), groupId));
                    Thread.sleep(10000);
                    message.acknowledge();
                } else {
                    System.out.println("Skipping message: " + messageId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
