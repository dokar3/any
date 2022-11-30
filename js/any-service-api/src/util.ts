export class NotImplementedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "NotImplementedError";
  }
}

/**
 * The aspect ratio type, it accepts:
 * 1. Numbers
 * 2. String concatenated by two numbers using ':', '/', 'x' or 'X'
 */
export type AspectRatio =
  | `${number}:${number}`
  | `${number}/${number}`
  | `${number}x${number}`
  | `${number}X${number}`
  | number;
