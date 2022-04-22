package net.apmoller.crb.ohm.microservices.sample.consumer;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.apmoller.crb.ohm.microservices.sample.models.User;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains all the configuration information for Kafka producer factory to be able to create a Kafka
 * template for publishing messages
 */
@Configuration
@Getter
@Slf4j
@RequiredArgsConstructor
public class KafkaConfig implements KafkaListenerConfigurer {

    private final LocalValidatorFactoryBean validator;

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${kafka.username:}")
    private String username;
    @Value("${kafka.password:}")
    private String password;
    @Value("${kafka.login-module:org.apache.kafka.common.security.plain.PlainLoginModule}")
    private String loginModule;
    @Value("${kafka.sasl-mechanism:PLAIN}")
    private String saslMechanism;
    @Value("${kafka.truststore-location:}")
    private String truststoreLocation;
    @Value("${kafka.clientId}")
    private String kafkaClientId;
    @Value("${kafka.truststore-password:}")
    private String truststorePassword;
    @Value("${kafka.producer.acks-config:all}")
    private String producerAcksConfig;
    @Value("${kafka.producer.linger:1}")
    private int producerLinger;
    @Value("${kafka.producer.timeout:30000}")
    private int producerRequestTimeout;
    @Value("${kafka.producer.batch-size:16384}")
    private int producerBatchSize;
    @Value("${kafka.producer.send-buffer:131072}")
    private int producerSendBuffer;
    @Value("${kafka.security-protocol:SASL_SSL}")
    private String securityProtocol;
    @Value("${kafka.consumer.group:submission-consumer-group1}")
    private String consumerGroup;
    @Value("${kafka.consumer.offset-auto-reset:latest}")
    private String consumerOffsetAutoReset;
    @Value("${kafka.consumer.max-poll-records:20}")
    private String consumerMaxPollRecords;
    @Value("${kafka.consumer.concurrency:3}")
    private int consumerConcurrency;
    @Value("${kafka.consumer.retry.max-attempts:3}")
    private int maxRetryAttempts;
    @Value("${kafka.consumer.retry.initial-interval-secs:1}")
    private int retryInitialIntervalSeconds;
    @Value("${kafka.consumer.retry.max-interval-secs:10}")
    private int retryMaxIntervalSeconds;

    private static void addSaslProperties(Map<String, Object> properties, String saslMechanism, String securityProtocol,
            String loginModule, String username, String password) {
        if (!StringUtils.isEmpty(username)) {
            properties.put("security.protocol", securityProtocol);
            properties.put("sasl.mechanism", saslMechanism);
            String saslJassConfig = String.format("%s required username=\"%s\" password=\"%s\" ;", loginModule,
                    username, password);
            properties.put("sasl.jaas.config", saslJassConfig);
        }
    }

    private static void addTruststoreProperties(Map<String, Object> properties, String location, String password) {
        if (!StringUtils.isEmpty(location)) {
            properties.put("ssl.truststore.location", location);
            properties.put("ssl.truststore.password", password);
        }
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, producerLinger);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerRequestTimeout);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        properties.put(ProducerConfig.SEND_BUFFER_CONFIG, producerBatchSize);
        properties.put(ProducerConfig.ACKS_CONFIG, producerAcksConfig);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaClientId);
        properties.put(ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG, "org.apache.kafka.common.metrics.JmxReporter");
        properties.put(ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO");
       // properties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");


        addSaslProperties(properties, saslMechanism, securityProtocol, loginModule, username, password);
        addTruststoreProperties(properties, truststoreLocation, truststorePassword);

        DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(properties);
        pf.addListener(new MicrometerProducerListener<String, String>(Metrics.globalRegistry,
                Collections.singletonList(new ImmutableTag("customTag", "customTagValue"))));

        return pf;
    }

    @Bean
    public KafkaTemplate kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaClientId);

        addSaslProperties(properties, saslMechanism, securityProtocol, loginModule, username, password);
        addTruststoreProperties(properties, truststoreLocation, truststorePassword);

        return new KafkaAdmin(properties);
    }

    @Bean
    public ConsumerFactory<String, User> consumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerOffsetAutoReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerMaxPollRecords);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaClientId);
        //properties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor");

        addSaslProperties(properties, saslMechanism, securityProtocol, loginModule, username, password);
        addTruststoreProperties(properties, truststoreLocation, truststorePassword);

        DefaultKafkaConsumerFactory<String, User> cf = new DefaultKafkaConsumerFactory<>(properties);
        cf.addListener(new MicrometerConsumerListener<String, User>(Metrics.globalRegistry,
                Collections.singletonList(new ImmutableTag("customTag", "customTagValue"))));

        return cf;
    }

    @Bean
    public StringJsonMessageConverter stringJsonMessageConverter(ObjectMapper mapper) {
        return new StringJsonMessageConverter(mapper);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, User> kafkaListenerContainerFactory(
            StringJsonMessageConverter messageConverter) {
        ConcurrentKafkaListenerContainerFactory<String, User> factory = new ConcurrentKafkaListenerContainerFactory();

        factory.setMessageConverter(messageConverter);
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(consumerConcurrency);
        factory.setRetryTemplate(retryTemplate());
        factory.setStatefulRetry(true);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate()),
                new ExponentialBackOff()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    @Bean
    public DeadLetterPublishingRecoverer recoverer(KafkaTemplate<Object, Object> template) {
        return new DeadLetterPublishingRecoverer(template);
    }

    private RetryPolicy retryPolicy() {
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(maxRetryAttempts);
        return policy;
    }

    private BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(retryInitialIntervalSeconds * 1000);
        policy.setMaxInterval(retryMaxIntervalSeconds * 1000);
        return policy;
    }

    private RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        template.setRetryPolicy(retryPolicy());
        template.setBackOffPolicy(backOffPolicy());

        return template;
    }

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar registrar) {
        registrar.setValidator(this.validator);
    }

}
