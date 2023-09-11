import { DomElement, PagedResult, Post } from "any-service-api";
import { BASE_URL } from "./shared";

/**
 * Fetch and parse shots.
 *
 * @param {string} url The Dribbble webpage url.
 * @param {number} page The page number.
 * @returns {PagedResult<Post[]>} The paged result.
 */
export function fetchShots(url: string, page: number): PagedResult<Post[]> {
  const options = {
    headers: {
      "x-requested-with": "XMLHttpRequest",
    },
  };
  const html = http.get(url, options).text;
  if (html === null) {
    return PagedResult.err({ error: "Cannot fetch shots" });
  }
  return parseShots(html, page);
}

/**
 * Parse shots.
 *
 * @param {string} html The Dribbble webpage html.
 * @param {number} page The page number.
 * @returns {PagedResult<Post[]>} The paged result.
 */
export function parseShots(html: string, page: number): PagedResult<Post[]> {
  const doc = DOM.createDocument(html);
  if (doc === null) {
    return PagedResult.err({ error: "Cannot parse dribbble page" });
  }

  const items = doc.selectAll("li.shot-thumbnail");
  const shots = new Array<Post>();
  for (let i = 0; i < items.length; i++) {
    const item = items[i];

    const path = item.select("a.dribbble-link")?.attr("href");
    if (path === null) {
      continue;
    }
    const shotUrl = `${BASE_URL}${path}`;

    const coverImg = item.select("figure > img");
    const thumb = parseImageUrl(coverImg);

    const title = item.select(".shot-thumbnail-overlay .shot-title")?.text();

    const infoElement = item.select(".user-information");
    let avatar: string = null;
    let artist: string = null;
    let artistId: string = null;
    if (infoElement !== null) {
      avatar = infoElement.select(".photo").attr("data-src");
      artist = infoElement.select(".display-name").text();
      artistId = infoElement.select("a.url").attr("href");
    }

    const shot = new Post({
      title: title,
      url: shotUrl,
      media: [Post.Media.photo({ url: thumb })],
      author: artist,
      authorId: artistId,
      avatar: avatar,
    });

    shots.push(shot);
  }

  const scripts = doc.selectAll("script");
  for (let i = 0; i < scripts.length; i++) {
    const scriptContent = scripts[i].html();
    const varDefine = "var newestShots = ";
    const start = scriptContent.indexOf(varDefine);
    if (start !== -1) {
      let end = scriptContent.length;
      for (let j = start + varDefine.length; j < scriptContent.length; j++) {
        if (scriptContent[j] === ";") {
          end = j;
          break;
        }
      }
      const variable = scriptContent.substring(start + varDefine.length, end);

      const details = eval(variable);

      if (Array.isArray(details)) {
        for (let i = 0; i < details.length; i++) {
          const shot = shots[i];
          const detail = details[i];
          shot.commentCount = parseInt(detail.comments_count);
          shot.commentsKey = detail.id.toString();
          shot.url = `${BASE_URL}${detail.path}`;
        }
      }
      break;
    }
  }

  return PagedResult.ok({ data: shots, nextKey: page + 1 });
}

export function parseShotPage(content: DomElement): Post.ContentElement[] {
  const elements = new Array<Post.ContentElement>();

  const page = content.select(".shot-page-container");
  if (page !== null) {
    const blocks = page.selectAll(".content-block-container > *");

    for (let i = 0; i < blocks.length; i++) {
      const block = blocks[i];

      const carousel = block.select(".base-carousel");
      if (carousel != null) {
        elements.push(parseShotCarousel_block_media(block));
      } else {
        const video = block.select("video");
        if (video !== null) {
          const aspectRatio = resolveAspectRatioFromStyle(block);
          let videoUrl = video.attr("src");
          if (videoUrl.length == 0) {
            videoUrl = video.attr("data-src");
          }
          elements.push(
            Post.ContentElement.video({
              url: videoUrl,
              aspectRatio: aspectRatio,
            })
          );
          continue;
        }

        const image = block.select("img.v-img");
        if (image !== null) {
          elements.push(parseShotImage(block));
          continue;
        }

        elements.push(Post.ContentElement.html({ html: block.html() }));
      }
    }

    return elements;
  }

  const media = content.select(".shot-media-container");
  if (media !== null) {
    elements.push(parseShotMediaContainer(media));

    const description = content.select(".shot-description-container");
    if (description != null) {
      elements.push(Post.ContentElement.html({ html: description.html() }));
    }

    return elements;
  }

  return null;
}

/**
 * Parse element in the 'shot-media-container' element.
 *
 * @param {DomElement} media 'shot-media-container' element.
 * @returns {PostContentElement} Parsed element.
 */
