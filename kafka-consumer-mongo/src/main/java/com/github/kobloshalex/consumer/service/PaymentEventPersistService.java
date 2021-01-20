package com.github.kobloshalex.consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kobloshalex.consumer.entity.PaymentEvent;
import com.github.kobloshalex.consumer.repository.PaymentEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PaymentEventPersistService {

  private final ObjectMapper objectMapper;
  private final PaymentEventRepository repository;

  public PaymentEventPersistService(ObjectMapper objectMapper, PaymentEventRepository repository) {
    this.objectMapper = objectMapper;
    this.repository = repository;
  }

  public void processPaymentEvent(final ConsumerRecord<Integer, String> consumerRecord)
      throws JsonProcessingException {
    final PaymentEvent paymentEvent =
        objectMapper.readValue(consumerRecord.value(), PaymentEvent.class);
    switch (paymentEvent.getEventOperationType()) {
      case POST:
        save(paymentEvent);
        break;
      case UPDATE:
        validate(paymentEvent);
        save(paymentEvent);
        break;
      default:
        log.info("Invalid payment event");
    }
  }

  private void validate(PaymentEvent paymentEvent) {
    if (paymentEvent.getPaymentEventId() == null) {
      throw new IllegalArgumentException("Payment id is missing");
    }
    final Optional<PaymentEvent> optionalPaymentEvent =
        repository.findById(paymentEvent.getPaymentEventId());

    if (!optionalPaymentEvent.isPresent()) {
      throw new IllegalArgumentException("not a valid event id");
    }
    log.info("Successfully update payment event {}", optionalPaymentEvent.get());
  }

  private void save(final PaymentEvent paymentEvent) {
    paymentEvent.getPayment().setPaymentEvent(paymentEvent); // map entities
    repository.save(paymentEvent);
    log.info(
        "Successfully persist event: {} with payment {}", paymentEvent, paymentEvent.getPayment());
  }
}
