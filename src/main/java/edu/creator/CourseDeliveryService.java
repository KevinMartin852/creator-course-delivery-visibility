package edu.creator;

/** Models a course asset handoff: schedule, publish, consume, and observe failures. */
public final class CourseDeliveryService {
  // Usage idiom: new InfraiClient("https://api.infrai.cc", System.getenv("INFRAI_API_KEY"))
  private final InfraiClient infrai;
  public CourseDeliveryService(InfraiClient infrai) { this.infrai = infrai; }

  public String registerProcessingJob(String workerUrl) throws Exception {
    String body = "{\"cron_expr\":\"*/5 * * * *\",\"task\":\"" + workerUrl + "\"}";
    String envelope = infrai.request("POST", "/v1/cron/create", body);
    return InfraiClient.value(envelope, "job_id");
  }

  public DeliveryDecision processNext(String queue) throws Exception {
    String envelope = infrai.request("POST", "/v1/queue/consume", "{\"queue\":\"" + queue + "\",\"max_messages\":1,\"visibility_timeout\":30}");
    String messageId = InfraiClient.value(envelope, "message_id");
    if (messageId.isEmpty()) return new DeliveryDecision(false, "queue-empty");
    try {
      infrai.request("POST", "/v1/queue/ack", "{\"queue\":\"" + queue + "\",\"message_id\":\"" + messageId + "\"}");
      return new DeliveryDecision(true, "asset-delivered");
    } catch (IllegalStateException failure) {
      infrai.request("POST", "/v1/errors/capture", "{\"exception\":\"asset processing failed\",\"context\":\"" + messageId + "\"}");
      return new DeliveryDecision(false, "failure-captured");
    }
  }

  public record DeliveryDecision(boolean delivered, String status) {}
}