function parseShotMediaContainer(media: DomElement): Post.ContentElement {
  const gallery = media.select(".media-gallery");
  if (gallery === null) {
    const img = media.select(".media-content > img");
    if (img !== null) {
      return parseShotImage(media);
    }

    const video = media.select(".media-content video");
    if (video != null) {
      let videoUrl = video.attr("src");
      if (videoUrl.length == 0) {
        videoUrl = video.attr("data-src");
      }
      return Post.ContentElement.video({ url: videoUrl });
    }

    return parseShotCarousel_block_media(media);
  } else {
    return parseShotCarousel_media_slide(gallery);
  }
}

/**
 * Parse carousel element.
 *
 * @param {DomElement} carousel The carousel container element.
 * @returns {PostContentElement} Parsed carousel element.
 */
function parseShotCarousel_media_slide(
  carousel: DomElement
): Post.ContentElement {
  return parseShotCarousel(carousel, ".media-slide");
}

/**
 * Parse carousel element.
 *
 * @param {DomElement} carousel The carousel container element.
 * @returns {PostContentElement} Parsed carousel element.
 */
function parseShotCarousel_block_media(
  carousel: DomElement
): Post.ContentElement {
  return parseShotCarousel(
    carousel,
    ".base-carousel .block-media-wrapper .block-media"
  );
}

/**
 * Parse carousel element.
 *
 * @param {DomElement} carousel The carousel container element.
 * @param {string} slideSelector The slide item css selector.
 * @returns {PostContentElement} Parsed carousel element.
 */
function parseShotCarousel(
  carousel: DomElement,
  slideSelector: string
): Post.ContentElement {
  const slides = carousel.selectAll(slideSelector);
  const items = [];
  for (let i = 0; i < slides.length; i++) {
    const slide = slides[i];

    const image = slide.select("img");
    if (image !== null) {
      const item = {
        image: JSON.parse(parseShotImage(slide).value).url,
      };
      items.push(item);
      continue;
    }

    const video = slide.select("video");
    if (video != null) {
      const item = {
        video: video.attr("data-src"),
      };
      items.push(item);
    }
  }
  return Post.ContentElement.carousel({ items: items });
}

/**
 * Parse image element.
 *
 * @param {DomElement} imageWrapper The image wrapper may contain an 'aspect-ratio' style.
 * @returns {PostContentElement} The image element.
 */
function parseShotImage(imageWrapper: DomElement): Post.ContentElement {
  const image = imageWrapper.select("img");

  let aspectRatio = parseFloat(image.attr("data-aspectratio"));
  if (isNaN(aspectRatio)) {
    aspectRatio = resolveAspectRatioFromStyle(imageWrapper);
  }
  if (!aspectRatio || isNaN(aspectRatio) || aspectRatio <= 0) {
    aspectRatio = 8.0 / 6;
  }

  const width = 1024;
  const height = parseInt((width / aspectRatio).toString());
  const resize = `${width}x${height}`;

  return Post.ContentElement.image({
    url: parseImageUrl(image).replace("{width}x{height}", resize),
    aspectRatio: aspectRatio,
  });
}

/**
 * Try resolve the aspect ratio from style attribute.
 *
 * @param {DomElement} element The target element.
 * @returns {number|null} The aspect ratio, null if failed to resolve.
 */
function resolveAspectRatioFromStyle(element: DomElement): number | null {
  const styleCss = element.attr("style");
  const aspectRatioRet = /aspect-ratio:([\d\.]+)/.exec(styleCss);
  if (aspectRatioRet !== null) {
    return parseFloat(aspectRatioRet[1]);
  } else {
    return null;
  }
}

/**
 * Get image url from the img element.
 *
 * @param {DomElement} img The cover element.
 * @returns {string} Image url.
 */
function parseImageUrl(img: DomElement): string {
  let srcSet = img.attr("data-srcset");
  if (srcSet.length === 0) {
    srcSet = img.attr("srcset");
  }
  if (srcSet.length === 0) {
    const dataSrc = img.attr("data-src");
    if (dataSrc.length !== 0) {
      return dataSrc;
    } else {
      return img.attr("src");
    }
  }

  const imgs = srcSet
    .split(", ")
    .map((urlAndWidth) => {
      const texts = urlAndWidth.split(" ");
      return {
        url: texts[0],
        width: parseInt(texts[1]),
      };
    })
    .sort((a, b) => a.width - b.width)
    .map((imgWithSize) => imgWithSize.url);

  return imgs[parseInt((imgs.length / 2).toString())];
}
