#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const process = require("process");
const child_process = require("child_process");
const forEachProjects = require("./forEachProjects.js");

const HELP = `Build all builtin services
Example:
  node buildAll.js --platform=PLATFORM --output=/PATH/TO/OUTPUT/DIR

Options:
--platform            [Required] The target platform, could be 'android', 'desktop' or 'browser'.
--output              [Required] The output dir to copy outputs.
--file-prefix         The prefix for the main source file and other resource files.
--minimize            Enable the minimize optimization. Defaults to true.
--ignore-unchanged    Force to run build command in projects even if source and output are not changed.
--help, -h            Print help messages`;

const args = process.argv.slice(2);

if (args[0] === "--help" || args[2] === "-h" || args.length <= 2) {
  console.log(HELP);
  return;
}

function err(msg) {
  if (err != null) {
    console.error(msg);
  }
  process.exit(1);
}

const CURRENT_DIR = process.cwd();
const JS_DIR = path.join(CURRENT_DIR, "js");
const SERVICES_DIR = path.join(JS_DIR, "services");

if (!fs.existsSync(JS_DIR)) {
  err(
    "'js' folder is not found, Are you running this script another location " +
      "rather than the project's root directory?"
  );
  return;
}

// -platform
const argPlatform = args.find((arg) => arg.startsWith("--platform="));
if (!argPlatform) {
  err("No platform specified, use '-platform' to specify the target platform");
}
const PLATFORM = argPlatform.split("=")[1];

// -output
const argOutputDir = args.find((arg) => arg.startsWith("--output="));
if (!argOutputDir) {
  err(
    "No output path specified, use '-output=path/to/output/dir' to specify the path"
  );
}
const OUTPUT_DIR = argOutputDir.split("=")[1];
if (!fs.existsSync(OUTPUT_DIR)) {
  err("Output path does not exist, path: " + OUTPUT_DIR);
}

// -file-prefix
const argFilePrefix = args.find((arg) => arg.startsWith("--file-prefix="));
let FILE_PREFIX = null;
if (argFilePrefix) {
  FILE_PREFIX = argFilePrefix.split("=")[1];
}
if (FILE_PREFIX === null) {
  switch (PLATFORM) {
    case "android": {
      FILE_PREFIX = "file:///android_asset/";
      break;
    }
    default:
  }
}

// -minimize
const argMinimize = args.find((arg) => arg.startsWith("--minimize="));
let MINIMIZE = true;
if (argMinimize) {
  MINIMIZE = argMinimize.split("=")[1] === "true";
}

// --ignore-unchanged
const IGNORE_UNCHANGED =
  args.find((arg) => arg === "--ignore-unchanged") !== undefined;

function isManifestFile(filepath) {
  const filename = path.basename(filepath);
  return (
    filename.startsWith("manifest.") &&
    filename.endsWith(".json") &&
    fs.lstatSync(filepath).isFile()
  );
}

/**
 * Replace resource/source paths with platform assets path and return the updated manifest.
 */
function processOutputManifest(filepath, projectOutputDir) {
  const manifest = JSON.parse(fs.readFileSync(filepath, "utf-8"));

  const dir = path.relative(OUTPUT_DIR, projectOutputDir);

  const resourceFields = ["main", "icon", "headerImage", "changelog"];
  for (const field of resourceFields) {
    const res = manifest[field];
    if (!res || res.startsWith("http://") || res.startsWith("https://")) {
      continue;
    }
    const resPath = path.join(path.dirname(filepath), res);
    if (!fs.existsSync(resPath)) {
      continue;
    }
    const updatedPath =
      FILE_PREFIX +
      path.join(dir, manifest[field]).split(path.sep).join(path.posix.sep);
    manifest[field] = updatedPath;
  }

  return manifest;
}

/**
 * Copy built service to output dir.
 *
 * @returns {object[]} Manifests.
 */
