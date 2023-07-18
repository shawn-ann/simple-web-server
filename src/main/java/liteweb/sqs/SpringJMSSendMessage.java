package liteweb.sqs;

import com.amazon.sqs.javamessaging.*;
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
import java.util.Map;

public class SpringJMSSendMessage {
    private final String queueName = "example.fifo";
    private final String serviceEndpoint = "http://localhost:4566";
    private final String queueURL = serviceEndpoint + "/000000000000/" + queueName;
    private final AmazonSQSAsync sqsClient = AmazonSQSAsyncClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, "us-east-1"))
            .build();

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
        SpringJMSSendMessage example = new SpringJMSSendMessage();
//         发送10条消息
        while (true){
            Thread.sleep(2000);
            for (int i = 1; i <= 2; i++) {
                String messageBody = String.valueOf(System.currentTimeMillis());
                example.sendMessage(messageBody);
            }
        }
    }
}
