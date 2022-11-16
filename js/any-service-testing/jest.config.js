/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  transform: { "^.+\\.(t|j)sx?$": "ts-jest" },
  transformIgnorePatterns: ["node_modules/(?!any-api|os-locale)"]
};
