import { expect, test } from "@jest/globals";
import { multibyteSubstring } from "../src/ParseTweet";

const tweets = [
  {
    id: "1591634458159026183",
    text: "âï¸\nGood morning dear friends \n\n#nature #photography https://t.co/00gc5VUiq7",
    annotations: [
      {
        start: 52,
        end: 75,
        text: "https://t.co/00gc5VUiq7",
      },
      {
        start: 31,
        end: 38,
        text: "#nature",
      },
      {
        start: 39,
        end: 51,
        text: "#photography",
      },
    ],
  },
  {
    id: "1591622180710014976",
    text: "ð¥²https://t.co/JXkG2tOHo7\nðªhttps://t.co/cgcSarBfby\nð¨\u200dð©\u200dð§\u200dð¦https://t.co/JWPnmBS7bF\nFighting with emojis and ð¤ð¡ððððð #characters",
    annotations: [
      {
        start: 1,
        end: 24,
        text: "https://t.co/JXkG2tOHo7",
      },
      {
        start: 26,
        end: 49,
        text: "https://t.co/cgcSarBfby",
      },
      {
        start: 57,
        end: 80,
        text: "https://t.co/JWPnmBS7bF",
      },
      {
        start: 114,
        end: 125,
        text: "#characters",
      },
    ],
  },
  {
    id: "1589799999311077377",
    text: "This edition of #NowInAndroid recaps #AndroidDevSummit, @kotlin multiplatform updates, Material Design releases, and more! ð ð¥°\n \nTune in:\nð â https://t.co/TEB3SBKcGe\nð¹ â https://t.co/Evmyhd0O9Y\nð§ â https://t.co/sIXVv91Phc https://t.co/CbpUopGNwY",
    annotations: [
      {
        start: 56,
        end: 63,
        text: "@kotlin",
      },
      {
        start: 16,
        end: 29,
        text: "#NowInAndroid",
      },
      {
        start: 37,
        end: 54,
        text: "#AndroidDevSummit",
      },
      {
        start: 142,
        end: 165,
        text: "https://t.co/TEB3SBKcGe",
      },
      {
        start: 170,
        end: 193,
        text: "https://t.co/Evmyhd0O9Y",
      },
      {
        start: 198,
        end: 221,
        text: "https://t.co/sIXVv91Phc",
      },
      {
        start: 222,
        end: 245,
        text: "https://t.co/CbpUopGNwY",
      },
    ],
  },
  {
    id: "1590468405605588992",
    text: "Fuel your Wear OS app with high-quality fitness data using the latest version of Health Services â ð\u200dâï¸ \n\nHealth Services Jetpack Beta captures more with new metrics, consolidates exercise and state, and improves passive monitoring! \n\nGet details here â https://t.co/YjPboL6RwK",
    annotations: [
      {
        start: 254,
        end: 277,
        text: "https://t.co/YjPboL6RwK",
      },
    ],
  },
  {
    id: "1590539754851950592",
    text: "âï¸\nÉ¢á´á´á´ á´á´ÊÉ´ÉªÉ´É¢ ð\n\n#nature #photography https://t.co/fdN1EeASoW",
    annotations: [
      {
        start: 19,
        end: 26,
        text: "#nature",
      },
      {
        start: 27,
        end: 39,
        text: "#photography",
      },
      {
        start: 40,
        end: 63,
        text: "https://t.co/fdN1EeASoW",
      },
    ],
  },
];

describe("test substring() with emojis", () => {
  for (let i = 0; i < tweets.length; i++) {
    const tweet = tweets[i];
    const text = tweet.text;
    test(`test tweet ${tweet.id} index ${i}`, () => {
      for (const annotation of tweet.annotations) {
        const start = annotation.start;
        const end = annotation.end;
        const sub = multibyteSubstring(tweet.text, start, end);
        expect(sub).toBe(annotation.text);
      }
    });
  }
});
