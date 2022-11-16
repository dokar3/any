import { describe, expect, test } from "@jest/globals";
import { Feature } from "../src/service/feature/Feature";
import { ConfigFeature } from "../src/service/feature/ConfigFeature";
import { PostFeature } from "../src/service/feature/PostFeature";
import { UserFeature } from "../src/service/feature/UserFeature";
import { Features } from "../src/service/Features";

class MyPostFeature extends PostFeature {}

class MyUserFeature extends UserFeature {}

describe("Features", () => {
  test("test register and get", () => {
    const features = new Features(null);
    features.add(MyPostFeature);
    expect(features.getOrCreate(MyPostFeature)).toBeInstanceOf(MyPostFeature);
    expect(features.getOrCreate(PostFeature)).toBeInstanceOf(MyPostFeature);
    expect(() => features.getOrCreate(UserFeature)).toThrow();
    features.add(MyUserFeature);
    expect(features.getOrCreate(MyUserFeature)).toBeInstanceOf(MyUserFeature);
    expect(features.getOrCreate(UserFeature)).toBeInstanceOf(MyUserFeature);
  });

  test("test register builtin services", () => {
    const features = new Features(null);
    expect(() => features.add(Feature)).toThrow();
    expect(() => features.add(ConfigFeature)).toThrow();
    expect(() => features.add(PostFeature)).toThrow();
    expect(() => features.add(UserFeature)).toThrow();
  });

  test("test lazy creation", () => {
    const features = new Features(null);
    features.add(MyPostFeature);
    const s1 = features.getOrCreate(MyPostFeature);
    const s2 = features.getOrCreate(PostFeature);
    expect(s2).toStrictEqual(s1);
  });

  test("test isRegistered()", () => {
    const features = new Features(null);
    expect(features.isAdded(MyPostFeature)).toBe(false);
    features.add(MyPostFeature);
    expect(features.isAdded(MyPostFeature)).toBe(true);
  });
});
