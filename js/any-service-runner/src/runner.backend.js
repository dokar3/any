import process from "process";
import child_process from "child_process";
import os from "os";
import path from "path";
import fs from "fs";
import express from "express";
import cors from "cors";
import axios from "axios";
import bodyParser from "body-parser";
import { buildService } from "any-compile";

const cwd = process.cwd(0);
const buildDir = path.join(cwd, "dist/");

const app = express();

const COMPILED_SERVICE_PATH = "/public";

let PORT = 10101;

// Enable all cors requests
app.use(cors());

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Serve compiled service dir
app.use(COMPILED_SERVICE_PATH, express.static(buildDir, { maxAge: 0 }));

app.get("/", (req, res) => {
  res.send("Backend server is running");
});

app.get("/stop", (req, res) => {
  res.send("Closed");
  app.close();
});

app.get("/manifests", (req, res) => {
  fetchServiceManifests(req, res);
});

app.post("/request", (req, res) => {
  doRequest(req, res);
});

app.post("/compile_service", (req, res) => {
  compileService(req, res);
});

app.post("/run_command", (req, res) => {
  runCommand(req, res);
});

/**
 * Start the backend server.
 *
 * @param {number} port The server port, defaults to 10101
 */
function startBackendRunnerServer(port) {
  if (port instanceof Number && port > 0) {
    PORT = port;
  }
  app.listen(PORT, function () {
    console.log(`Listening service runner server...\nhttp://localhost:${PORT}`);
  });
}

function errorPromise(error) {
  return new Promise((resolve, reject) => reject(error));
}

function runCommand(req, res) {
  const command = req.body.command;
  if (command.startsWith("adb")) {
    child_process.execSync(command);
    res.send("Command executed");
  } else {
    res.send("Only 'adb' commands are allowed.");
  }
}

function fetchServiceManifests(req, res) {
  const manifests = fs
    .readdirSync(cwd)
    .filter((filename) => {
      return filename.startsWith("manifest.") && filename.endsWith(".json");
    })
    .map((filename) => {
      const manifestPath = path.join(cwd, filename);
      const text = fs.readFileSync(manifestPath, "utf-8");
      try {
        return {
          name: filename,
          manifest: JSON.parse(text),
        };
      } catch (e) {
        return null;
      }
    })
    .filter((manifest) => manifest != null);
  res.send(manifests);
}

function doRequest(req, res) {
  const request = req.body;
  console.log(`Requesting: ${JSON.stringify(request)}`);
  res.setHeader("Content-Type", "text/plain");
  if (request) {
    newRequest(request)
      .then((response) => {
        let text;
        if (response.data instanceof Object) {
          text = JSON.stringify(response.data);
        } else {
          text = response.data;
        }
        const obj = {
          text: text,
          status: response.status,
          headers: response.headers,
        };
        res.send(JSON.stringify(obj));
      })
      .catch((error) => {
        const msg = `Failed to request: ${error}`;
        console.error(msg);
        res.send(msg);
      });
  } else {
    res.send("Invalid request");
  }
}

function newRequest(request) {
  const url = request.url;
  if (!url) {
    return errorPromise("Request require a valid 'url' argument");
  }

  const method = request.method;
  if (!method) {
    return errorPromise("Request require a valid 'method' argument");
  }
  if (method != "GET" && method != "POST") {
    const msg = `Illegal request 'method': ${method}, only 'GET' and 'POST' are supported`;
    return errorPromise(msg);
  }

  const axiosConfigs = {};

  const axiosReq = {
    url: url,
    method: method,
    headers: request.headers,
    timeout: request.timeout,
  };

  if (method == "POST" && request.params) {
    axiosReq.data = request.params;
  }

  return axios.create(axiosConfigs).request(axiosReq);
}

function compileService(req, res) {
  console.log("Request body: " + JSON.stringify(req.body));
  const manifestName = req.body.manifestName;
  if (!manifestName) {
    const result = {
      isSuccess: false,
      message: "Compile failed: No manifestName provided.",
    };
    console.error(result.message);
    res.send(result);
    return;
  }

  const address = getNetAddress();
  if (address != null) {
    const platform = "android";

    const networkPath = `http://${address}:${PORT}${COMPILED_SERVICE_PATH}/uncompressed`;

    buildService(buildDir, platform, false, [manifestName])
      .then(() => {
        const dir = path.join(buildDir, "uncompressed");
        const networkManifests = findAndUpdateManifests(dir, networkPath);
        const result = {
          isSuccess: true,
          message: "Service compiled",
          data: {
            manifests: networkManifests,
          },
        };
        console.log("Service compiled");
        res.send(result);
      })
      .catch((error) => {
        console.error(error.stack);
        const result = {
          isSuccess: false,
          message: `Compile failed: ${error}`,
        };
        res.send(result);
      });
  } else {
    const result = {
      isSuccess: false,
      message: "Server error: cannot get net address",
    };
    res.send(result);
  }
}

/**
 * Replace local paths with network paths in manifest files.
 *
 * @param dir Manifest directory.
 * @param networkPath Network path.
 * @returns {string[]} Network manifest paths.
 */
function findAndUpdateManifests(dir, networkPath) {
  return fs
    .readdirSync(dir)
    .filter((filename) => {
      return (
        filename.startsWith("manifest") &&
        filename.endsWith(".json") &&
        fs.lstatSync(path.join(dir, filename)).isFile()
      );
    })
    .map((filename) => {
      const filepath = path.join(dir, filename);
      const manifestText = fs.readFileSync(filepath, "utf-8");
      const manifest = JSON.parse(manifestText);

      manifest.main = `${networkPath}/${path.basename(manifest.main)}`;

      if (manifest.icon) {
        manifest.icon = `${networkPath}/${path.basename(manifest.icon)}`;
      }

      fs.writeFileSync(filepath, JSON.stringify(manifest));

      return `${networkPath}/${filename}`;
    });
}

// Copied from https://stackoverflow.com/a/8440736
function getNetAddress() {
  const nets = os.networkInterfaces();
  const results = Object.create(null); // Or just '{}', an empty object

  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      // Skip over non-IPv4 and internal (i.e. 127.0.0.1) addresses
      if (net.family === "IPv4" && !net.internal) {
        if (!results[name]) {
          results[name] = [];
        }
        results[name].push(net.address);
      }
    }
  }

  if (results == null) {
    return null;
  }

  if (!Array.isArray(results.WLAN)) {
    return null;
  }

  if (results.WLAN.length == 0) {
    return null;
  }

  return results.WLAN[0];
}

export { startBackendRunnerServer };
