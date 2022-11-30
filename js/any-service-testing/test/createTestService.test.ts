import { describe, expect, test } from "@jest/globals";
import { AnyService } from "any-service-api";
import { createTestService } from "../src/createTestService";

class MyService extends AnyService {}

describe("createTestService()", () => {
  test("test create service", () => {
    const service = createTestService({
      serviceClass: MyService,
      manifestPath: "./test/test_manifest.json",
    });
    expect(service.manifest.name).toBe("MyService");
    expect(service.manifest.id).toBe("me.service");
  });
});
