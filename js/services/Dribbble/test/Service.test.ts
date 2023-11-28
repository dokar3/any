import { describe, expect, test } from "@jest/globals";
import { AnyService } from "any-service-api";
import { createTestService } from "any-service-testing";
import { features } from "../src/main";

function service(): AnyService {
  return createTestService({
    features: features,
    manifestPath: "manifest.json",
  });
}

describe("DribbleService", () => {
  test("test fetch fresh shots", () => {
    const result = service().features.post.fetchFreshList({ pageKey: null });
    expect(result.isOk()).toBe(true);
    expect(result.data).toHaveLength(24);
  });

  test("test fetch shot", () => {
    const url = "https://dribbble.com/shots/23117340-Winter";
    const result = service().features.post.fetch({ url: url });
    expect(result.isOk()).toBe(true);
    const post = result.data!!;
    expect(post.title).toBe("Winter  ❄️🌲🌨️⛄️🧣🧤");
    expect(post.content).toHaveLength(2);
  });

  test("test fetch user", () => {
    const result = service().features.user!.fetchById({ userId: "Ramotion" });
    expect(result.isOk()).toBe(true);
    expect(result.data?.name).toBe("Ramotion");
  });
});
