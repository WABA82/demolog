package com.examples.demolog.domains.common.outbox.publisher;

import com.examples.demolog.domains.common.outbox.model.Outbox;
import com.examples.demolog.domains.common.outbox.model.OutboxStatus;
import com.examples.demolog.domains.common.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 아웃 박스 엔티티에 등록된 이벤트 발행
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<Outbox> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (Outbox outbox : pendingEvents) {
            try {
                kafkaTemplate.send(outbox.getTopic(), outbox.getAggregateId().toString(), outbox.getPayload()).get();
                outbox.markAsPublished();
                log.info("Outbox 이벤트 발행 완료: topic={}, eventType={}, aggregateId={}",
                        outbox.getTopic(), outbox.getEventType(), outbox.getAggregateId());
            } catch (Exception e) {
                outbox.markAsFailed();
                log.error("Outbox 이벤트 발행 실패: topic={}, eventType={}, aggregateId={}",
                        outbox.getTopic(), outbox.getEventType(), outbox.getAggregateId(), e);
            }
        }
    }

}
