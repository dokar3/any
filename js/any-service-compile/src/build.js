import archiver from "archiver";
import clc from "cli-color";
import fs from "fs";
import path from "path";
import process from "process";
import { compileJsSources } from "./compile.js";
import Handlebars from "handlebars";
import * as dotenv from "dotenv";
import crypto from "crypto";

dotenv.config();

function zipDirFiles(dir, out, onFinished) {
  var archive = archiver("zip", {
    gzip: true,
    zlib: { level: 9 },
  });

  archive.on("error", function (err) {
    throw err;
  });

  archive.on("finish", () => {
    if (onFinished) {
      onFinished();
    }
  });

  const output = fs.createWriteStream(out);
  archive.pipe(output);

  fs.readdirSync(dir).forEach((filename) => {
    const filepath = path.join(dir, filename);
    if (fs.lstatSync(filepath).isDirectory()) {
      archive.directory(filepath, false);
    } else {
      archive.file(filepath, { name: filename });
    }
  });

  archive.finalize();
}

/**
 * @typedef BuildOutputs The build outputs.
 *
 * @property {string} manifest Path of the output manifest.
 * @property {string} dir Path of the output directory.
 * @property {string} [zip] Path of the output zip.
 */

/**
 * Build service.
 *
 * @param {string} outputDir Output directory.
 * @param {string} platform Build platform, 'android' and 'browser are supported.
 * @param {boolean} shouldPack Should pack build outputs to a zip archive.
 * @param {string[]} [manifestNames] Specify the service manifest and source to compile. Will compile
 * all manifests if null or undefined
 * @param {boolean} [minimize] Enable the minimize optimization. Defaults to true.
 * @returns {Promise<BuildOutputs>} Build Promise object.
 */
