import { argv } from "process";

/**
 * Get command line arguments. Based on https://stackoverflow.com/a/54098693
 *
 * @returns {string[]} Arguments.
 */
function getCommandArgs() {
  const args = {};
  argv.slice(2).forEach((arg) => {
    // long arg
    if (arg.includes("=")) {
      const longArg = arg.split("=");
      const longArgFlag = removePrefixDash(longArg[0]);
      const longArgValue = longArg.length > 1 ? longArg[1] : true;
      args[longArgFlag] = longArgValue;
    }
    // flags
    else if (arg[0] === "-" || args.slice(0, 2) === "--") {
      const flags = removePrefixDash(arg);
      flags.forEach((flag) => {
        args[flag] = true;
      });
    }
  });
  return args;
}

function removePrefixDash(str) {
  let start = 0;
  while (str[start] == "-") {
    start++;
  }
  return str.substring(start);
}

export { getCommandArgs };
