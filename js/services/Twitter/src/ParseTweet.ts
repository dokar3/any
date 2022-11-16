import { AspectRatio, Post } from "any-service-api";

/**
 * Parse tweet to html string by the tweet.entities.
 *
 * @returns the html string.
 */
export function parseTweet({
  serviceId,
  id,
  text,
  authorId,
  referencedTweets,
  mediaKeys,
  entities,
  includes,
}: {
  serviceId: string;
  id: string;
  text: string;
  authorId: string;
  referencedTweets: any[];
  mediaKeys: any[];
  entities: any;
  includes: any;
}): Tweet {
  function findTweet(id: string): any | null {
    const tweets = includes.tweets;
    if (tweets === undefined) {
      throw "No 'includes.tweets' field";
    }
    const target = tweets.find((t: any) => t.id == id);
    if (target === undefined) {
      return null;
    }
    return target;
  }

  function getRefPost(id: string): Post | null {
    const ref = findTweet(id);
    if (ref === null) {
      return null;
    }
    const tweet = parseTweet({
      serviceId: serviceId,
      id: id,
      text: ref.text,
      authorId: ref.author_id,
      referencedTweets: null,
      mediaKeys: ref.attachments?.media_keys ?? [],
      entities: ref.entities,
      includes: includes,
    });
    const refPost = new Post({
      title: "",
      url: tweet.url,
      author: tweet.author,
      authorId: tweet.authorId,
      media: tweet.media,
      avatar: tweet.avatar,
      summary: tweet.text,
      openInBrowser: true,
    });
    return refPost;
  }

  let reference: Post.Reference = null;
  if (referencedTweets !== undefined && referencedTweets !== null) {
    for (const ref of referencedTweets) {
      const post = getRefPost(ref.id);
      if (post === null) {
        continue;
      }
      switch (ref.type) {
        case "retweeted": {
          reference = Post.Reference.repost({ post: post });
          break;
        }
        case "quoted": {
          reference = Post.Reference.quote({ post: post });
          break;
        }
        case "replied_to": {
          reference = Post.Reference.reply({ post: post });
          break;
        }
        default: {
        }
      }
      if (reference !== null) {
        break;
      }
    }
  }

  const user = includes.users.find((u: any) => u.id === authorId);

  const url = `https://twitter.com/${user.username}/status/${id}`;

  let tweetText: string = null;
  let tweetMedia: Post.Media[] = null;
  if (reference === null || reference.type !== Post.Reference.Type.Repost) {
    tweetText = rebuildTweetText(serviceId, text, entities);
    tweetMedia = parseTweetMedia(mediaKeys, includes.media);
  }

  const tweet = new Tweet({
    id: id,
    url: url,
    text: tweetText,
    author: user.name,
    authorId: authorId,
    // Use the 'bigger' image url
    avatar: user.profile_image_url?.replace("_normal.", "_bigger."),
    media: tweetMedia,
    reference: reference,
  });

  return tweet;
}

function rebuildTweetText(
  serviceId: string,
  text: string,
  entities: any
): string {
  if (entities === undefined) {
    return text;
  }

  const tagsToInsert: any = {};

  text = trimTrailingUrl(text, entities);

  // Urls
  const urlAnnotations = entities.urls ?? [];
  const urlReplacements: any = {};
  for (const annotation of urlAnnotations) {
    urlReplacements[annotation.url] = annotation.display_url;

    const start = annotation.start + "";
    const end = annotation.end + "";

    if (annotation.start > text.length || annotation.end > text.length) {
      continue;
    }

    let startTags = tagsToInsert[start] ?? "";
    startTags += `<a href="${annotation.expanded_url}">`;
    tagsToInsert[start] = startTags;

    let endTags = tagsToInsert[end] ?? "";
    endTags = "</a>" + endTags;
    tagsToInsert[end] = endTags;
  }

  // Tags
  const tagAnnotations = entities.hashtags ?? [];
  for (const annotation of tagAnnotations) {
    const start = annotation.start + "";
    const end = annotation.end + "";

    let startTags = tagsToInsert[start] ?? "";
    startTags += `<a href="anyapp://search?serviceId=${serviceId}&query=%23${annotation.tag}">`;
    tagsToInsert[start] = startTags;

    let endTags = tagsToInsert[end] ?? "";
    endTags = "</a>" + endTags;
    tagsToInsert[end] = endTags;
  }

  // Users
  const userAnnotations = entities.mentions ?? [];
  for (const annotation of userAnnotations) {
    const start = annotation.start + "";
    const end = annotation.end + "";

    let startTags = tagsToInsert[start] ?? "";
    startTags += `<a href="anyapp://user?serviceId=${serviceId}&id=${annotation.id}">`;
    tagsToInsert[start] = startTags;

    let endTags = tagsToInsert[end] ?? "";
    endTags = "</a>" + endTags;
    tagsToInsert[end] = endTags;
  }

  // Rebuild text
  let offset = 0;
  let rebuildText = text;
  const indices = Object.keys(tagsToInsert)
    .sort((a, b) => parseInt(a) - parseInt(b))
    .map((idx) => parseInt(idx));
  for (const index of indices) {
    const tags = tagsToInsert[index];
    const insertIdx = index + offset;
    rebuildText =
      multibyteSubstring(rebuildText, 0, insertIdx) +
      tags +
      multibyteSubstring(rebuildText, insertIdx, rebuildText.length);
    offset += tags.length;
  }

  // Replace 'url' with 'display_url'
  const urlsToReplace = Object.keys(urlReplacements);
  for (const urlToReplace of urlsToReplace) {
    const find = `>${urlToReplace}</a>`;
    const replace = `>${urlReplacements[urlToReplace]}</a>`;
    rebuildText = replaceAll(rebuildText, find, replace);
  }

  return rebuildText.replace(/\n/g, "<br>");
}

