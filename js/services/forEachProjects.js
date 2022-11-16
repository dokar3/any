const fs = require("fs");
const path = require("path");
const process = require("process");

/**
 * Project foreach callback.
 *
 * @callback ForEachCallback
 * @param {string} projectDir The project directory.
 */

/**
 * For each all sub-project in current directory.
 *
 * @param {ForEachCallback} action The for each action.
 * @param {callback} [onEmpty] Optional empty project callback, will be called if no projects found.
 */
function forEachProjects(action, onEmpty) {
  const currentDir = process.cwd();

  const projects = fs
    .readdirSync(currentDir)
    .filter((dir) => {
      const packageFile = path.join(currentDir, dir, "package.json");
      return fs.existsSync(packageFile);
    })
    .map((dir) => path.join(currentDir, dir));

  if (projects.length === 0) {
    onEmpty();
    return;
  }

  projects.forEach(action);
}

module.exports = forEachProjects;
