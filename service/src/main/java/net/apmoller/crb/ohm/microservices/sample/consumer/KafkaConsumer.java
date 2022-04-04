package net.apmoller.crb.ohm.microservices.sample.consumer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import net.apmoller.crb.ohm.microservices.sample.models.User;

/**
 * This class contains all the functionality needed for acting on messages consumed from
 * Submission related kafka topics
 */
@Configuration
@AllArgsConstructor
@EnableKafka
@Slf4j
public class KafkaConsumer {

    /**
     * Responsible for reading submission results message on Kafka topic
     * and updating the submission status accordingly in C*
     * @param submissionResultKafkaMessage Kafka message payload for the submission result
     * @param key CommandCorrelationId used to identify submission command
     * @param acknowledgment to indicate Kafka broker if the kafka message has been successfully consumed and its safe to commit Kafka message offset
     */
    @KafkaListener(topics = {"${kafka.command.submission-result.topic}"}, containerFactory = "kafkaListenerContainerFactory" , clientIdPrefix = "results")
    public String onSubmissionResultsMessage(@Payload String submissionResultKafkaMessage,
                                           @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                                           Acknowledgment acknowledgment) {
        log.debug("Received Submission result {} for message key {} ", submissionResultKafkaMessage, key);
        log.info("Consumer-Service, Consumed message {} from Submission topic with key {}", submissionResultKafkaMessage, key);

        acknowledgment.acknowledge();

      return "Message Successfully Consumed";
    }

    /**
     * Responsible for reading submission kafka message and acknowledging (status = ACCEPTED) the submission request in C*
     * @param submissionKafkaMessage Kafka message payload for the submission request
     * @param key CommandCorrelationId used to identify submission command
     * @param acknowledgment to indicate Kafka broker if the kafka message has been successfully consumed and its safe to commit Kafka message offset
     */
    @KafkaListener(topics = {"${kafka.command.submission.topic}"}, containerFactory = "kafkaListenerContainerFactory", clientIdPrefix = "command")
    public String onSubmissionKafkaMessage(@Payload User submissionKafkaMessage,
                                         @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                                         Acknowledgment acknowledgment) {
        log.debug("Received Submission Kafka message with key {}", key);
        log.info("Consumer-Service, Consumed message {} from Submission topic with key {}", submissionKafkaMessage, key);

        acknowledgment.acknowledge();

      return "Message Successfully Consumed";
    }
}