function copyBuiltServiceToOutputDir(projectDir, outputServicesPath) {
  const projectBuildInfoFile = path.join(projectDir, "dist/build_info.json");
  if (!fs.existsSync(projectBuildInfoFile)) {
    console.warn("dist/build_info.json not found, skip.");
    return;
  }

  const dirName = path.basename(projectDir).toLowerCase();
  const targetDir = path.join(outputServicesPath, dirName);

  if (fs.existsSync(targetDir)) {
    fs.rmSync(targetDir, { recursive: true, force: true });
  }
  fs.mkdirSync(targetDir, { recursive: true });

  const builds = JSON.parse(fs.readFileSync(projectBuildInfoFile));

  const serviceManifests = [];

  builds
    .map((buildInfo) => buildInfo.outputs.dir)
    .forEach((dir) => {
      fs.readdirSync(dir)
        .map((filename) => path.join(dir, filename))
        .forEach((filepath) => {
          if (isManifestFile(filepath)) {
            serviceManifests.push(processOutputManifest(filepath, targetDir));
          } else {
            const outFile = path.join(targetDir, path.basename(filepath));
            fs.copyFileSync(filepath, outFile);
          }
        });
    });

  return serviceManifests;
}

function getBuildInfoFile() {
  const dir = path.join(SERVICES_DIR, ".build");
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir);
  }
  return path.join(dir, "build_info.json");
}

/**
 * Read build infos.
 *
 * @returns {object} build info object
 */
function readBuildInfo() {
  const filepath = getBuildInfoFile();
  if (fs.existsSync(filepath)) {
    try {
      return JSON.parse(fs.readFileSync(filepath));
    } catch (e) {
      return null;
    }
  } else {
    return null;
  }
}

/**
 * Write build info to local file.
 *
 * @param {object[]} buildInfo Build infos.
 * @param {string} project The service project directory.
 */
function addBuildInfo(buildInfos, projectDir) {
  const distDir = path.join(projectDir, "dist");
  const excludes = [path.join(projectDir, "node_modules")];

  const files = [];
  const filepaths = [distDir, projectDir];

  for (const filepath of filepaths) {
    const stat = fs.statSync(filepath);
    let lastModifiedAt;
    if (stat.isDirectory()) {
      lastModifiedAt = calcDirLastModifiedAt(filepath, excludes);
    } else {
      lastModifiedAt = stat.mtimeMs;
    }
    files.push({
      path: filepath,
      lastModifiedAt: parseInt(lastModifiedAt),
    });
  }

  const buildInfo = {
    projectDir: projectDir,
    files: files,
  };

  const index = buildInfos.findIndex((info) => info.projectDir === projectDir);
  if (index !== -1) {
    buildInfos[index] = buildInfo;
  } else {
    buildInfos.push(buildInfo);
  }
}

/**
 * Calculate the latest last modified time of a directory and its contents.
 *
 * @param {string} directory Directory path.
 * @param {string[]|null} [excludes] Excluded filepaths.
 * @returns {number} The last modified time.
 */
function calcDirLastModifiedAt(directory, excludes) {
  const currStat = fs.statSync(directory);
  let lastModifiedAt = currStat.mtimeMs;

  if (currStat.isFile()) {
    return lastModifiedAt;
  }

  fs.readdirSync(directory, null).forEach((filename) => {
    const filepath = path.join(directory, filename);

    if (Array.isArray(excludes) && excludes.includes(filepath)) {
      return;
    }

    const stat = fs.statSync(filepath);

    if (stat.mtimeMs > lastModifiedAt) {
      lastModifiedAt = stat.mtimeMs;
    }

    if (stat.isDirectory()) {
      const t = calcDirLastModifiedAt(filepath);
      if (t > lastModifiedAt) {
        lastModifiedAt = t;
      }
    }
  });

  return lastModifiedAt;
}

/**
 * Check if the project source has changed.
 *
 * @param {object[]} buildInfo Build infos.
 * @param {string} projectDir Service project directory.
 * @returns {boolean}
 */
