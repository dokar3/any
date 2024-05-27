import { createTestService } from "any-service-testing";
import { describe, expect, test } from "bun:test";
import { features } from "../src/main";

describe("MyService", () => {
  test("test fetch posts", () => {
    const service = createTestService({
      features: features,
      manifestPath: "manifest.json",
    });
    expect(() => service.features.post.fetch({ url: "whatever" })).toThrow(
      "Not implemented yet."
    );
  });
});
