import {
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  PagedResult,
  Post,
} from "any-service-api";

const MAX_SUMMARY_LEN = 500;

export function fetch(params: FetchPostParams): FetchResult<Post> {
  throw "Not supported yet, please click open in browser";
}

export function fetchFreshList(
  params: FetchFreshListParams
): PagedResult<Post[]> {
  const url = service.configs.url;
  if (!url) {
    return PagedResult.err({
      error: "Please configure the rss url before fetching",
    });
  }

  const xml = http.get(url).text;
  if (xml === null) {
    return PagedResult.err({
      error: "Cannot fetch posts, no response: " + url,
    });
  }

  const doc = DOM.createDocument(xml, "xml");
  if (doc == null) {
    return PagedResult.err({ error: "Cannot parse rss" });
  }

  let author: string = null;

  const authorElement = doc.select("author");
  if (authorElement !== null) {
    const nameElement = authorElement.select("name");
    if (nameElement !== null) {
      // <author>
      //   <name>..</name>
      // </author>
      author = nameElement.text();
    } else {
      author = authorElement.text();
    }
  }

  let items = doc.selectAll("item");
  if (items.length == 0) {
    items = doc.selectAll("entry");
  }
  const posts = new Array<Post>();
  for (let i = 0; i < items.length; i++) {
    const item = items[i];

    const title = item.select("title")?.text() ?? "";

    let postUrl = item.select("link")?.text() ?? null;
    if (!postUrl) {
      postUrl = item.select("link")?.attr("href") ?? "";
    }

    if (author === null) {
      author = item.select("author")?.text() ?? null;
    }
    if (author === null) {
      // dc:creator
      author = item.select("dc|creator")?.text() ?? null;
    }

    const date =
      item.select("pubDate")?.text() ?? item.select("updated")?.text() ?? "";

    const content = item.select("content|encoded")?.text() ?? "";

    const description =
      item.select("description")?.text() ??
      item.select("summary")?.text() ??
      "";

    let summary = null;
    if (description.length > 0) {
      const descriptionText = DOM.createDocument(description)?.text();
      if (descriptionText) {
        summary = descriptionText.substring(
          0,
          Math.min(descriptionText.length, MAX_SUMMARY_LEN)
        );
      }
    }

    let thumb = item.select("featuredImage")?.text() ?? "";
    if (thumb.length === 0) {
      const enclosures = item.selectAll("enclosure");
      for (const enclosure of enclosures) {
        if (enclosure.attr("type").startsWith("image/")) {
          thumb = enclosure.attr("url");
          break;
        }
      }
    }

    let contentElements = new Array<Post.ContentElement>();

    const text = content.length > 0 ? content : description;
    if (text.length > 0) {
      let contentDoc = DOM.createDocument(text, "xml");
      if (contentDoc != null) {
        if (thumb.length == 0) {
          thumb = contentDoc.select("img")?.attr("src") ?? "";
        }
        contentElements.push(Post.ContentElement.html({ html: text }));
      } else {
        contentElements.push(Post.ContentElement.text({ text: text }));
      }
    }

    const avatar = item.select("media|thumbnail")?.attr("url") ?? null;

    const openInBrowser =
      service.configs.alwaysOpenInBrowser || contentElements.length == 0;

    let media: Array<Post.Media> = null;
    if (thumb !== "") {
      media = [Post.Media.photo({ url: thumb })];
    }

    const post = new Post({
      title: title,
      url: postUrl,
      media: media,
      author: author,
      avatar: avatar,
      summary: summary,
      date: date,
      content: contentElements,
      openInBrowser: openInBrowser,
    });

    posts.push(post);
  }

  return PagedResult.ok({ data: posts });
}
