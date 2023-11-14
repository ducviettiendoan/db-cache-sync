package com.sb.sb.redisson;

/**
 * Copyright (c) 2016-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.redisson.Redisson;
import org.redisson.api.MapOptions;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.MapLoader;
import org.redisson.api.map.MapWriter;
import org.redisson.config.Config;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
// import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.SDObject;
import com.sb.sb.redis.StudentRedisConfig;
import com.sb.sb.student.Student;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

@Service
public class WTProcess {
    @Autowired
    private SDObject sdo;

    private String STRING_KEY_PREFIX = "redi2read:strings:student";

    @Autowired
    //Qualifier make sure template choosing is correct
    @Qualifier("redisTemplate")
	private RedisTemplate<String,String> template;

    @Value("${spring.datasource.username}")
    private String DB_USERNAME;

    @Value("${spring.datasource.password}")
    private String DB_PASSWORD;

    @Value("${spring.datasource.url}")
    private String DB_URL;


    @Async
    // @EventListeners
    public CompletableFuture<WTProcess> runWT() throws IOException, ClassNotFoundException, SQLException {
        //Setup Properties for consumer
        Properties kafkaProps = new Properties();
        System.out.println("NEW PROPERTIES");
        //List of Kafka brokers to connect to
        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "kafka1:29092,kafka2:29093,kafka3:29094");
        System.out.println("CONNECT TO 3 KAFKA BROKER");
        //Deserializer class to convert Keys from Byte Array to String
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        //Deserializer class to convert Messages from Byte Array to String
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        //Consumer Group ID for this consumer
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG,
                "kafka-consumer-group-2");
        System.out.println("CREATE GROUP ID");
        //Set to consume from the earliest message, on start when no offset is
        //available in Kafka
        kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        kafkaProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        //Create a Consumer
        KafkaConsumer<String, String> simpleConsumer = new KafkaConsumer<String, String>(kafkaProps);

        //Subscribe to the kafka.learning.orders topic
        simpleConsumer.subscribe(Arrays.asList("kafka.cache-wt"));
        System.out.println("CREATE CONSUMER");
        
        // connects to 127.0.0.1:6379 by default
        Config config = new Config();
        config.useSingleServer().setAddress("redis://cache:6379"); 
        RedissonClient redisson = Redisson.create(config);
        System.out.println("REDISSON CONNNECT REDIS");
        Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        System.out.println("REDISSON CONNECT POSTGRES");
        MapWriter<String, String> mapWriter = new MapWriter<String, String>() {
            
            @Override
            public void write(Map<String, String> map) {
                try {
                    PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO student (id, age, name) values (?, ?, ?)");
                    try {
                        for (Entry<String, String> entry : map.entrySet()) {
                            Student messageStudent = (Student)sdo.fromString(entry.getValue());
                            preparedStatement.setLong(1, messageStudent.getId());
                            preparedStatement.setInt(2, messageStudent.getAge());
                            preparedStatement.setString(3, messageStudent.getName());
                            preparedStatement.addBatch();
                        }
                        preparedStatement.executeBatch();
                    } finally {
                        preparedStatement.close();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            
            @Override
            public void delete(Collection<String> keys) {
                try {
                    PreparedStatement preparedStatement = conn.prepareStatement("DELETE FROM student where id = ?");
                    try {
                        for (String key : keys) {
                            preparedStatement.setString(1, key);
                            preparedStatement.addBatch();
                        }
                        preparedStatement.executeBatch();
                    } finally {
                        preparedStatement.close();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

            }
        };
        
        MapLoader<String, String> mapLoader = new MapLoader<String, String>() {
            
            @Override
            public Iterable<String> loadAllKeys() {
                List<String> list = new ArrayList<String>();
                try {
                    Statement statement = conn.createStatement();
                    try {
                        ResultSet result = statement.executeQuery("SELECT id FROM student");
                        while (result.next()) {
                            list.add(result.getString(1));
                        }
                    } finally {
                        statement.close();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

                return list;
            }
            
            @Override
            public String load(String key) {
                try {
                    PreparedStatement preparedStatement = conn.prepareStatement("SELECT name FROM student where id = ?");
                    try {
                        preparedStatement.setString(1, key);
                        ResultSet result = preparedStatement.executeQuery();
                        if (result.next()) {
                            return result.getString(1);
                        }
                        return null;
                    } finally {
                        preparedStatement.close();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        
        MapOptions<String, String> options = 
                MapOptions.<String, String>defaults()
                    .writer(mapWriter)
                    .loader(mapLoader);
        System.out.println("@@@@@@@READY TO POLL@@@@@@@");
        while(true) {

            //Poll with timeout of 100 milli seconds
            ConsumerRecords<String, String> messages =
                    simpleConsumer.poll(Duration.ofMillis(1000));
            //Print batch of records consumed
            for (ConsumerRecord<String, String> message : messages){
                System.out.println(message.value());
                System.out.println("---Receive massage from Kafka--- Value: " + message);
                RMap<String, String> map =  redisson.getMap(STRING_KEY_PREFIX, options);
                try{
                    map.put("key", message.value());
                    System.out.println("WRITE TO DB RES");
                    simpleConsumer.commitAsync();
                }catch(Exception e){
                    System.out.println("Cannot Write, EXIT");
                    simpleConsumer.commitAsync();
                }
            }
        }
        // redisson.shutdown();
    }
    
}