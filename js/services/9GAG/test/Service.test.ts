import { describe, expect, test } from "@jest/globals";
import { AnyPostFeature } from "any-service-api";
import { createTestService } from "any-service-testing";
import NineGAGService from "../src/Service";

describe("MyService", () => {
  test("test fetch posts", () => {
    const service = createTestService({
      serviceClass: NineGAGService,
      manifestPath: "manifest.json",
      configs: {
        section: "home",
      },
    });
    const result = service
      .getFeature(AnyPostFeature)
      .fetchFreshList({ pageKey: null });
    expect(result.isOk()).toBe(true);
  });
});
