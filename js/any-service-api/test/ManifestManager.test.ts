import { ServiceManifest } from "../src";
import { ManifestManager } from "../src/manifest/ManifestManager";
import { ManifestUpdater } from "../src/manifest/ManifestUpdater";

const globalManifest = {
  id: "my.app.id",
  name: "My app",
} as ServiceManifest;

class TestManifestUpdater extends ManifestUpdater {
  updateName(newValue) {
    globalManifest.name = newValue;
  }
}

const manifestUpdater = new TestManifestUpdater();
const manifestManager = new ManifestManager(globalManifest, manifestUpdater);

describe("ManifestManager", () => {
  test("test update fields", () => {
    const manifest = manifestManager.observableManifest();
    manifest.name = "New name";
    expect(globalManifest.name).toBe("New name");
  });
});
