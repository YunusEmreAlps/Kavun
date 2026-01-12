# Roadmap

This document outlines the planned features and improvements for the **Kavun** project. The roadmap is organized by feature categories to provide a clear vision of the project's development trajectory.

---

## Database

- [x] PostgreSQL for production
- [x] H2 for development and testing
- [x] Liquidbase for database migrations
- [x] Hibernate Envers for auditing
- [x] Database connection pooling (HikariCP)
- [ ] Database read replicas for scalability
- [ ] Automated database backups and disaster recovery
- [ ] Multi-tenancy support

---

## Logging

- [x] Logback for structured logging
- [x] Syslog integration for centralized logging [logback.xml]
- [x] Web access logging with custom filters [LoggingFilter.java]
- [ ] Log rotation and retention policies
- [ ] Structured JSON logging for better parsing

---

## Monitoring & Observability

- [x] Prometheus for metrics collection
- [x] Grafana for visualization and dashboards
- [x] Spring Boot Actuator for health checks
- [ ] Alerting and notifications (AlertManager, PagerDuty, Slack)
- [ ] Distributed tracing (Jaeger, Zipkin, OpenTelemetry)
- [ ] Integration with external APM tools (New Relic, Datadog, Dynatrace)
- [ ] Custom business metrics and KPIs

---

## Testing

- [x] Unit tests with JUnit 5 and Mockito
- [x] Integration tests with Testcontainers
- [ ] End-to-end tests with Selenium or Cypress
- [ ] Code coverage reports with JaCoCo (target: 80%+)
- [ ] Performance tests with JMeter or Gatling
- [ ] Contract testing with Pact or Spring Cloud Contract
- [ ] Mutation testing with PIT

---

## Security

- [x] Spring Security for authentication and authorization
- [x] JWT for token-based authentication
- [x] Dynamic role and permission management
- [ ] OAuth2 and OpenID Connect integration
- [ ] Rate limiting and throttling (Bucket4j, Resilience4j)
- [ ] API key management
- [ ] Security scanning with OWASP Dependency-Check
- [ ] Penetration testing and vulnerability assessments
- [ ] Data encryption at rest and in transit

---

## Deployment & Infrastructure

- [x] Docker for containerization
- [x] Docker Compose for local development
- [x] Kubernetes for orchestration
- [x] GitLab CI/CD pipeline
- [ ] GitHub Actions for CI/CD
- [ ] Helm charts for Kubernetes deployments
- [ ] Blue-green and canary deployment strategies
- [ ] Infrastructure as Code (Terraform, Ansible)
- [ ] Multi-region deployment for high availability
- [ ] CDN integration for static assets

---

## Documentation

- [x] API documentation with Swagger/OpenAPI 3.0
- [x] User documentation with Markdown
- [x] Code documentation with Javadoc
- [x] Contributing guidelines and code of conduct
- [x] Roadmap and future plans
- [ ] Architecture documentation with C4 diagrams
- [ ] Deployment documentation with flowcharts
- [ ] Runbooks for operations and troubleshooting
- [ ] API versioning strategy documentation

---

## 🔧 Additional Features

### Communication

- [x] SMTP and Email support
- [ ] SMS notifications (Twilio, AWS SNS)
- [ ] Push notifications (Firebase, OneSignal)
- [ ] WebSocket support for real-time updates

### Caching

- [ ] Redis for distributed caching
- [ ] Memcached support
- [ ] Application-level caching with Caffeine
- [ ] Cache warming strategies

### Messaging & Queuing

- [ ] RabbitMQ for message queuing
- [ ] Apache Kafka for event streaming
- [ ] AWS SQS/SNS integration
- [ ] Event-driven architecture patterns

### Search

- [ ] Elasticsearch for full-text search
- [ ] Apache Solr support
- [ ] Search result ranking and relevance tuning

### Performance & Scalability

- [ ] Horizontal scaling with load balancers
- [ ] Database sharding strategies
- [ ] Async processing with Spring @Async
- [ ] Connection pooling optimization

---

**Last Updated:** October 11, 2025
