package com.fraud;

import com.solab.iso8583.*;
import com.solab.iso8583.parse.NumericParseInfo;
import com.solab.iso8583.parse.AlphaParseInfo;
import com.solab.iso8583.parse.FieldParseInfo;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FraudDetector {

    public static void main(String[] args) {
        // setup: same schema used by the generator
        MessageFactory<IsoMessage> mfact = new MessageFactory<>();

        mfact.setUseBinaryMessages(false);
        mfact.setAssignDate(true);

        Map<Integer, FieldParseInfo> parseMap0200 = new HashMap<>();
        parseMap0200.put(4, new NumericParseInfo(12)); 
        parseMap0200.put(43, new AlphaParseInfo(40)); 

        // Register the parsing rules for 0200
        mfact.setParseMap(0x200, parseMap0200);

        IsoMessage template = new IsoMessage();     
        template.setBinary(false);
        template.setType(0x200);
        template.setValue(4, "000000000000", IsoType.NUMERIC, 12); // Amount
        template.setValue(43, "Dummy Merchant", IsoType.ALPHA, 40); // Merchant
        mfact.addMessageTemplate(template);

        try {
            String testMsg = "02001000000000200000000000000500Dummy Merchant Full Name 123456789 Seven";
            IsoMessage check = mfact.parseMessage(testMsg.getBytes(StandardCharsets.UTF_8), 0);
            System.out.println("FACTORY SELF-TEST PASSED: " + check.debugString());
        } catch (Exception e) {
            System.err.println("FACTORY SELF-TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            return; 
        }

        // kafka config
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fraud-detector-v1");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> transactions = builder.stream("financial-transactions");

        // string -> isomessage
        KStream<String, IsoMessage> isoMessages = transactions.mapValues(value -> {
            try {
                // raw string into bytes --> then parse it
                return mfact.parseMessage(value.getBytes(StandardCharsets.UTF_8), 0);
            } catch (Exception e) {
                System.err.println("Failed to parse ISO message: " + e.getMessage());
                e.printStackTrace();
                return null; 
            }
        });

        isoMessages
            .filter((key, isoMsg) -> isoMsg != null) //remove null messages
            
            .filter((key, isoMsg) -> {
                try {
                    String amountStr = isoMsg.getObjectValue(4).toString();
                    long amountCents = Long.parseLong(amountStr);
                    return amountCents > 100000; // amount > $1000.00
                } catch (Exception e) {
                    System.err.println("Failed to parse amount: " + e.getMessage());
                    return false; 
                }
            })

            .foreach((key, isoMsg) -> {
                String merchant = isoMsg.getObjectValue(43).toString();
                String amount = isoMsg.getObjectValue(4).toString();

                double niceAmount = Double.parseDouble(amount) / 100.0;

                System.out.println("\n***FRAUD DETECTED***");
                System.out.println("Merchant: " + merchant);
                System.out.println("Amount: $" + String.format("%.2f", niceAmount));
                System.out.println("---");
            });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        System.out.println("Fraud Detector Listening.");
    }
}