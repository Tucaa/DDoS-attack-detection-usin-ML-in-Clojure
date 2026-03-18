# DDoS Attack Detection using Neural Networks
This project implements a machine learning-based system for detecting Distributed Denial of Service (DDoS) attacks using classification neural networks. The system analyzes network traffic patterns to identify and classify potential DDoS attacks in real-time.
The core idea of this project is to use neural networks to detect DDoS attack patterns extracted from sFlow data. Detailed analysis has identified specific attacks that can be detected within sFlow traffic.

1.**DNS Amplification**: The attacker uses spoofed queries to DNS servers to overwhelm the victim with amplified responses, leveraging port 53 for massive volumetric impact.

2.**Subnet Carpet Bombing**: Instead of a single target, the attack is dispersed across an entire range of IP addresses within a subnet to evade security detection thresholds set for individual hosts.

3.**SYN Flood**: A classic resource-exhaustion attack that exploits the TCP handshake by sending continuous connection requests without the intention of completing them, consuming the server's memory and processing power.

4.**ICMP Flood**: Overwhelms the target with incessant ICMP Echo Request (ping) packets, forcing the system to waste resources on processing and responding until it becomes unavailable to legitimate users.

5.**UDP Flood (Mixed)**: Generates chaotic traffic on random ports to overload network interfaces and firewalls, making precise filtering difficult due to high port entropy.

6.**NTP Amplification**: Similar to DNS, this abuses port 123 and NTP servers to reflect massive amounts of data toward the victim, turning small queries into devastating waves of traffic.

7.**ACK Flood**: Sends spoofed TCP ACK packets that do not belong to any active session, aiming to confuse the firewall and exhaust server resources attempting to validate non-existent connections.

## Project Structure

The project consists of the following logical components:

### attacks.clj
This namespace defines the template (data structure) for DDoS attacks (beforementioned). This structure is used to create the new_ddos_dataset.csv. I opted for generating synthetic data to maintain full control and tailor the data to the specific use case, supplemented by findings from Kaggle datasets.

### normal.clj
Similar to the attacks namespace, but represents normal traffic. Normal traffic is categorized into the following groups:

**Normal Web Traffic**: Standard internet browsing characterized by typical TCP handshakes on ports 80 and 443, with balanced diversity in source and destination addresses.

**Normal Enterprise Traffic**: A diverse mix of TCP and UDP protocols with high port entropy, reflecting a wide range of business applications and internal services.

**Normal Streaming Traffic**: Constant flow of large UDP packets with a high share of total bytes but low IP entropy, as traffic usually originates from specific CDN servers.

**Normal DNS Traffic**: Stable and predictable traffic on port 53 with a near 1:1 query-to-response ratio, showing no signs of suspicious amplification.

**Normal Email Traffic**: Specific low-intensity TCP traffic with clear session structures on standard ports (25, 465, 587).

**Normal Mixed Traffic**: A natural and heterogeneous combination of all the above patterns, representing a realistic, dynamic state of daily network activity.

### random.clj
Contains helper functions for data randomization used for both attack patterns and normal traffic.

#### importexport.clj
Contains helper functions for importing and exporting .csv files.

### windowing.clj
The core of the synthetic data generation. This namespace defines chronological segments. Instead of random dataset filling, it generates a realistic flow over time.
Key components:

**Duration Definitio**: Defines how long each attack (and normal traffic) lasts via define-duration and attack-pattern-duration.
**Attack Waves**: Simulates attack waves using waves and wave-patterns functions, reflecting how DDoS attacks often occur in multiple bursts.
**Time Windows**: The critical part where traffic is distributed into segments and metadata (timestamps) is added. Functions include generate-attack-windows, generate-windows-normal, and add-window-metadata.
**Orchestration**: The generate-timeline function serves as the primary orchestrator for the functions mentioned above.

**Note**: These functions are called within core.clj.

### Neural Networks Implementation
**This section represents the heart of the project**!.

### simplenn.clj
I implemented a simple neural network from scratch using the Neanderthal library. The code is divided into three logical parts:

**Activation and Mathematical Functions**: The most important are ReLU and Softmax. ReLU is used as the activation function for hidden layers, while Softmax is used in the output layer for classification. (Initially, I started with a Sigmoid function but realized it was incorrect for this specific classification task).

**Network Definition Functions**:

**Layer definition**: create-layer and create-layer-he.

**Training steps**: single-step.

**Forward Pass**: forward and nn-forward.

**Gradient Initialization**: init-grad (output layer) and layer-gradients (other layers).

**Layer Updates**: update-layer!.

**Backpropagation**: backpropagation-singular, backpropagation, and propagate-backwards.

**Full Training**: train-batch and train-nn.

**Metrics Functions**: Calculates the Confusion Matrix (confusion-matrix-fn), which is used to derive further metrics (class-metrics): Precision, Recall, and F1-score.

**Note**: Metrics indicated that the current simple network does not yield optimal results. This is expected, as this type of sequential data typically requires a Recurrent Neural Network (RNN), which has been implemented in deepdnn.clj

### deepdnn.clj and dataprep.clj

Dataprep contains functions for data preparation in a format that matches the deep diamond library
Deepdnn.clj contains 2 defined neural networks, feedforward and recurrent neural network (rnn)
Basically for a dataset containing sequential data (which is essentially our dataset that we generated) recurrent networks are the best choice.
The feedforward network is created just for example, or for comparison with simplenn.clj which is also feedforward. 
This namespace consists of the neural network architectures nn-architecture and rnn-architecture (and the gpu helper functions feedforward-nn, recurrentt-nn, nn nn-rec) and the neural network training functions train-nn and train-rnn

### Literature:
https://dragan.rocks/

https://aiprobook.com/deep-learning-for-programmers/

https://aiprobook.com/numerical-linear-algebra-for-programmers/

https://github.com/uncomplicate

https://inmon.com/technology/index.php

https://www.cisco.com/c/en/us/td/docs/iosxr/cisco8000/netflow/configuration/b-netflow-configuration-ios-xr-8000.html

https://www.geeksforgeeks.org/machine-learning/introduction-to-recurrent-neural-network/

https://www.cloudflare.com/learning/ddos/what-is-a-ddos-attack/

Internal documentation (from company)
