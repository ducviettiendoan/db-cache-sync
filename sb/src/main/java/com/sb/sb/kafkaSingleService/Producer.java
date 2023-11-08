package com.sb.sb.kafkaSingleService;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import com.sb.sb.redis.SDObject;
import com.sb.sb.student.Student;

import java.util.Properties;

@Service
public class Producer {
    private final SDObject sdo = new SDObject();
    private KafkaProducer producer;

    public Producer(){
        configProd();
    }
    public void configProd(){
        Properties kafkaProps = new Properties();

        //List of brokers to connect to
        kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka1:29092,kafka2:29093,kafka3:29094");

        //Serializer class used to convert Keys to Byte Arrays
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        //Serializer class used to convert Messages to Byte Arrays
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        //Create a Kafka producer from configuration
        KafkaProducer simpleProducer = new KafkaProducer(kafkaProps);
        this.producer = simpleProducer;
    }

    public void runProd(Student student){
        try{
            //Create a producer Record
            ProducerRecord<String, String> kafkaRecord =
                    new ProducerRecord<String, String>(
                            "kafka.cache-wt",    //Topic name
                            String.valueOf(student.getId()),          //Key for the message
                            this.sdo.toString(student) //Message Content
                    );
            System.out.println("Sending Message : "+ kafkaRecord.toString());
            //Publish to Kafka
            this.producer.send(kafkaRecord);
        }
        catch(Exception e) {
            System.out.println(e);
            this.producer.close();
        }
    }
}