function isSourceChanged(buildInfos, projectDir) {
  const info = buildInfos.find((info) => info.projectDir === projectDir);
  if (!info) {
    return true;
  }
  if (!info.files) {
    return true;
  }
  for (const file of info.files) {
    const filepath = file.path;
    const lastModifiedAt = file.lastModifiedAt;
    if (!fs.existsSync(filepath)) {
      return true;
    }
    const stat = fs.statSync(filepath);
    let actualLastModifiedAt;
    if (stat.isDirectory()) {
      const excludes = [path.join(projectDir, "node_modules")];
      actualLastModifiedAt = parseInt(
        calcDirLastModifiedAt(filepath, excludes)
      );
      if (actualLastModifiedAt != lastModifiedAt) {
        console.log("dir changed: " + filepath);
      }
    } else {
      actualLastModifiedAt = parseInt(stat.mtimeMs);
    }
    if (actualLastModifiedAt != lastModifiedAt) {
      return true;
    }
  }
  return false;
}

function addOrUpdateManifests(allManifests, newManifests) {
  for (const manifest of newManifests) {
    const index = allManifests.findIndex((m) => m.id === manifest.id);
    if (index !== -1) {
      allManifests[index] = manifest;
    } else {
      allManifests.push(manifest);
    }
  }
}

function buildAll() {
  const start = Date.now();

  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR);
  }

  const outputServicesPath = path.join(OUTPUT_DIR, "services");
  if (!fs.existsSync(outputServicesPath)) {
    fs.mkdirSync(outputServicesPath);
  }

  const serviceManifestsFile = path.join(OUTPUT_DIR, "services.json");

  let projectCount = 0;

  const allServiceManifests = [];

  if (fs.existsSync(serviceManifestsFile)) {
    const manifests = JSON.parse(fs.readFileSync(serviceManifestsFile));
    [].push.apply(allServiceManifests, manifests);
  }

  let buildItems = [];
  let buildItemsForRead = [];

  const buildInfo = readBuildInfo();
  if (buildInfo) {
    if (buildInfo.platform === PLATFORM && buildInfo.minimize === MINIMIZE) {
      buildItems = buildInfo.items;
      buildItemsForRead = buildItems.slice();
    }
  }

  //  Copy source and resource files to output dir
  function copyCompiledSourceAndResources(projectDir) {
    const manifests = copyBuiltServiceToOutputDir(
      projectDir,
      outputServicesPath
    );
    addOrUpdateManifests(allServiceManifests, manifests);
  }

  forEachProjects(
    (projectDir) => {
      projectCount++;

      console.log(`Building project /${path.basename(projectDir)} ...`);

      if (
        !IGNORE_UNCHANGED &&
        !isSourceChanged(buildItemsForRead, projectDir)
      ) {
        console.log("UP-TO-DATE");
        copyCompiledSourceAndResources(projectDir);
        return;
      }

      process.chdir(projectDir);
      child_process.execSync(
        `yarn run build-${PLATFORM} -minimize=${MINIMIZE}`,
        {
          stdio: "inherit",
        }
      );

      console.log("Coping to output dir...");
      copyCompiledSourceAndResources(projectDir);

      addBuildInfo(buildItems, projectDir);

      console.log("Done");
      console.log();
    },
    () => {
      console.log("No projects to build");
    }
  );

  // Write build infos
  const latestBuildInfo = {
    platform: PLATFORM,
    minimize: MINIMIZE,
    items: buildItems,
  };
  fs.writeFileSync(
    getBuildInfoFile(),
    JSON.stringify(latestBuildInfo, null, 2)
  );

  if (allServiceManifests.length > 0) {
    console.log("Writing service manifests...");
    // Write manifests to services.json
    const json = JSON.stringify(allServiceManifests);
    fs.writeFileSync(serviceManifestsFile, json);
  }

  const timeElapse = ((Date.now() - start) / 1000).toFixed(1);

  console.log();
  console.log(
    `Build services task completed in (${timeElapse}s), \
    ${projectCount} project(s), \
    ${allServiceManifests.length} manifest(s).`
  );
  process.exit(0);
}

buildAll();
