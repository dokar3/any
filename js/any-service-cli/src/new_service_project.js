import inquirer from "inquirer";
import fs from "fs";
import path from "path";
import process from "process";
import Handlebars from "handlebars";
import { fileURLToPath } from "url";
import {
  copyRecursiveSync,
  getCurrentApiVersion,
  toPosixPath,
} from "./utils.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const POSTS_VIEW_TYPES = ["list", "grid", "card", "full_width"];

const QUESTIONS = [
  {
    name: "serviceId",
    type: "input",
    message: "Service id",
    default: "me.example",
  },
  {
    name: "serviceName",
    type: "input",
    message: "Service name",
    validate: (input) => {
      if (input) {
        return true;
      } else {
        return "Service name cannot be empty";
      }
    },
  },
  {
    name: "description",
    type: "input",
    message: "Description",
  },
  {
    name: "author",
    type: "input",
    message: "Author",
  },
  {
    name: "version",
    type: "input",
    message: "Version",
    default: "1.0.0",
  },
  {
    name: "minApiVersion",
    type: "input",
    message: "Min api version",
    default: getCurrentApiVersion(),
    validate: (input) => {
      if (input) {
        return true;
      } else {
        return "Minimum api version cannot be empty";
      }
    },
  },
  {
    name: "maxApiVersion",
    type: "input",
    message: "Max api version (optional)",
  },
  {
    name: "isPageable",
    type: "list",
    message: "Can this service load multiple pages?",
    choices: ["true", "false"],
    default: "true",
  },
  {
    name: "postsViewType",
    type: "list",
    message: "Post list view type",
    choices: POSTS_VIEW_TYPES,
    default: POSTS_VIEW_TYPES[0],
  },
  {
    name: "mediaAspectRatio",
    type: "input",
    message: "Default media (thumbnail) aspect ratio of post",
    default: "5:4",
  },
  {
    name: "license",
    type: "input",
    message: "License",
    default: "Apache-2.0",
  },
];

// https://stackoverflow.com/a/5827895
var walk = function (dir, done) {
  var results = [];
  fs.readdir(dir, function (err, list) {
    if (err) return done(err);
    var pending = list.length;
    if (!pending) return done(null, results);
    list.forEach(function (file) {
      file = path.resolve(dir, file);
      fs.stat(file, function (err, stat) {
        if (stat && stat.isDirectory()) {
          walk(file, function (err, res) {
            results = results.concat(res);
            if (!--pending) done(null, results);
          });
        } else {
          results.push(file);
          if (!--pending) done(null, results);
        }
      });
    });
  });
};

function newAnyService(templateDir, params) {
  const currentDir = process.cwd();
  const dirName = params.serviceName.replace(/[ <>:"\/\\?|*]/g, "");
  const projectDir = path.join(currentDir, dirName);

  if (fs.existsSync(projectDir)) {
    throw new Error(`There is already a directory named '${dirName}'.`);
  }

  const ignoreDirs = [
    path.join(templateDir, "dist"),
    path.join(templateDir, "node_modules"),
  ];

  const ignoreFiles = [path.join(templateDir, "package-lock.json")];

  // Copy template to project dir
  copyRecursiveSync(
    templateDir,
    projectDir,
    false,
    (src, _dest, isDirectory) => {
      if (isDirectory) {
        return !ignoreDirs.includes(src);
      } else {
        return !ignoreFiles.includes(src);
      }
    }
  );

  // Resolve paths of file dependencies
  params.packageName = params.serviceName.toLowerCase().replace(/ /g, "-");
  const anyApiPath = path.join(__dirname, "../../any-service-api");
  const anyCompilePath = path.join(__dirname, "../../any-service-compile");
  const anyRunnerPath = path.join(__dirname, "../../any-service-runner");
  const anyTestingPath = path.join(__dirname, "../../any-service-testing");
  params.anyApiPath = toPosixPath(path.relative(projectDir, anyApiPath));
  params.anyCompilePath = toPosixPath(
    path.relative(projectDir, anyCompilePath)
  );
  params.anyRunnerPath = toPosixPath(path.relative(projectDir, anyRunnerPath));
  params.anyTestingPath = toPosixPath(
    path.relative(projectDir, anyTestingPath)
  );

  // Replace template strings
  walk(projectDir, (err, results) => {
    if (err) {
      throw new Error(err);
    }
    results
      .filter((filepath) => filepath.endsWith(".json"))
      .forEach((filepath) => {
        const source = fs.readFileSync(filepath).toString();
        const template = Handlebars.compile(source);
        const output = template(params);
        fs.writeFileSync(filepath, output);
      });
  });

  console.log("Service project created: " + projectDir);
  console.log("Run the following commands to get started:");
  console.log();
  console.log(`cd ${path.basename(projectDir)}`);
  console.log(`bun install`);
  console.log(`bun build-android`);
}

export default function newServiceProject(templateDir) {
  console.log("Creating new Any service:");

  inquirer.prompt(QUESTIONS).then((answers) => {
    newAnyService(templateDir, answers);
  });
}
