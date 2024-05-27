import { NotImplementedError, ServiceFeatures } from "any-service-api";
import { createTestService } from "../src/createTestService";
import { describe, test, expect } from "bun:test";

const features: ServiceFeatures = {
  post: {
    fetch: () => {
      throw new NotImplementedError("Not implemented.");
    },
    fetchFreshList: () => {
      throw new NotImplementedError("Not implemented.");
    },
  },
};

describe("createTestService()", () => {
  test("test create service", () => {
    const service = createTestService({
      features: features,
      manifestPath: "./test/test_manifest.json",
    });
    expect(service.features.post.fetch).toThrow("Not implemented.");
    expect(service.manifest.name).toBe("MyService");
    expect(service.manifest.id).toBe("me.service");
  });
});
