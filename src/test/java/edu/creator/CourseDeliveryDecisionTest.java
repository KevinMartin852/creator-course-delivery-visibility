package edu.creator;

public final class CourseDeliveryDecisionTest {
  public static void main(String[] args) {
    CourseDeliveryService.DeliveryDecision d = new CourseDeliveryService.DeliveryDecision(false, "failure-captured");
    if (d.delivered() || !d.status().equals("failure-captured")) throw new AssertionError("failed delivery must be visible");
    System.out.println("course delivery decision: PASS");
  }
}
