import {
  AspectRatio,
  DomElement,
  FetchFreshListParams,
  PagedResult,
  Post,
  PostFeature,
  SearchPostsParams,
} from "any-service-api";

export class FilmGrabPostFeature extends PostFeature {
  fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
    const page = (params.pageKey as number) ?? 1;
    const url = `https://film-grab.com/page/${page}`;

    const html = http.get(url).text;
    if (html === null) {
      return PagedResult.err({ error: "Cannot fetch web page" });
    }

    const doc = DOM.createDocument(html);
    if (doc === null) {
      return PagedResult.err({ error: "Cannot parse web page" });
    }

    const items = doc.selectAll("#primary #main article.post");
    const posts = [];
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      const titleElement = item.select(
        ".entry-details .entry-header .entry-title a"
      );
      if (titleElement === null) {
        continue;
      }
      const postUrl = titleElement.attr("href");
      const title = titleElement.text();

      const summary =
        item.select(".entry-details .entry-content > p")?.text() ?? null;

      const thumbElement = item.select(".entry-thumb .popup-image > img");
      const thumb = this.getMediumSizeThumb(thumbElement);

      let media: Post.Media[] = null;
      if (thumb.url) {
        media = [
          Post.Media.photo({ url: thumb.url, aspectRatio: thumb.ratio }),
        ];
      }

      const post = new Post({
        url: postUrl,
        title: title,
        media: media,
        summary: summary,
        openInBrowser: true,
      });

      posts.push(post);
    }

    return PagedResult.ok({ data: posts, nextKey: page + 1 });
  }

  search(params: SearchPostsParams): PagedResult<Post[]> {
    const page = (params.pageKey as number) ?? 1;
    const url = `https://film-grab.com/page/${page}/?s=${params.query}`;

    const html = http.get(url).text;
    if (html === null) {
      return PagedResult.err({ error: "Cannot fetch web page" });
    }

    const doc = DOM.createDocument(html);
    if (doc === null) {
      return PagedResult.err({ error: "Cannot parse web page" });
    }

    const items = doc.selectAll("#primary #main article.post");
    const posts = [];
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      const titleElement = item.select(".entry-header .entry-title a");
      if (titleElement === null) {
        continue;
      }
      const postUrl = titleElement.attr("href");
      const title = titleElement.text();

      const summary = item.select(".entry-summary > p")?.text() ?? null;

      const thumbElement = item.select(
        ".entry-thumb .entry-thumb-content > img"
      );
      const thumb = this.getMediumSizeThumb(thumbElement);

      let media: Array<Post.Media> = null;
      if (thumb.url) {
        media = [
          Post.Media.photo({ url: thumb.url, aspectRatio: thumb.ratio }),
        ];
      }

      const post = new Post({
        url: postUrl,
        title: title,
        media: media,
        summary: summary,
        openInBrowser: true,
      });

      posts.push(post);
    }

    return PagedResult.ok({ data: posts, nextKey: page + 1 });
  }

  private getMediumSizeThumb(coverElement: DomElement) {
    const cover: { url?: string | null; ratio?: AspectRatio | null } = {};

    if (coverElement === null) {
      return cover;
    }

    const srcset = coverElement.attr("srcset");
    if (srcset.length > 0) {
      const images = srcset
        .split(", ")
        .map((urlWithWidth) => {
          const texts = urlWithWidth.split(" ");
          return {
            url: texts[0],
            width: parseInt(texts[1]),
          };
        })
        .sort((a, b) => a.width - b.width);
      const mid = Math.floor(images.length / 2);
      cover.url = images[mid].url;
    } else {
      cover.url = coverElement.attr("src");
    }

    const width = parseInt(coverElement.attr("width"));
    const height = parseInt(coverElement.attr("height"));
    cover.ratio = `${width}:${height}`;

    return cover;
  }
}
