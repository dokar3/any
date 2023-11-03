import {
  Comment,
  DomElement,
  FetchCommentsParams,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  PagedResult,
  Post,
  SearchPostsParams,
} from "any-service-api";
import { BASE_URL } from "./shared";

export function fetch(params: FetchPostParams): FetchResult<Post> {
  const results = /item\?id=(\d+)/.exec(params.url);
  if (results === null) {
    return FetchResult.err({ error: `Unsupported url: ${params.url}` });
  }

  const id = results[1];
  const url = `https://hacker-news.firebaseio.com/v0/item/${id}.json`;
  const json = http.get(url).text;
  if (json === null) {
    return FetchResult.err({ error: "Cannot fetch post" });
  }

  const story = JSON.parse(json);
  const post = new Post({
    title: story.title,
    url: params.url,
    author: story.by,
    commentCount: story.descendants,
    commentsKey: id,
    content: [Post.ContentElement.html({ html: story.text })],
  });

  return FetchResult.ok({ data: post });
}

export function fetchFreshList(
  params: FetchFreshListParams
): PagedResult<Post[]> {
  const page = (params.pageKey as number) ?? 1;
  const url = BASE_URL + "news?p=" + page;
  return fetchNews(url, page);
}

export function search(params: SearchPostsParams): PagedResult<Post[]> {
  const page = (params.pageKey as number) ?? 0;
  const url =
    "http://hn.algolia.com/api/v1/search?query=" +
    params.query +
    "&tags=story&page=" +
    page;

  const json = http.get(url).text;
  if (json === null) {
    return PagedResult.err({ error: "Cannot fetch search result" });
  }

  const ret = JSON.parse(json);
  const items: any[] = ret.hits;
  const posts = items.map((item) => {
    const id = item.objectID;
    let url: string;
    let openInBrowser: boolean;
    if (item.url == null) {
      url = BASE_URL + "item?id=" + id;
      openInBrowser = false;
    } else {
      url = item.url;
      openInBrowser = true;
    }
    let content: Post.ContentElement[] = null;
    if (item.story_text != null) {
      content = [Post.ContentElement.html({ html: item.story_text })];
    }
    return new Post({
      title: item.title,
      url: url,
      author: item.author,
      commentCount: item.num_comments,
      commentsKey: id,
      openInBrowser: openInBrowser,
      content: content,
    });
  });

  return PagedResult.ok({
    data: posts,
    nextKey: page + 1,
  });
}

export function fetchComments(
  params: FetchCommentsParams
): PagedResult<Comment[]> {
  const url = "https://news.ycombinator.com/item?id=" + params.loadKey;
  const html = http.get(url).text;
  if (html === null) {
    return PagedResult.err({ error: "Cannot fetch comments" });
  }

  const doc = DOM.createDocument(html);
  if (doc === null) {
    return PagedResult.err({ error: "Cannot parse comments" });
  }

  const commentElements = doc.selectAll(".comment-tree > tbody > tr.athing");
  const comments = parseComments(commentElements);

  return PagedResult.ok({ data: comments, nextKey: null });
}

export function fetchNews(url: string, page: number): PagedResult<Post[]> {
  const html = http.get(url).text;
  if (html === null) {
    return PagedResult.err({ error: "Cannot fetch the news page" });
  }
  const doc = DOM.createDocument(html);
  if (doc === null) {
    return PagedResult.err({ error: "Cannot parse the news page" });
  }
  return parseNews(doc, page);
}

export function parseNews(doc: DomElement, page: number): PagedResult<Post[]> {
  const list = doc.select("#hnmain");
  const titles = list.selectAll(".athing");
  const infos = list.selectAll(".subtext");
  const posts = new Array<Post>();

  for (let i = 0; i < titles.length; i++) {
    const titleElement = titles[i];

    const id = titleElement.attr("id");

    const siteElement = titleElement.select(".sitebit");
    const site = siteElement?.select(".sitestr").text() ?? null;

    const titleTag = titleElement.select(".title .titleline a");
    const title = titleTag.text() + (site !== null ? " (" + site + ")" : "");
    let url = titleTag.attr("href");
    let isInSiteUrl = false;
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = BASE_URL + url;
      isInSiteUrl = true;
    }

    const info = infos[i];

    const authorId = info.select("a.hnuser")?.text() ?? null;

    const infoLinks = info.selectAll(".subline a");
    const commentCountStr =
      infoLinks.length > 0 ? infoLinks[infoLinks.length - 1].text() : "";
    const count = parseInt(commentCountStr);
    const commentCount = isNaN(count) ? 0 : count;

    const post = new Post({
      title: title,
      url: url,
      author: authorId,
      authorId: authorId,
      commentsKey: id,
      commentCount: commentCount,
      openInBrowser: !isInSiteUrl,
    });
    posts.push(post);
  }

  return PagedResult.ok({
    data: posts,
    nextKey: page + 1,
  });
}

function parseComments(elements: DomElement[]): Comment[] {
  const comments: Comment[] = [];
  const flattenComments: Comment[] = [];
  const indents: number[] = [];

  for (let i = 0; i < elements.length; i++) {
    const element = elements[i];

    const indent = parseInt(element.select("td.ind").attr("indent"));
    const username = element.select(".comhead .hnuser").text();
    const text = parseCommentContent(element.select(".comment .commtext"));
    if (text == null) {
      continue;
    }

    const comment = new Comment({
      username: username,
      content: text,
      replies: [],
    });

    if (indent > 0) {
      // Find the parent comment
      let parentIndex = indents.lastIndexOf(indent - 1);
      if (parentIndex === -1) {
        throw new Error(
          `Cannot find parent comment for child comment ${i}, indent: ${indent}`
        );
      }
      const parent = flattenComments[parentIndex];
      parent.replies.push(comment);
    } else {
      comments.push(comment);
    }

    indents.push(indent);
    flattenComments.push(comment);
  }

  return comments;
}

function parseCommentContent(commentElement: DomElement | null): string | null {
  if (commentElement === null) {
    return null;
  }
  const content = commentElement.text().trim();
  if (content.startsWith("> any")) {
    console.log("comment: ", content);
  }
  const replyElement = commentElement.select('> [class*="reply"]');
  if (replyElement === null) {
    return content;
  }
  const replyElementText = replyElement.text();
  if (content.endsWith(replyElementText)) {
    // Remove the last 'reply' in the comment content
    return content.substring(0, content.length - replyElementText.length);
  } else {
    return content;
  }
}
