package com.fraud; 

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;

import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.*;

public class IsoGenerator {
    private static final List<String> MERCHANTS = List.of(
            "Starbucks NYC", "McDonalds LA", "InnOut SF", "Uber Ride", "Walmart LV", "Five Guys Madrid", "Apple Store Bangkok"
    );

    public static void main(String[] args) throws Exception {
        MessageFactory<IsoMessage> mfact = new MessageFactory<>();
        Random random = new Random();

        System.out.println("Starting transaction simulator (press Ctrl+C to stop)");

        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        while (true) {
            IsoMessage msg = mfact.newMessage(0x200);

            String amount;
            if (random.nextInt(100) < 90) {
                amount = String.valueOf(random.nextInt(6000) + 100); // Normal Behavior
            } else {
                amount = String.valueOf(random.nextInt(400000) + 100000); // Suspicious Behavior
            }
            String merchant = MERCHANTS.get(random.nextInt(MERCHANTS.size()));

            double displayAmount = Double.valueOf(amount) / 100.0;

            msg.setValue(4, amount, IsoType.NUMERIC, 12);
            msg.setValue(43, merchant, IsoType.ALPHA, 40);

            ProducerRecord<String, String> record = new ProducerRecord<>("financial-transactions", merchant, msg.debugString());
            producer.send(record);

            System.out.printf("[%s] New Transaction: %s -> $%.2f%n", java.time.LocalTime.now(), merchant, displayAmount);
            System.out.println("RAW ISO8583: " + msg.debugString());
            System.out.println("---");

            TimeUnit.SECONDS.sleep(1);
        }
    }
}