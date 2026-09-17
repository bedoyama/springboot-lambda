# ShortLink

A URL shortener used as a teaching example for running Spring Boot on AWS Lambda.

This is still a normal Spring Boot 4 app. Lambda packaging comes in later steps. See [plan.md](plan.md) for the commit-by-commit path.

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
