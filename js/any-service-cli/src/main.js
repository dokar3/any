#!/usr/bin/env node

import process from "process";
import path from "path";
import newServiceProject from "./new_service_project.js";
import { fileURLToPath } from "url";

const argv = process.argv;

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const templateDir = path.join(__dirname, "../../any-template");

if (argv.length >= 3) {
  switch (argv[2]) {
    case "new": {
      newServiceProject(templateDir);
      break;
    }
    case "-h":
    case "--help": {
      const message = `Options:\n
        new           Create new service project from the template.
        -h, --help    Print help messages. 
        `;
      console.log(message);
      break;
    }
    default: {
      throw new Error(`Unknown option: ${argv[2]}`);
    }
  }
} else {
  newServiceProject(templateDir);
}
