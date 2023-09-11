import { describe, expect, test } from "@jest/globals";
import { createTestService } from "any-service-testing";
import { features } from "../src/main";

describe("MyService", () => {
  test("test fetch posts", () => {
    const service = createTestService({
      features: features,
      manifestPath: "manifest.json",
      configs: {
        section: "home",
      },
    });
    const result = service.features.post.fetchFreshList({ pageKey: null });
    expect(result.isOk()).toBe(true);
  });
});
