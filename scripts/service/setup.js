#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const process = require("process");
const child_process = require("child_process");
const forEachProjects = require("./forEachProjects.js");

const HELP = `This script will do:
1. Install dependencies for projects in the 'js/any-***' and 'js/services/' folder
2. Build local dependencies, e.g. 'js/any-service-api', 'js/any-service-test'
3. Link 'js/any-service-cli' so you can use the 'any-service-cli' command to create a new project

Options:
--help, -h    Print help messages`;

const BUN_LINK_PATHS = [
  "./any-service-api",
  "./any-service-compile",
  "./any-service-testing",
  "./any-service-runner",
]

const PRE_BUILD_PATHS = [
  "./any-service-api",
  "./any-service-testing",
  "./any-service-compile",
];

const BIN_LINKS = [
  {
    testCmd: "any-service-cli --test",
    path: "./any-service-cli",
  },
];

const CURRENT_DIR = process.cwd();
const JS_DIR = path.join(CURRENT_DIR, "js");

const args = process.argv.slice(2);
if (args.length > 0) {
  switch (args[0]) {
    case "-h":
    case "--help": {
      console.log(HELP);
      return;
    }
  }
}

function err(msg) {
  if (err != null) {
    throw new Error(msg);
  }
  process.exit(1);
}

if (!fs.existsSync(JS_DIR)) {
  err(
    "'js' folder is not found, Are you running this script another location " +
      "rather than the project's root directory?\nExpected dir to exist: " + JS_DIR
  );
  return;
}

function checkBun() {
  child_process.execSync("bun -v", { stdio: "inherit" });
}

function bunLinkDependencies() {
  process.chdir(CURRENT_DIR);
  for (const p of BUN_LINK_PATHS) {
    const projectDir = path.join(JS_DIR, p);
    if (!fs.existsSync(projectDir)) {
      err("Pre-build local dependency does not exist, path: \n" + projectDir);
    }
    process.chdir(projectDir);
    console.log(`Creating link for '${p}'`);
    child_process.execSync("bun link", { stdio: "inherit" });
    console.log()
  }
}

function installDependencies() {
  process.chdir(CURRENT_DIR);
  forEachProjects(
    (projectDir) => {
      process.chdir(projectDir);
      console.log(
        "Install dependencies for " + path.relative(JS_DIR, projectDir)
      );
      child_process.execSync("bun install", { stdio: "inherit" });
      console.log();
    },
    () => {
      console.warn("No projects found");
    },
    true
  );
}

function buildLocalDependencies() {
  process.chdir(CURRENT_DIR);
  for (const p of PRE_BUILD_PATHS) {
    const projectDir = path.join(JS_DIR, p);
    if (!fs.existsSync(projectDir)) {
      err("Pre-build local dependency does not exist, path: \n" + projectDir);
    }

    const packageText = fs.readFileSync(path.join(projectDir, "package.json"));
    const packageInfo = JSON.parse(packageText);

    let shouldBuildMain = false;
    if (packageInfo.main != null) {
      const mainFile = path.join(projectDir, packageInfo.main);
      shouldBuildMain = !fs.existsSync(mainFile);
    }

    if (shouldBuildMain) {
      console.log("Building local dependency: ", p);
      process.chdir(projectDir);
      child_process.execSync("bun tsc", { stdio: "inherit" });
      console.log();
    }
  }
}

function initBinLinks() {
  process.chdir(CURRENT_DIR);
  for (const link of BIN_LINKS) {
    try {
      const r = child_process.execSync(link.testCmd);
    } catch (e) {
      console.log("Linking project " + link.path);
      process.chdir(path.join(JS_DIR, link.path));
      child_process.execSync("npm link --bin-links");
      console.log();
    }
  }
}

const start = Date.now();

checkBun();
bunLinkDependencies();
installDependencies();
buildLocalDependencies();
initBinLinks();

const end = Date.now();
console.log();
console.log("Setup finished in " + ((end - start) / 1000).toFixed(3) + "s");
