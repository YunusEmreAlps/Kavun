package com.kavun.backend.persistent.specification;

import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.kavun.backend.persistent.domain.siem.ApplicationLog;

@Component
public class ApplicationLogSpecification extends BaseSpecification<ApplicationLog> {

    @SuppressWarnings("unchecked")
    public Specification<ApplicationLog> search(Map<String, Object> search) {
        Specification<ApplicationLog> specification = ApplicationLogSpecification.conjunction();
        if (search.containsKey("correlationId") && !search.get("correlationId").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("correlationId")),
                            "%" + search.get("correlationId").toString().toLowerCase() + "%"))));
        }
        // log_level
        if (search.containsKey("logLevel") && !search.get("logLevel").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("logLevel"),
                            search.get("logLevel").toString()))));
        }
        // thread_name
        if (search.containsKey("threadName") && !search.get("threadName").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("threadName")),
                            "%" + search.get("threadName").toString().toLowerCase() + "%"))));
        }
        // logger_name
        if (search.containsKey("loggerName") && !search.get("loggerName").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("loggerName")),
                            "%" + search.get("loggerName").toString().toLowerCase() + "%"))));
        }
        // log_message
        if (search.containsKey("logMessage") && !search.get("logMessage").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("logMessage")),
                            "%" + search.get("logMessage").toString().toLowerCase() + "%"))));
        }
        // hostname
        if (search.containsKey("hostname") && !search.get("hostname").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("hostname")),
                            "%" + search.get("hostname").toString().toLowerCase() + "%"))));
        }
        // ip
        if (search.containsKey("ip") && !search.get("ip").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("ipAddress")),
                            "%" + search.get("ip").toString().toLowerCase() + "%"))));
        }
        // log_type
        if (search.containsKey("logType") && !search.get("logType").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("logType"),
                            search.get("logType").toString()))));
        }
        // user_ip_address
        if (search.containsKey("userIpAddress") && !search.get("userIpAddress").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("userIpAddress")),
                            "%" + search.get("userIpAddress").toString().toLowerCase() + "%"))));
        }
        // username
        if (search.containsKey("username") && !search.get("username").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("username")),
                            "%" + search.get("username").toString().toLowerCase() + "%"))));
        }
        // request_url
        if (search.containsKey("requestUrl") && !search.get("requestUrl").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("requestUrl")),
                            "%" + search.get("requestUrl").toString().toLowerCase() + "%"))));
        }
        // action
        if (search.containsKey("action") && !search.get("action").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("action")),
                            "%" + search.get("action").toString().toLowerCase() + "%"))));
        }
        // request_params
        if (search.containsKey("requestParams") && !search.get("requestParams").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("requestParams")),
                            "%" + search.get("requestParams").toString().toLowerCase() + "%"))));
        }
        // http_status
        if (search.containsKey("httpStatus") && search.get("httpStatus") != null
                && !search.get("httpStatus").toString().isEmpty()) {
            try {
                int httpStatus = Integer.parseInt(search.get("httpStatus").toString());
                specification = specification.and(
                        (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("httpStatus"),
                                httpStatus))));
            } catch (NumberFormatException e) {
                // Ignore invalid number format for httpStatus
            }
        }
        // device_id
        if (search.containsKey("deviceId") && !search.get("deviceId").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("deviceId")),
                            "%" + search.get("deviceId").toString().toLowerCase() + "%"))));
        }
        // device_type
        if (search.containsKey("deviceType") && !search.get("deviceType").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("deviceType")),
                            "%" + search.get("deviceType").toString().toLowerCase() + "%"))));
        }
        // operating_system
        if (search.containsKey("operatingSystem") && !search.get("operatingSystem").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("operatingSystem")),
                            "%" + search.get("operatingSystem").toString().toLowerCase() + "%"))));
        }
        // browser
        if (search.containsKey("browser") && !search.get("browser").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("browser")),
                            "%" + search.get("browser").toString().toLowerCase() + "%"))));
        }

        // date range filtering for log creation time
        if (search.containsKey("startDate") && search.containsKey("endDate")
                && search.get("startDate") != null && search.get("endDate") != null) {
            try {
                String startDateStr = search.get("startDate").toString();
                String endDateStr = search.get("endDate").toString();
                specification = specification.and(
                        (((root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("createdAt"),
                                java.sql.Timestamp.valueOf(startDateStr + " 00:00:00"),
                                java.sql.Timestamp.valueOf(endDateStr + " 23:59:59")))));
            } catch (IllegalArgumentException e) {
                // Ignore invalid date format
            }
        }

        return specification;
    }
}
