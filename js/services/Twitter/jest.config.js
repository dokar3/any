/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
export default {
  testEnvironment: "node",
  transform: { "^.+\\.(t|j)sx?$": "ts-jest" },
  transformIgnorePatterns: ["node_modules/(?!any-service-api|any-service-testing|os-locale)"],
  maxWorkers: 1,
};
