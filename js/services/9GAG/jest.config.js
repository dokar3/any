/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
export default {
  testEnvironment: "node",
  transform: { "^.+\\.(t|j)sx?$": "ts-jest" },
  transformIgnorePatterns: ["node_modules/(?!any-api|any-testing|os-locale)"],
  /**
   * Workaround for slow tests:
   * https://github.com/kulshekhar/ts-jest/issues/259#issuecomment-504088010
   */
  maxWorkers: 1,
};
