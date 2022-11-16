/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  /**
   * Workaround for slow tests:
   * https://github.com/kulshekhar/ts-jest/issues/259#issuecomment-504088010
   */
  maxWorkers: 1,
};
