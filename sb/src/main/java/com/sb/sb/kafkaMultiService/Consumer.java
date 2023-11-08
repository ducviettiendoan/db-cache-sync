package com.sb.sb.kafkaMultiService;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
// import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.sb.sb.redis.SDObject;
import com.sb.sb.redis.StudentRedisConfig;
import com.sb.sb.student.Student;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Service
public class Consumer {
    @Autowired
    private SDObject sdo;

    private String STRING_KEY_PREFIX = "redi2read:strings:student";

    @Autowired
    //Qualifier make sure template choosing is correct
    @Qualifier("redisTemplate")
	private RedisTemplate<String,String> template;

    private void processMessage(Student messageStudent) throws IOException , ClassNotFoundException{
        String serializeS = sdo.toString(messageStudent);
        //put sleep() to test manual commit
        template.opsForHash().put(STRING_KEY_PREFIX,"key"+messageStudent.getId(),serializeS);
        System.out.println("Kafka Consumer writes key to Redis "+messageStudent+" complete");
    }

	@Async
	// @EventListener
    public CompletableFuture<Consumer> run() throws IOException, ClassNotFoundException{

        //Setup Properties for consumer
        Properties kafkaProps = new Properties();

        //List of Kafka brokers to connect to
        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka1:29092,kafka2:29093,kafka3:29094");

        //Deserializer class to convert Keys from Byte Array to String
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        //Deserializer class to convert Messages from Byte Array to String
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        //Consumer Group ID for this consumer
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG,
                "kafka-consumer-group-1");

        //Set to consume from the earliest message, on start when no offset is
        //available in Kafka
        kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        //Create a Consumer
        KafkaConsumer<String, String> simpleConsumer =
                new KafkaConsumer<String, String>(kafkaProps);

        //Subscribe to the kafka.learning.orders topic
        simpleConsumer.subscribe(Arrays.asList("kafka.postgres-modify"));

        //Continuously poll for new messages
        while(true) {

            //Poll with timeout of 100 milli seconds
            ConsumerRecords<String, String> messages =
                    simpleConsumer.poll(Duration.ofMillis(100));

            //Print batch of records consumed
            for (ConsumerRecord<String, String> message : messages){
                System.out.println(message.value());
                Student messageStudent = (Student)sdo.fromString(message.value());
                System.out.println("---Receive massage from Kafka--- Name: " + messageStudent.getName()+" "+"Age: "+messageStudent.getAge());
                processMessage(messageStudent);
            }
        }
    }
}

