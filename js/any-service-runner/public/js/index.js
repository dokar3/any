import * as Comlink from "comlink";
import * as CallEvent from "./call_event.js";
import "./prism.js";

const worker = new Worker("app_worker.bundle.js");
const call = Comlink.wrap(worker);

let serviceManifests = [];
let serviceManifestNames = [];
let currentManifestIndex = 0;

let compiledServiceManifestUrls = [];

window.onload = () => {
  fetchServiceManifests();
};

window.fetchLatestPosts = () => {
  const page = inputPage.value;
  const configs = getServiceConfigs();
  const type = CallEvent.TYPE_FETCH_LATEST_POSTS;
  const params = {
    isPageable: serviceManifests[currentManifestIndex].isPageable,
    page: page,
  };
  call(configs, type, params)
    .then((posts) => {
      updateResult(posts);
    })
    .catch((error) => {
      updateError(error.stack);
    });
  disableActions();
};

window.fetchPostContent = () => {
  const url = inputUrl.value;
  const configs = getServiceConfigs();
  const type = CallEvent.TYPE_FETCH_POST_CONTENT;
  const params = { url: url };
  call(configs, type, params)
    .then((post) => {
      updateResult(post);
    })
    .catch((error) => {
      updateError(error.stack);
    });
  disableActions();
};

window.searchPosts = () => {
  const keywords = inputKeywords.value;
  const configs = getServiceConfigs();
  const type = CallEvent.TYPE_SEARCH_POSTS;
  const params = { keywords: keywords ?? "", page: 1 };
  disableActions();
  call(configs, type, params)
    .then((posts) => {
      updateResult(posts);
    })
    .catch((error) => {
      updateError(error.stack);
    });
};

window.selectCompiledService = function (index) {
  const tabs = document.querySelectorAll(
    "#compiled-app-tab-container .compiled-app-tab"
  );
  tabs.forEach((tab, idx) => {
    if (idx == index) {
      tab.classList.add("selected");
    } else {
      tab.classList.remove("selected");
    }
  });

  const url = compiledServiceManifestUrls[index];
  const command =
    "adb shell am start -n com.dokar.any/.MainActivity " +
    '-a any.action.add_app --es "extra.app_manifest_url" "' +
    url +
    '"';
  updateAddServiceCommand(command);
};

function updateCompiledServices(manifestUrls) {
  // Fetch all manifests
  const fetches = manifestUrls.map((url) =>
    fetch(url).then((res) => res.json())
  );
  Promise.all(fetches)
    .then((manifests) => {
      const tabContainer = document.getElementById(
        "compiled-app-tab-container"
      );
      tabContainer.innerHTML = "";
      manifests.forEach((manifest, index) => {
        const tab = document.createElement("p");
        tab.className = "compiled-app-tab";
        tab.onclick = () => {
          selectCompiledService(index);
        };
        tab.innerHTML = manifest.name;
        tabContainer.appendChild(tab);
      });
      selectCompiledService(0);
      showServiceCompileResult();
      hideCompiling();
    })
    .catch((error) => {
      updateError(`Cannot fetch manifest: ${error}`);
      hideCompiling();
    });
}

function updateAddServiceCommand(command) {
  const code = Prism.highlight(command, Prism.languages.shell, "shell");
  document.getElementById("add-app-command").innerHTML = code;

  const runButton = document.getElementById("run-add-app-command");
  runButton.onclick = () => {
    if (runButton.classList.contains(".disabled")) {
      return;
    }
    runButton.classList.add(".disabled");
    const init = {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      keepalive: true,
      body: JSON.stringify({
        command: command,
      }),
    };
    fetch("run_command", init)
      .then((res) => res.text())
      .then((text) => {
        runButton.classList.remove(".disabled");
        console.log(`Result: ${text}, command: ${command}`);
      })
      .catch((error) => {
        runButton.classList.remove(".disabled");
        console.error(`Cannot execute command: ${command}, error: ${error}`);
      });
  };

  const copyButton = document.getElementById("copy-add-app-command");
  copyButton.onclick = () => {
    navigator.clipboard.writeText(command);
  };
}

window.hideServiceCompileResult = () => {
  document.getElementById("app-compile-result-overlay").style.display = "none";
};

window.showServiceCompileResult = () => {
  document.getElementById("app-compile-result-overlay").style.display = "flex";
};

function fetchServiceManifests() {
  fetch("manifests")
    .then((res) => res.json())
    .then((results) => {
      serviceManifests = results.map((ret) => ret.manifest);
      serviceManifestNames = results.map((ret) => ret.name);
      setCurrentService(0);
    })
    .catch((error) => console.error(error));
}

