package liteweb.sqs;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSMessagingClientConstants;
import com.amazon.sqs.javamessaging.message.SQSMessage;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SQSJMSExample {
    private final String queueName = "example.fifo";
    private final String serviceEndpoint = "http://localhost:4566";
    private final String queueURL = serviceEndpoint + "/000000000000/" + queueName;
    private final AmazonSQSAsync sqsClient = AmazonSQSAsyncClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, "us-east-1"))
            .build();;
    private final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), sqsClient);

    public void sendMessage(String messageBody) throws JMSException {
        String messageGroupId = "Default";
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
        messageAttributeValue.setDataType(SQSMessagingClientConstants.STRING);
        messageAttributeValue.setStringValue(SQSMessage.OBJECT_MESSAGE_TYPE);
        messageAttributes.put(SQSMessage.JMS_SQS_MESSAGE_TYPE, messageAttributeValue);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueURL)
                .withMessageGroupId(messageGroupId)
                .withMessageBody(messageBody)
                .withMessageDeduplicationId(messageBody)
                .withMessageAttributes(messageAttributes);
        sqsClient.sendMessage(sendMessageRequest);
    }

    public static void main(String[] args) throws Exception {
        SQSJMSExample example = new SQSJMSExample();
//         发送10条消息
        for (int i = 1; i <= 10; i++) {
            String messageBody = String.valueOf(System.currentTimeMillis());
            example.sendMessage(messageBody);
        }
        System.out.println("send done");
//         接收并确认消息
        example.receive();
    }

    public void receive() throws InterruptedException {
        MessageListener messageListener = new MessageListenerAdapter(new MyListener());
        DefaultMessageListenerContainer listenerContainer = new DefaultMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.setDestinationName(queueName); // 替换为您的队列名称
        listenerContainer.setMessageListener(messageListener);
        listenerContainer.setConcurrentConsumers(5);
        listenerContainer.start();
        TimeUnit.MINUTES.sleep(5);
    }
    public class MyListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            try {
                System.out.println("receive");
                    message.acknowledge();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }
}