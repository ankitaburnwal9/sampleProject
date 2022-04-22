package net.apmoller.crb.ohm.microservices.sample.services;

import net.apmoller.crb.ohm.microservices.sample.models.User;
import lombok.extern.slf4j.Slf4j;
import net.apmoller.crb.ohm.microservices.sample.producer.KafkaMessageGenerator;
import net.apmoller.crb.ohm.microservices.sample.exceptions.InternalServerException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * This is a implementation class for {@link SubmissionService} interface.
 */
@Service
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final KafkaMessageGenerator kafkaMessageGenerator;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String sampleCommandTopic;

    @Value("${spring.application.name}")
    private String serviceName;

    public SubmissionServiceImpl(KafkaMessageGenerator kafkaMessageGenerator,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.command.submission.topic}") String sampleCommandTopic) {
        this.kafkaMessageGenerator = kafkaMessageGenerator;
        this.kafkaTemplate = kafkaTemplate;
        this.sampleCommandTopic = sampleCommandTopic;
    }

    @Override
    public User submit(String name, String dept) {

        UUID commandCorrelationId = UUID.randomUUID();
        log.debug("Going to send submission message for command correlation Id {}", commandCorrelationId);

        User user = new User(name, dept);

        String kafkaMessage = kafkaMessageGenerator.getMessage(user);

        try {
            RecordMetadata recordMetadata = kafkaTemplate
                    .send(new ProducerRecord<>(sampleCommandTopic, commandCorrelationId.toString(), kafkaMessage)).get()
                    .getRecordMetadata();
            log.info(
                    "Command correlation Id: {} - Published to Kafka topic {} on partition {} at offset {}. Submission message: {}",
                    commandCorrelationId, sampleCommandTopic, recordMetadata.partition(), recordMetadata.offset(),
                    kafkaMessage);
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred! {}", e);
            throw new InternalServerException(e.getMessage());
        } catch (ExecutionException e) {
            log.error("ExecutionException occurred! {}", e);
            throw new InternalServerException(e.getMessage());
        }
        return User.builder().name(name).dept(dept).build();
    }
}