function setCurrentService(index) {
  currentManifestIndex = index;

  const manifest = serviceManifests[index];

  document.querySelector("#app-info #name").innerHTML = manifest.name;

  const imgIcon = document.querySelector("#app-info #icon img");
  if (manifest.icon) {
    imgIcon.style.display = "block";
    imgIcon.src = manifest.icon;
  } else {
    imgIcon.src = "";
    imgIcon.style.display = "none";
  }

  document.querySelector("#app-info #id").innerHTML = manifest.id;

  document.querySelector("#app-info #version").innerHTML = manifest.version;

  document.querySelector("#app-info #developer").innerHTML = manifest.developer;

  document.querySelector("#app-info #description").innerHTML =
    manifest.description;

  updateServiceConfigs(manifest.configs);

  const manifestDropDown = document.getElementById("manifest-dropdown");
  manifestDropDown.innerHTML = "";
  serviceManifests.forEach((manifest, idx) => {
    const item = document.createElement("li");
    item.className = "item";
    if (idx === index) {
      item.classList.add("selected");
    }
    item.onclick = (e) => {
      e.stopPropagation();
      hideManifestDropDown();
      setCurrentService(idx);
    };
    item.innerHTML = manifest.name;
    manifestDropDown.appendChild(item);
  });
}

window.hideManifestDropDown = function (e) {
  (e || window.event).stopPropagation();
  document.getElementById("manifest-dropdown").style.display = "none";
};

window.showManifestDropDown = function (e) {
  (e || window.event).stopPropagation();
  document.getElementById("manifest-dropdown").style.display = "block";
};

window.compileService = function () {
  let isComplete = false;

  setTimeout(() => {
    if (!isComplete) {
      showCompiling();
    }
  }, 50);

  const body = {
    manifestName: serviceManifestNames[currentManifestIndex],
  };
  const init = {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    keepalive: true,
    body: JSON.stringify(body),
  };
  console.log("compile body " + JSON.stringify(body));
  fetch("compile_service", init)
    .then((res) => res.json())
    .then((result) => {
      isComplete = true;
      console.log(`Compile result: ${JSON.stringify(result)}`);
      if (result.isSuccess === true) {
        const manifestUrls = result.data.manifests;
        if (manifestUrls && manifestUrls.length > 0) {
          compiledServiceManifestUrls = manifestUrls;
          updateCompiledServices(manifestUrls);
          updateError("");
        } else {
          updateError("Service compiled but no manifest files found.");
        }
      } else {
        hideCompiling();
        if (result.message) {
          updateError(result.message);
        } else {
          updateError(result);
        }
      }
    })
    .catch((error) => {
      isComplete = true;
      hideCompiling();
      updateError(`Compile failed: ${error}`);
    });
};

function showCompiling() {
  document.getElementById("compile-overlay").style.display = "flex";
}

function hideCompiling() {
  document.getElementById("compile-overlay").style.display = "none";
}

function updateServiceConfigs(configs) {
  const fields = document.getElementById("app-configs-fields");
  const templateItem = document.getElementById("app-configs-template-item");
  fields.innerHTML = "";
  fields.appendChild(templateItem);
  if (!configs || !Array.isArray(configs)) {
    return;
  }
  configs.forEach((config) => {
    const item = templateItem.cloneNode(true);
    if (configs.length == 1) {
      item.style = "display: block; width: 100%";
    } else {
      item.style = "display: block";
    }
    item.id = config.key;

    item.querySelector(".config-name").innerHTML = config.name ?? "";

    const input = item.querySelector(".config-value");
    input.value = config.value ?? "";
    input.placeholder = config.description;

    fields.appendChild(item);
  });
}

function getServiceConfigs() {
  const configs = {};
  document
    .querySelectorAll("#app-configs-fields .config-item")
    .forEach((item, index) => {
      if (index == 0) {
        return;
      }
      const key = item.id;
      const value = item.querySelector(".config-value").value;
      configs[key] = value;
    });
  return configs;
}

function updateResult(result) {
  const message = JSON.stringify(result, null, 2);
  const output = document.getElementById("output");
  output.style.color = "black";
  const code = Prism.highlight(message, Prism.languages.json, "json");
  output.innerHTML = code;
  enableActions();
}

function updateError(error) {
  const output = document.getElementById("output");
  output.style.color = "red";
  output.textContent = error;
  enableActions();
}

function enableActions() {
  document
    .querySelectorAll(".app-action > button")
    .forEach((button) => (button.disabled = false));
}

function disableActions() {
  document
    .querySelectorAll(".app-action > button")
    .forEach((button) => (button.disabled = true));
}
