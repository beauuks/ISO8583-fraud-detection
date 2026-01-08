package com.fraud;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class FraudDetector {

    public static void main(String[] args) {
        // add config
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fraud-detector-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // stream builder
        StreamsBuilder builder = new StreamsBuilder();

        // create the stream
        KStream<String, String> transactions = builder.stream("financial-transactions");

        //print (only a simple logic for now)
        transactions.peek((key, value) -> {
            System.out.println("Processing: " + value);
        });

        // start the engine
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close)); // close
        
        System.out.println("Fraud detector listening.");
    }
}
