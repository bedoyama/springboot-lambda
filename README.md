# ShortLink

A URL shortener used as a teaching example for running Spring Boot on AWS Lambda.

The same `@RestController` beans run in two ways:

- Locally: `./mvnw spring-boot:run` starts embedded Tomcat (in-memory store, `local` profile)
- On Lambda: `StreamLambdaHandler` translates API Gateway events into HTTP and dispatches them to Spring (DynamoDB)

See [plan.md](plan.md) for the commit-by-commit path.

## Prerequisites

- Java 21
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) for `sam build` / `sam deploy`
- Docker for `./mvnw test` (DynamoDB Local via Testcontainers) and `sam local invoke`

The Maven wrapper (`./mvnw`) is committed, so a local Maven install is optional.

## Run locally

The default profile is `local`, so `spring-boot:run` does not need AWS credentials or DynamoDB.

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

## Package for Lambda

Spring Boot's executable jar puts classes under `BOOT-INF/`, which Lambda cannot load as a handler. `./mvnw package` also builds a shaded uber-jar at `target/shortlink-aws.jar` with classes at the root.

```bash
./mvnw -DskipTests package
sam build
```

`SkipBuild: true` in `template.yaml` tells SAM to use that jar as-is instead of compiling Java itself.

`template.yaml` points one function at that jar. API Gateway HTTP API sends **every** path to it (`/{proxy+}` and `/`). Spring MVC routes `/health`, `/links`, and `/r/{code}` inside the process.

HTTP API payload format is **1.0** so the event matches `AwsProxyRequest` used by `StreamLambdaHandler`. The default HTTP API payload (2.0) would not.

Local invoke needs Docker:

```bash
sam local invoke ShortlinkFunction --event events/health.json
```

`/health` works without DynamoDB. Create/redirect/stats in a real deploy need the table.

## Deploy

```bash
./mvnw -DskipTests package
sam build
sam deploy --guided
```

SAM creates the HTTP API, Lambda, and a DynamoDB table (`PAY_PER_REQUEST`, partition key `code`). The function gets `TABLE_NAME` and `SPRING_PROFILES_ACTIVE=lambda`.

Curl the `ApiEndpoint` output the same way as the local commands above.
