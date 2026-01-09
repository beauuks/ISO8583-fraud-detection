# ISO8583 Fraud Detection System

A real-time distributed system that simulates financial transaction traffic and detects fraudulent activity using Apache Kafka and Kafka Streams. The system parses raw ISO8583 financial messages and applies context-aware logic to identify anomalies.

## Architecture

1.  **IsoGenerator (The Source):** Acts as a transaction terminal. Generates synthetic ISO8583 messages with weighted random logic (90% normal, 10% anomalous) and publishes them to the Kafka topic `financial-transactions`.
2.  **Kafka Cluster (The Transport):** A single-node Kafka broker running via Docker to handle message throughput.
3.  **FraudDetector (The Engine):** A Kafka Streams application that:
    * Consumes raw ISO8583 strings.
    * Parses them into Java objects using the `j8583` library (XML configuration).
    * Applies merchant-specific thresholds to flag fraud.
    * Outputs alerts to the console.

## Prerequisites

* Java 17+
* Maven 3.6+
* Docker & Docker Compose

## Setup

1.  **Start Infrastructure**
    Run the Kafka and Zookeeper containers using Docker Compose.
    ```bash
    docker-compose up -d
    ```

2.  **Build Project**
    Compile the Java applications and download dependencies.
    ```bash
    mvn clean install
    ```

## Usage

You must run two separate terminal windows to operate the system.

### Terminal 1: The Generator
Starts the stream of synthetic transactions.
```bash
mvn exec:java -Dexec.mainClass="com.fraud.IsoGenerator"
```

### Terminal 2: The Detector
Starts the analysis engine. This process listens to the Kafka topic and prints alerts when fraud rules are violated.

```bash
mvn exec:java -Dexec.mainClass="com.fraud.FraudDetector"
```

## Detection Logic
The system uses **Context-Aware Thresholds** rather than a flat limit. A transaction is flagged as fraud only if the amount exceeds the specific limit for that merchant category. Any transaction exceeding these limits triggers a fraud alert.

## ISO8583 Configuration
The project handles the ISO8583 standard (v1987) using the j8583 library.
* Message Type: 0200 (Financial Request).
* Field 4: Amount (Numeric, 12 digits).
* Field 43: Merchant Name (Alpha-numeric, 40 characters).