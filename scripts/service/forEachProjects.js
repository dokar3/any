const fs = require("fs");
const path = require("path");
const process = require("process");

function err(msg) {
  if (err != null) {
    console.error(msg);
  }
  process.exit(1);
}

const DEPENDENCY_PROJECTS = [
  "any-service-api",
  "any-service-compile",
  "any-service-testing",
  "any-service-runner",
  "any-service-cli",
];

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
 * @param {boolean} [includeDependencyProjects] If true, 'js/any-***' projects will be included,
 * and will be evaluated first. Defaults to false.
 */
function forEachProjects(action, onEmpty, includeDependencyProjects) {
  const currentDir = process.cwd();
  const jsDir = path.join(currentDir, "js");
  if (!fs.existsSync(jsDir)) {
    err(
      "'js' folder is not found, Are you running this script another location " +
        "rather than the project's root directory?"
    );
    return;
  }

  const allProjects = [];
  if (includeDependencyProjects === true) {
    for (const proj of DEPENDENCY_PROJECTS) {
      const projectDir = path.join(jsDir, proj);
      if (!fs.existsSync(projectDir)) {
        err(`Dependency project ${proj} does not exist, path: ${projectDir}`);
        return;
      }
      allProjects.push(projectDir);
    }
  }

  const servicesDir = path.join(jsDir, "services");
  const serviceProjects = fs
    .readdirSync(servicesDir)
    .filter((dir) => {
      const packageFile = path.join(servicesDir, dir, "package.json");
      return fs.existsSync(packageFile);
    })
    .map((dir) => path.join(servicesDir, dir));
  allProjects.push(...serviceProjects);

  if (allProjects.length === 0) {
    onEmpty();
    return;
  }

  allProjects.forEach(action);
}

module.exports = forEachProjects;
