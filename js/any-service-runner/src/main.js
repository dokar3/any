#!/usr/bin/env node
import process from "process";
import { startFrontendRunnerServer } from "./runner.frontend.js";
import { startBackendRunnerServer } from "./runner.backend.js";

function getPortFromArgs(args, argNames) {
  for (const arg of args) {
    for (const name of argNames) {
      if (arg.startsWith(name + "=")) {
        return parseInt(arg.substring(name.length + 1));
      }
    }
  }
  return null;
}

const args = process.argv;
if (args.length >= 3) {
  const option = args[2];
  switch (option) {
    case "-b":
    case "--backend": {
      let port = getPortFromArgs(args, ["--port", "-p"]);
      startBackendRunnerServer(port);
      break;
    }
    case "-f":
    case "--frontend": {
      let port = getPortFromArgs(args, ["--port", "-p"]);
      let backendServerPort = getPortFromArgs(args, ["--backend-port", "-bp"]);
      startFrontendRunnerServer(port, backendServerPort);
      break;
    }
    case "-h":
    case "--help": {
      const message = `Options:
        -b,  --backend        Run service runner backend server. Default port is 10101.
        -f,  --frontend       Run service runner frontend server. Default port is 10102.
        -p,  --port           Add it after --backend or --frontend to specify the server port.
        -bp, --backend-port   Add it after --frontend to specify the backend server port when starting the frontend server.
        -h,  --help           Print help messages.
      `;
      console.log(message);
      break;
    }
    default: {
      throw new Error(`Unknown option: ${option}`);
    }
  }
} else {
  throw new Error("Require an option, see available options with --help.");
}