// Copied from https://stackoverflow.com/a/1144788
function escapeRegExp(string: string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"); // $& means the whole matched string
}

// Copied from https://stackoverflow.com/a/1144788
function replaceAll(str: string, find: string, replace: string): string {
  return str.replace(new RegExp(escapeRegExp(find), "g"), replace);
}

function trimTrailingUrl(text: string, entities: any): string {
  if (entities === undefined) {
    return text;
  }

  const urls = entities.urls;
  if (urls === undefined) {
    return text;
  }

  const urlMatch = /https:\/\/t\.co\/[\w\d]+$/.exec(text);
  if (urlMatch === null) {
    return text;
  }

  const url = urlMatch[0];
  const targetItem = urls.find((item: any) => item.url === url);
  if (!targetItem) {
    return text;
  }

  const expandedUrl = targetItem.expanded_url;
  if (!expandedUrl) {
    return text;
  }

  if (expandedUrl.startsWith("https://twitter.com/")) {
    return text.substring(0, text.length - url.length);
  }

  return text;
}

/**
 * Twitter's string annotation indices may be generated by Ruby (They switched from Ruby on Rails
 * at some point, but the returned indices from API still still follow Ruby's string split),
 * JavaScript's substring() won't work with these indices.
 *
 * This function has similar behavior to Ruby's string slice or PHP's mb_substr.
 */
export function multibyteSubstring(
  str: string,
  start: number,
  end: number
): string {
  const s = codePointIndexToStringIndex(str, start);
  const e = codePointIndexToStringIndex(str, end);
  return str.substring(s, e);
}

// Modified from: https://coolaj86.com/articles/how-to-count-unicode-characters-in-javascript/
function codePointIndexToStringIndex(str: string, index: number): number {
  if (index == 0 || index == str.length) {
    return index;
  }
  var point: number;
  var idx: number;
  var width = 0;
  var len = 0;
  for (idx = 0; idx < str.length; ) {
    point = str.codePointAt(idx);
    width = 0;
    while (point) {
      width += 1;
      point = point >> 8;
    }
    len += 1;
    if (len == index + 1) {
      break;
    }
    idx += Math.round(width / 2);
  }
  return idx;
}

export function parseTweetMedia(
  mediaKeys: any[],
  includesMedia: any
): Post.Media[] {
  if (includesMedia === undefined) {
    return [];
  }
  const media = new Array<Post.Media>();
  for (const key of mediaKeys) {
    const item = includesMedia.find((m: any) => m.media_key === key);
    if (item === undefined) {
      continue;
    }
    const url = item.url ?? "'url' is not available for V2 API yet";
    const width = parseInt(item.width);
    const height = parseInt(item.height);
    const aspectRatio: AspectRatio = `${width}:${height}`;
    if (item.type === "photo") {
      media.push(Post.Media.photo({ url: url, aspectRatio: aspectRatio }));
    } else if (item.type === "animated_gif") {
      const thumb = item.preview_image_url;
      media.push(Post.Media.gif({ url: thumb, aspectRatio: aspectRatio }));
    } else if (item.type === "video") {
      const thumb = item.preview_image_url;
      media.push(
        Post.Media.video({
          url: url,
          aspectRatio: aspectRatio,
          thumbnail: thumb,
        })
      );
    }
  }
  return media;
}

class Tweet {
  id: string;

  url: string;

  text: string;

  author: string;

  authorId: string;

  avatar: string;

  media: Post.Media[];

  reference: Post.Reference | null;

  constructor(other: Tweet) {
    Object.assign(this, other);
  }
}
