#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const process = require("process");
const child_process = require("child_process");
const forEachProjects = require("./forEachProjects.js");

const HELP = `This script will do:
1. Check and install yarn (if not)
2. Install dependencies for projects in the 'js/any-***' and 'js/services/' folder
3. Build local dependencies, e.g. 'js/any-service-api', 'js/any-service-test'
4. Link 'js/any-service-cli' so you can use the 'any-service-cli' command to create a new project

Options:
--yarn        Specify the yarn version
--help, -h    Print help messages`;

let YARN_VERSION = "3.3.0";

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

  if (args[0].startsWith("--yarn=")) {
    YARN_VERSION = args[0].substring(7);
  }
}

function err(msg) {
  if (err != null) {
    console.error(msg);
  }
  process.exit(1);
}

if (!fs.existsSync(JS_DIR)) {
  err(
    "'js' folder is not found, Are you running this script another location " +
      "rather than the project's root directory?"
  );
  return;
}

function initYarn() {
  function checkYarn() {
    try {
      const currVer = child_process.execSync("yarn -v").toString().trim();
      return currVer === YARN_VERSION;
    } catch (e) {
      return false;
    }
  }

  function checkCorepack() {
    try {
      child_process.execSync("corepack -v");
      return true;
    } catch (e) {
      return false;
    }
  }

  function installCorepack() {
    console.log("Installing corepack...");
    child_process.execSync("npm i -g corepack");
  }

  function installYarn() {
    if (!checkCorepack()) {
      installCorepack();
    }
    try {
      child_process.execSync("corepack enable");
    } catch (e) {
      console.warn(
        "Unable to run 'corepack enable', 'yarn' may fail to install. \nPlease " +
          "try to run this script in administrator mode if it fails.\n"
      );
    }
    console.log(`Installing yarn ${YARN_VERSION}`);
    child_process.execSync(`corepack prepare yarn@${YARN_VERSION} --activate`);
    console.log();
  }

  if (!checkYarn()) {
    installYarn();
    initYarn();
  }
}

function installDependencies() {
  forEachProjects(
    (projectDir) => {
      process.chdir(projectDir);
      console.log(
        "Install dependencies for " + path.relative(JS_DIR, projectDir)
      );
      child_process.execSync("yarn", { stdio: "inherit" });
      console.log();
    },
    () => {
      console.warn("No projects found");
    },
    true
  );
}

function buildLocalDependencies() {
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
      console.log(
        "Building local dependency: " + path.relative(JS_DIR, projectDir)
      );
      process.chdir(projectDir);
      child_process.execSync("yarn tsc", { stdio: "inherit" });
      console.log();
    }
  }
}

function initBinLinks() {
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

initYarn();
installDependencies();
buildLocalDependencies();
initBinLinks();

const end = Date.now();
console.log();
console.log("Setup finished in " + ((end - start) / 1000).toFixed(3) + "s");
