#!/usr/bin/env node

const path = require("path");
const process = require("process");
const readline = require("readline");
const child_process = require("child_process");
const forEachProjects = require("./forEachProjects.js");

const HELP = `Run commands for built-in service projects.

Examples:
Install dependencies for all projects:
  node runInEachProject.js yarn

Upgrade typescript for all projects without ask any interactive question:
  node runInEachProject.js --no-interaction yarn up typescript

Options:
--help, -h    Print help messages`;

const args = process.argv.slice(2);
if (args.length == 0 || args[0] === "--help" || args[0] === "-h") {
  console.log(HELP);
  return;
}

function runCommandForAllProjects(cmd) {
  forEachProjects(
    (projectDir) => {
      console.log(`Run '${cmd}'for /${path.basename(projectDir)} ...`);

      process.chdir(projectDir);

      child_process.execSync(cmd, { stdio: "inherit" });

      console.log();
    },
    () => {
      console.log("No npm projects found.");
    }
  );
}

async function askAndRun(cmd) {
  // https://stackoverflow.com/a/68504470
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });
  const prompt = (query) =>
    new Promise((resolve) => rl.question(query, resolve));
  const servicesDirs = path.join(process.cwd(), "js/services/")
  const choice = await prompt(
    `Run '${cmd}' for every project in '${servicesDirs}' ? (y/n)\n`
  );
  if (choice === "y" || choice === "Y") {
    const start = Date.now();

    runCommandForAllProjects(cmd);

    const end = Date.now();

    console.log();
    console.log("All done in " + ((end - start) / 1000).toFixed(3) + "s");
  } else {
    console.log("Skipped");
  }
  rl.close();
}

if (args[0] === "--no-interaction") {
  if (args.length > 1) {
    const cmd = args.slice(1).join(" ");
    runCommandForAllProjects(cmd);
  } else {
    throw new Error("No command after '--no-interaction'");
  }
} else {
  const cmd = args.join(" ");
  askAndRun(cmd);
}
