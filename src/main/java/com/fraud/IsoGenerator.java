package com.fraud; 

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class IsoGenerator {
    private static final List<String> MERCHANTS = List.of(
            "Starbucks NYC", "McDonalds LA", "InnOut SF", "Uber Ride", "Walmart LV", "Apple Store Madrid"
    );

    public static void main(String[] args) throws Exception {
        MessageFactory<IsoMessage> mfact = new MessageFactory<>();
        Random random = new Random();

        System.out.println("Starting transaction simulator (press Ctrl+C to stop)");

        while (true) {
            IsoMessage msg = mfact.newMessage(0x200);

            String amount = String.valueOf(random.nextInt(5000) + 10);
            String merchant = MERCHANTS.get(random.nextInt(MERCHANTS.size()));

            double displayAmount = Double.valueOf(amount) / 100.0;

            msg.setValue(4, amount, IsoType.NUMERIC, 12);
            msg.setValue(43, merchant, IsoType.ALPHA, 40);

            System.out.printf("[%s] New Transaction: %s -> $%.2f%n", java.time.LocalTime.now(), merchant, displayAmount);
            System.out.println("RAW ISO8583: " + msg.debugString());
            System.out.println("---");

            TimeUnit.SECONDS.sleep(1);

        }
    }
}