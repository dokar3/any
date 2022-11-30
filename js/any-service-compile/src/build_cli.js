#!/usr/bin/env node
import { buildService } from "./build.js";
import { getCommandArgs } from "./utils.js";

const args = getCommandArgs();

const platform = args.p ?? args.platform;
if (!platform) {
  throw "Platform is not defined, specify it by using '-p=[android|desktop|browser]' or '--platform=[PLATFORM]'";
}

const outputDir = args.o ?? args.output;
if (!outputDir) {
  throw "Output dir is not defined, specify it by using '-o=OUTPUT_DIR' or '--output=OUTPUT_DIR'";
}

let minimize = true;
if (args.m || args.minimize) {
  minimize = args.m === "true" || args.minimize === "true";
}

buildService(outputDir, platform, true, null, minimize);
