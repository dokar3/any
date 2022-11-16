import { describe, expect, test } from "@jest/globals";
import { PostFeature } from "any-api";
import { createTestService } from "any-testing";
import MyService from "../src/MyService";

describe("MyService", () => {
  test("test fetch posts", () => {
    const service = createTestService({
      serviceClass: MyService,
      manifestPath: "manifest.json",
    });
    const result = service
      .getFeature(PostFeature)
      .fetchFreshList({ pageKey: null });
    expect(result.isOk()).toBe(true);
  });
});
