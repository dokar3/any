import { describe, expect, test } from "@jest/globals";
import { AnyService, AnyPostFeature, AnyUserFeature } from "any-service-api";
import { createTestService } from "any-service-testing";
import Service from "../src/Service";

function service(): AnyService {
  return createTestService({
    serviceClass: Service,
    manifestPath: "manifest.json",
  });
}

describe("DribbleService", () => {
  test("test fetch shots", () => {
    const result = service()
      .getFeature(AnyPostFeature)
      .fetchFreshList({ pageKey: null });
    expect(result.isOk()).toBe(true);
    expect(result.data).toHaveLength(24);
  });

  test("test fetch user", () => {
    const result = service()
      .getFeature(AnyUserFeature)
      .fetchById({ userId: "Ramotion" });
    expect(result.isOk()).toBe(true);
    expect(result.data?.name).toBe("Ramotion");
  });
});
