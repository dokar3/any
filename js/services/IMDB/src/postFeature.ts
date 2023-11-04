import {
  DomElement,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
} from "any-service-api";

const BASE_URL = "https://www.imdb.com";

const HTTP_USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

export function fetch(params: FetchPostParams): FetchResult<Post> {
  throw new NotImplementedError("Not implemented yet.");
}

export function fetchFreshList(
  params: FetchFreshListParams
): PagedResult<Post[]> {
  const html = http.get(`${BASE_URL}/chart/top/`, {
    headers: { "User-Agent": HTTP_USER_AGENT },
  }).text;
  if (html == null) {
    return FetchResult.err({ error: "Cannot fetch web page" });
  }
  const doc = DOM.createDocument(html);
  return FetchResult.ok({ data: parseMovies(doc) });
}

function parseMovies(doc: DomElement): Post[] {
  const main = doc.select("main");
  if (main == null) {
    return [];
  }
  const elements = main.selectAll("li.ipc-metadata-list-summary-item");
  const posts: Post[] = [];
  for (const element of elements) {
    const title = element.select("div.ipc-title")?.text();
    if (title == null) {
      continue;
    }
    const url = element.select("a.ipc-title-link-wrapper")?.attr("href");
    if (url == null) {
      continue;
    }
    const poster = parsePoster(element.select("img.ipc-image")?.attr("srcset"));
    const summary = parseSummary(element.select("div.cli-title-metadata"));
    const rating = element.select("span.ipc-rating-star")?.text();
    const post = new Post({
      title: title + " - " + rating,
      url: BASE_URL + url,
      media: [Post.Media.photo({ url: poster })],
      summary: summary,
      rating: rating,
      openInBrowser: true,
    });
    posts.push(post);
  }

  return posts;
}

/**
 * Parse poster url from srcset attr.
 * from:
 * srcset="https://a.jpg 140w, https://b.jpg 210w, https://c.jpg 280w"
 * to:
 * https://c.jpg
 */
function parsePoster(srcset: string | null): string | null {
  if (srcset == null) {
    return null;
  }
  const arr = srcset.split(" ");
  if (arr.length < 2) {
    return null;
  }
  return arr[arr.length - 2];
}

function parseSummary(summaryElement: DomElement | null): string | null {
  if (summaryElement == null) {
    return null;
  }
  const children = summaryElement.selectAll("span");
  if (children.length == 0) {
    return null;
  }
  const separator = "  ";
  let summary = "";
  for (const child of children) {
    summary += child.text();
    summary += separator;
  }
  return summary.substring(0, summary.length - separator.length);
}
