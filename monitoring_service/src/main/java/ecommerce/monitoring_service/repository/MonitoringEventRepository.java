package ecommerce.monitoring_service.repository;

import ecommerce.monitoring_service.domain.MonitoringEvent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitoringEventRepository extends R2dbcRepository<MonitoringEvent, Long> {
}
