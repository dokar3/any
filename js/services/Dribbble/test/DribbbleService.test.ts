import { describe, expect, test } from "@jest/globals";
import { AnyService, PostFeature, UserFeature } from "any-service-api";
import { createTestService } from "any-service-testing";
import DribbbleService from "../src/DribbbleService";

function service(): AnyService {
  return createTestService({
    serviceClass: DribbbleService,
    manifestPath: "manifest.json",
  });
}

describe("DribbleService", () => {
  test("test fetch shots", () => {
    const result = service()
      .getFeature(PostFeature)
      .fetchFreshList({ pageKey: null });
    expect(result.isOk()).toBe(true);
    expect(result.data).toHaveLength(24);
  });

  test("test fetch user", () => {
    const result = service()
      .getFeature(UserFeature)
      .fetchById({ userId: "Ramotion" });
    expect(result.isOk()).toBe(true);
    expect(result.data?.name).toBe("Ramotion");
  });
});
