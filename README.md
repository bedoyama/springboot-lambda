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

First time:

```bash
./mvnw -DskipTests package
sam build
sam deploy --guided
```

Later deploys reuse `samconfig.toml`:

```bash
./mvnw -DskipTests package
sam build
sam deploy
```

SAM creates the HTTP API, Lambda (published version + `live` alias, SnapStart on), and a DynamoDB table. The function gets `TABLE_NAME` and `SPRING_PROFILES_ACTIVE=lambda`.

The first SnapStart deploy can take several minutes: Lambda initializes Spring, takes a snapshot, then publishes the version.

## Test in AWS

After deploy, read the API URL (stack name and region come from `samconfig.toml` if you used `--guided`):

```bash
sam list stack-outputs --stack-name sam-app --output table
```

Or:

```bash
aws cloudformation describe-stacks \
  --stack-name sam-app \
  --query "Stacks[0].Outputs" \
  --output table
```

Copy `ApiEndpoint` (no trailing slash) and run the same calls as locally. Do **not** add `-L` on the redirect; you want to see the 302.

```bash
API="https://xxxxxxxx.execute-api.us-east-2.amazonaws.com"

curl -sS "$API/health"
# {"status":"UP"}

curl -sS -X POST "$API/links" -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com"}'
# {"code":"...","shortUrl":"/r/...","originalUrl":"https://example.com"}

# paste the code from the previous response
CODE="REPLACE_ME"

curl -sSI "$API/r/$CODE"
# HTTP/2 302
# location: https://example.com

curl -sS "$API/links/$CODE"
# {"originalUrl":"https://example.com","createdAt":"...","clickCount":1}
```

If `/health` works but `POST /links` returns 500, the table name or IAM policy is wrong. Check:

```bash
sam logs --stack-name sam-app --name ShortlinkFunction --tail
```

Wait until SnapStart has finished snapshotting (a minute or two after deploy), then look at a **cold** invoke's `REPORT` line in those logs:

- Without SnapStart you see `Init Duration` (Spring Boot starting, often seconds).
- With SnapStart you see `Restore Duration` instead (usually hundreds of milliseconds).
- `$LATEST` never uses SnapStart. The HTTP API is wired to the `live` alias, which points at a published version.

## SnapStart

Java on Lambda is slow to start because of the JVM and Spring. SnapStart takes a memory snapshot **after** init and restores it on later cold starts.

Unsafe at class-init (would be copied into every restore): unique IDs, secrets, open connections. This app generates short codes **per request**, reseeds `SecureRandom` on restore, and rebuilds the DynamoDB client on restore (CRaC `afterRestore`).

GraalVM native can start even faster but is a much heavier build. This example stays on SnapStart.
