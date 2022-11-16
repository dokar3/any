import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

/**
 * File filter.
 *
 * @callback FileFilter
 * @param {string} src Source file path.
 * @param {string} dest Target file path.
 * @param {boolean} isDirectory True if current file is a directory, false if it is a file.
 * @returns {boolean} Skip if returns false.
 */

// Based on https://stackoverflow.com/a/22185855
/**
 * Look ma, it's cp -R.
 * @param {string} src  The path to the thing to copy.
 * @param {string} dest The path to the new copy.
 * @param {boolean} overwrite Should overwrite if file/dir already exists.
 * @param {FileFilter} filter The file filter.
 */
var copyRecursiveSync = function (src, dest, overwrite, filter) {
  var exists = fs.existsSync(src);
  var stats = exists && fs.statSync(src);
  var isDirectory = exists && stats.isDirectory();
  if (!filter(src, dest, isDirectory)) {
    return;
  }
  if (isDirectory) {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest);
    }
    fs.readdirSync(src).forEach(function (childItemName) {
      copyRecursiveSync(
        path.join(src, childItemName),
        path.join(dest, childItemName),
        overwrite,
        filter
      );
    });
  } else {
    if (overwrite === true) {
      fs.copyFileSync(src, dest);
    } else {
      fs.copyFileSync(src, dest, fs.constants.COPYFILE_EXCL);
    }
  }
};

/**
 * Convert windows path to posix path.
 *
 * @param {string} originalPath Windows or posix path.
 * @returns {string} Posix path.
 */
function toPosixPath(originalPath) {
  return originalPath.split(path.sep).join(path.posix.sep);
}

/**
 * Get version string from 'any-api'
 *
 * @return {string} The version string.
 */
function getCurrentApiVersion() {
  const __filename = fileURLToPath(import.meta.url);
  const __dirname = path.dirname(__filename);
  const apiDir = path.join(__dirname, "../../any-api");
  const json = fs.readFileSync(path.join(apiDir, "package.json"));
  return JSON.parse(json).version;
}

export { copyRecursiveSync, toPosixPath, getCurrentApiVersion };
