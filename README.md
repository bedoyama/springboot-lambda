# ShortLink

A URL shortener used as a teaching example for running Spring Boot on AWS Lambda.

The same `@RestController` beans run in two ways:

- Locally: `./mvnw spring-boot:run` starts embedded Tomcat
- On Lambda: `StreamLambdaHandler` translates API Gateway events into HTTP and dispatches them to Spring

Packaging and deploy come in later steps. See [plan.md](plan.md).

## Prerequisites

- Java 21

The Maven wrapper (`./mvnw`) is committed, so a local Maven install is optional.

## Run locally

```bash
./mvnw test
./mvnw spring-boot:run
```

Then:

```bash
curl localhost:8080/health
curl -i -X POST localhost:8080/links -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com"}'
curl -i localhost:8080/r/<code>
curl localhost:8080/links/<code>
```
