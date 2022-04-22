package net.apmoller.crb.ohm.microservices.sample.consumer;

import net.apmoller.crb.ohm.microservices.sample.consumer.KafkaConsumer;
import net.apmoller.crb.ohm.microservices.sample.models.User;
import net.apmoller.crb.ohm.microservices.sample.utils.ResponseStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@DisplayName("Kafka Consumer Test")
public class KafkaConsumerTest {

    private static final String RECEIVED_MESSAGE_KEY = "key";
    @MockBean
    private Acknowledgment acknowledgment;
    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    @BeforeEach
    public void setUp() {
        this.kafkaConsumer = new KafkaConsumer();
    }

    @AfterEach
    public void tearDown() {
        this.kafkaConsumer = null;
    }

    @Test
    @DisplayName("Test Process Events for User")
    public void testProcessEventsForUser() {
        final User user = ResponseStubs.createUser();
        String res = kafkaConsumer.onSubmissionKafkaMessage(user, RECEIVED_MESSAGE_KEY, acknowledgment);
        assertEquals(res, "Message Successfully Consumed");
    }

    @Test
    @DisplayName("Test Process Events for String")
    public void testProcessEventsForString() {
        String message = "message";
        String res = kafkaConsumer.onSubmissionResultsMessage(message, RECEIVED_MESSAGE_KEY, acknowledgment);
        assertEquals(res, "Message Successfully Consumed");
    }
}
