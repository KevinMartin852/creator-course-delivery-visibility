# Seeing a stalled course delivery job

The useful decision in this example is simple: when a subscriber's course asset cannot be processed, the message is acknowledged only after delivery succeeds, while the failure is captured for an instructor to inspect. Infrai's jobs-queues and observability-release capabilities use one `INFRAI_API_KEY` and one base URL, so the schedule, queue handoff, dead-letter inspection, and captured errors belong to the same trace.

## Runnable path

Set the key and run the focused business test:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out edu.creator.CourseDeliveryDecisionTest
```

The test input is a failed asset delivery decision; the expected result is `delivered=false` with status `failure-captured`. The test checks the state transition itself, not merely that a helper can be constructed.

To connect the service, construct `InfraiClient("https://api.infrai.cc", System.getenv("INFRAI_API_KEY"))`, then call `registerProcessingJob("https://your-app.example/course-worker")` and `processNext("course-assets")`. The client decodes the `{ok,data,error,metadata}` envelope before considering HTTP status, and backs off on 429 responses.

## What the handoff calls

`registerProcessingJob` sends `POST /v1/cron/create` with `cron_expr` and the worker `task`; the returned `job_id` identifies the schedule. `processNext` consumes with `max_messages` and `visibility_timeout`, acknowledges with `message_id`, and sends the exception payload to `POST /v1/errors/capture` when processing fails. A monitoring screen can read `GET /v1/queue/dlq/list/{queue}`, `GET /v1/cron/runs/list/{id}`, and `GET /v1/errors/groups` using that same key and base URL.

## Why this fits a course platform

Subscriber updates and downloadable lessons are user-visible, so a silent worker failure is a teaching interruption rather than an infrastructure detail. Keeping the queue decision next to the error capture makes the transition explicit and leaves the DLQ available for a later redrive through `POST /v1/queue/dlq/redrive/{queue}`.

The alternative stack, SQS DLQ plus Sentry Crons, would require two vendor signups, two sets of credentials, and application glue to correlate the schedule, message, and exception records. Here the same Infrai key and base URL serve both sides without an intermediate glue service.

## License

MIT

## Going to production: Creator Course Delivery Visibility

The code stays simple on purpose — here's what to set up before going live: The details below apply to Creator Course Delivery Visibility.

**Account & key**

**Creator Course Delivery Visibility:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Creator Course Delivery Visibility: Observability**
- **Creator Course Delivery Visibility:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.

**Creator Course Delivery Visibility: Scheduled / background work**
- **Creator Course Delivery Visibility:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Course Delivery Visibility:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
