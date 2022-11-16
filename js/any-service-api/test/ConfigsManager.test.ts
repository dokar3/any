import { ConfigsUpdater } from "../src/config/ConfigsUpdater";
import { ConfigsManager } from "../src/config/ConfigsManager";

let globalConfigs = {
  name: "Freddy",
  age: 15,
};

const configsUpdater = new ConfigsUpdater((configs) => {
  globalConfigs = configs;
});
const configsManager = new ConfigsManager(globalConfigs, configsUpdater);

describe("ConfigsManager", () => {
  test("test configs updating", () => {
    const configs = configsManager.observableConfigs();

    configs.name = "Jesse";
    configs.age = 20;

    expect(globalConfigs.name).toBe("Jesse");
    expect(globalConfigs.age).toBe(20);
  });
});