function buildService(
  outputDir,
  platform,
  shouldPack,
  manifestNames,
  minimize
) {
  const currentDir = process.cwd();

  if (!path.isAbsolute(outputDir)) {
    outputDir = path.resolve(currentDir, outputDir);
  }
  if (fs.existsSync(outputDir)) {
    fs.rmSync(outputDir, { recursive: true });
  }
  fs.mkdirSync(outputDir);

  console.log("Compiling service...");
  console.log("Output dir: " + outputDir);
  console.log();

  let manifestsToCompile = [];
  if (Array.isArray(manifestNames) && manifestNames.length > 0) {
    manifestsToCompile = manifestNames
      .map((dirName) => path.join(currentDir, dirName))
      .filter((filepath) => {
        return fs.existsSync(filepath) && fs.lstatSync(filepath).isFile();
      });
    if (manifestsToCompile.length === 0) {
      throw new Error(
        "Nothing to compile, make sure your specified manifests exist" +
          "in the project root directory. manifest: " +
          JSON.stringify(manifestNames)
      );
    }
  } else {
    manifestsToCompile = fs
      .readdirSync(currentDir)
      .map((dirName) => path.join(currentDir, dirName))
      .filter((filepath) => {
        const isFile = fs.lstatSync(filepath).isFile();
        const filename = path.basename(filepath);
        return (
          isFile &&
          filename.startsWith("manifest.") &&
          filename.endsWith(".json")
        );
      });
    if (manifestsToCompile.length === 0) {
      throw new Error(
        "Nothing to compile, make sure there is least 1 manifest.json file " +
          "in the project root directory."
      );
    }
  }

  const buildInfo = [];

  function addBuildInfo(manifest, outputs) {
    buildInfo.push({
      name: manifest.name,
      version: manifest.version,
      platform: platform,
      minimize: minimize ?? false,
      buildTime: manifest.buildTime,
      outputs: outputs,
    });
  }

  function writeBuildInfo() {
    const text = JSON.stringify(buildInfo, null, 2);
    fs.writeFileSync(path.join(outputDir, "build_info.json"), text);
  }

  const compilations = manifestsToCompile.map(
    (manifestFile) =>
      new Promise(async (resolve, reject) => {
        console.log("Processing " + manifestFile);

        // Read manifest.json
        const dir = path.dirname(manifestFile);
        const rawManifestText = fs.readFileSync(manifestFile, "utf-8");
        if (rawManifestText.length == 0) {
          reject("Empty manifest file");
          return;
        }

        // Replace env vars in the manifest.json
        const template = Handlebars.compile(rawManifestText);
        const manifestText = template(process.env);

        const manifest = JSON.parse(manifestText);

        const uncompressedDirName =
          manifest.name.replace(/[ <>:"\/\\?|*]/g, "") +
          "-" +
          manifest.version +
          "-" +
          platform;
        const uncompressedOutputDir = path.resolve(
          outputDir,
          uncompressedDirName
        );
        fs.mkdirSync(uncompressedOutputDir);

        // Add build time
        manifest.buildTime = Date.now();

        // Copy resources
        const resourceFields = ["icon", "headerImage", "changelog"];
        for (const field of resourceFields) {
          const res = manifest[field];
          if (!res || res.startsWith("http://") || res.startsWith("https://")) {
            continue;
          }

          const resPath = path.join(dir, res);
          if (!fs.existsSync(resPath)) {
            continue;
          }

          const resFilename = path.basename(res);
          const destResPath = path.join(uncompressedOutputDir, resFilename);
          fs.copyFileSync(resPath, destResPath);
          // Update icon path in manifest
          manifest[field] = resFilename;
        }

        // Check source file
        if (!manifest.main) {
          const msg = "Invalid manifest file: 'main' is not specified";
          console.error(clc.red(msg));
          reject(msg);
          return;
        }
        const serviceSourceFile = path.join(dir, manifest.main);
        if (!fs.existsSync(serviceSourceFile)) {
          const msg = "Service source file not found: " + serviceSourceFile;
          console.error(clc.red(msg));
          reject(msg);
          return;
        }
        if (!fs.lstatSync(serviceSourceFile).isFile()) {
          const msg = "Invalid service source file: " + serviceSourceFile;
          console.error(clc.red(msg));
          reject(msg);
          return;
        }

        // Recreate entry point
        const entryPointPath = generateEntryPoint(serviceSourceFile);

        const outputFilename = path
          .basename(serviceSourceFile)
          .replace(/.ts$/, ".js");

        try {
          // Compile service source
          await compileJsSources(
            [entryPointPath],
            uncompressedOutputDir,
            outputFilename,
            platform,
            null,
            minimize === true
          );
        } finally {
          // Delete temporary entry point file
          fs.rmSync(entryPointPath);
        }

        // Update source path in manifest
        manifest.main = outputFilename;
        const outputManifest = path.join(
          uncompressedOutputDir,
          path.basename(manifestFile)
        );

        // Calc and set main source file checksums
        const mainJsFilepath = path.join(uncompressedOutputDir, outputFilename);
        const mainJsBuffer = fs.readFileSync(mainJsFilepath);
        const md5 = crypto.createHash("md5").update(mainJsBuffer).digest("hex");
        const sha1 = crypto
          .createHash("sha1")
          .update(mainJsBuffer)
          .digest("hex");
        const sha256 = crypto
          .createHash("sha256")
          .update(mainJsBuffer)
          .digest("hex");
        const sha512 = crypto
          .createHash("sha512")
          .update(mainJsBuffer)
          .digest("hex");
        const checksums = {
          md5: md5,
          sha1: sha1,
          sha256: sha256,
          sha512: sha512,
        };
        manifest.mainChecksums = checksums;

        const updatedManifestText = JSON.stringify(manifest);
        // Write updated manifest
        fs.writeFileSync(outputManifest, updatedManifestText);

        const outputs = {
          manifest: outputManifest,
          dir: uncompressedOutputDir,
        };

        function ok() {
          // Add build info
          addBuildInfo(manifest, outputs);
          if (buildInfo.length == manifestsToCompile.length) {
            // Write build info
            writeBuildInfo();
          }
          resolve(outputs);
        }

        if (shouldPack === true) {
          // Zip
          const zipName = uncompressedDirName + ".zip";
          const zipFile = path.join(outputDir, zipName);
          console.log("Packing service to " + zipFile);
          zipDirFiles(uncompressedOutputDir, zipFile, () => {
            outputs.zip = zipFile;
            ok();
          });
        } else {
          ok();
        }
      })
  );
  return Promise.all(compilations);
}

/**
 * Generate a entry point which initiates the service.
 *
 * @param {string} originalEntryPointPath Original main path.
 *
 * @returns {string} The path of temporary entry point file.
 */
function generateEntryPoint(originalEntryPointPath) {
  const dir = path.dirname(originalEntryPointPath);
  const filename = path.parse(originalEntryPointPath).name;
  const code = `
    import { _createService, ServiceManifest } from "any-service-api";
    import { features } from "./${filename}";

    globalThis.createService = function(manifest: ServiceManifest, configs: any) {
      return _createService(
        features,
        manifest,
        configs,
        manifestUpdater ?? globalThis.manifestUpdater,
        configsUpdater ?? globalThis.configsUpdater,
        progressUpdater ?? globalThis.progressUpdater,
      );
    }
    `;
  const tempFile = path.join(dir, filename + "." + crypto.randomUUID() + ".ts");
  fs.writeFileSync(tempFile, code);
  return tempFile;
}

export { buildService };
