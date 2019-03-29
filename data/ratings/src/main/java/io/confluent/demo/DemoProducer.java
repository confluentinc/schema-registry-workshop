package io.confluent.demo;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;


import java.util.Properties;

public class DemoProducer {

    public static final String TOPIC = "demo-topic";

    public static void main(String args[]) {

        Properties config = new Properties();
        config.put("bootstrap.servers", "localhost:29092");
        config.put("acks", "all");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(config);

        final ProducerRecord<String, String> record = new ProducerRecord<String, String>(TOPIC, "key", "value");
        producer.send(record);

        producer.flush();

    }

}
