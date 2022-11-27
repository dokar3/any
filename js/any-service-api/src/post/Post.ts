import { AspectRatio } from "../util";

/**
 * Properties of {@link Post}.
 *
 * @since 0.1.0
 */
export type PostProps = {
  /**
   * Title of post.
   *
   * @since 0.1.0
   */
  title: string;

  /**
   * Url of post.
   *
   * @since 0.1.0
   */
  url: string;

  /**
   * Media objects, will be displayed as thumbnail(s) for this post.
   *
   * @since 0.1.0
   */
  media?: Post.Media[] | null;

  /**
   * Post type. Defaults to 'PostType.Article'.
   *
   * @since 0.1.0
   */
  type?: Post.Type | null;

  /**
   * Rating of post.
   *
   * @since 0.1.0
   */
  rating?: string | null;

  /**
   * Release or update date.
   *
   * @since 0.1.0
   */
  date?: string | null;

  /**
   * Summary text.
   *
   * @since 0.1.0
   */
  summary?: string | null;

  /**
   * The author name.
   *
   * @since 0.1.0
   */
  author?: string | null;

  /**
   * User id of the author.
   *
   * @since 0.1.0
   */
  authorId?: string | null;

  /**
   * The avatar url of the author.
   *
   * @since 0.1.0
   */
  avatar?: string | null;

  /**
   * Post category.
   *
   * @since 0.1.0
   */
  category?: string | null;

  /**
   * Tag list.
   *
   * @since 0.1.0
   */
  tags?: string[] | null;

  /**
   * Post content elements.
   *
   * @since 0.1.0
   */
  content?: Post.ContentElement[] | null;

  /**
   * Integer comment count.
   *
   * @since 0.1.0
   */
  commentCount?: number | null;

  /**
   * The key used to fetch comments by the PostFeature.
   *
   * @since 0.1.0
   */
  commentsKey?: string | null;

  /**
   * Should always open the post in the browser. Defaults to
   * false.
   *
   * @since 0.1.0
   */
  openInBrowser?: boolean;

  /**
   * The reference post, usually the original post of a quoted or replied post.
   *
   * @since 0.1.0
   */
  reference?: Post.Reference | null;
}

/**
 * The post entity.
 *
 * @since 0.1.0
 */
export class Post implements PostProps {
  title: string;
  url: string;
  media?: Post.Media[];
  type?: Post.Type;
  rating?: string;
  date?: string;
  summary?: string;
  author?: string;
  authorId?: string;
  avatar?: string;
  category?: string;
  tags?: string[];
  content?: Post.ContentElement[];
  commentCount?: number;
  commentsKey?: string;
  openInBrowser?: boolean;
  reference?: Post.Reference;

  constructor({
    title,
    url,
    media,
    type,
    rating,
    date,
    summary,
    author,
    authorId,
    avatar,
    category,
    tags,
    content,
    commentCount,
    commentsKey,
    openInBrowser,
    reference,
  }: PostProps) {
    this.title = title;
    this.url = url;
    this.media = media;
    this.type = type;
    this.rating = rating;
    this.date = date;
    this.summary = summary;
    this.author = author;
    this.authorId = authorId;
    this.avatar = avatar;
    this.category = category;
    this.tags = tags;
    this.content = content;
    this.commentCount = commentCount;
    this.commentsKey = commentsKey;
    this.openInBrowser = openInBrowser;
    this.reference = reference;
  }
}

export namespace Post {
  /**
   * The post type. Different types will affect the post screen UI.
   *
   * @since 0.1.0
   */
  export enum Type {
    Article = "article",
    Comic = "comic",
  }

  /**
   * Properties of {@link Media}.
   *
   * @since 0.1.0
   */
  export interface MediaProps {
    /**
     * Media type, must be one of ['photo', 'gif', 'video'].
     *
     * @since 0.1.0
     */
    type: Media.Type;

    /**
     * Media url.
     *
     * @since 0.1.0
     */
    url: string;

    /**
     * Media aspect ratio. Both numbers and strings (with the specified pattern) are acceptable.
     *
     * @see {@link AspectRatio}
     * @since 0.1.0
     */
    aspectRatio?: string;

    /**
     * Video thumbnail url, for 'video' type only.
     *
     * @since 0.1.0
     */
    thumbnail?: string | null;
  }

  /**
   * The post media object, could be a photo, gif or video.
   *
   * @since 0.1.0
   */
  export class Media implements MediaProps {
    type: Media.Type;
    url: string;
    aspectRatio?: string;
    thumbnail?: string;

    constructor({ type, url, aspectRatio, thumbnail }: MediaProps) {
      this.type = type;
      this.url = url;
      this.aspectRatio = aspectRatio;
      this.thumbnail = thumbnail;
    }

    /**
     * Create a photo media.
     *
     * @since 0.1.0
     */
    static photo({
      url,
      aspectRatio = null,
    }: {
      url: string;
      aspectRatio?: AspectRatio;
    }): Media {
      return new Media({
        type: Media.Type.Photo,
        url: url,
        aspectRatio: aspectRatio?.toString() ?? null,
      });
    }

    /**
     * Create a gif media.
     *
     * @since 0.1.0
     */
    static gif({
      url,
      aspectRatio = null,
    }: {
      url: string;
      aspectRatio?: AspectRatio;
    }): Media {
      return new Media({
        type: Media.Type.Gif,
        url: url,
        aspectRatio: aspectRatio?.toString() ?? null,
      });
    }

    /**
     * Create a video media.
     *
     * @since 0.1.0
     */
    static video({
      url,
      aspectRatio = null,
      thumbnail = null,
    }: {
      url: string;
      aspectRatio?: AspectRatio;
      thumbnail?: string | null;
    }): Media {
      return new Media({
        type: Media.Type.Video,
        url: url,
        aspectRatio: aspectRatio?.toString() ?? null,
        thumbnail: thumbnail,
      });
    }
  }

  export namespace Media {
    /**
     * The post media type.
     *
     * @since 0.1.0
     */
    export enum Type {
      Photo = "photo",
      Gif = "gif",
      Video = "video",
    }
  }

  /**
   * @since 0.1.0
   */
  export type TextEleProps = {
    /**
     * The text content.
     *
     * @since 0.1.0
     */
    text: string;
  };

  /**
   * @since 0.1.0
   */
  export type HtmlEleProps = {
    /**
     * The html content.
     *
     * @since 0.1.0
     */
    html: string;
  };

  /**
   * @since 0.1.0
   */
  export type ImageEleProps = {
    /**
     * The image url.
     *
     * @since 0.1.0
     */
    url: string;

    /**
     * Optional aspect ratio for the image. Both numbers and strings (with the specified pattern)
     * are acceptable.
     *
     * @see {@link AspectRatio}
     * @since 0.1.0
     */
    aspectRatio?: AspectRatio;
  };

  /**
   * @since 0.1.0
   */
  export type VideoEleProps = {
    /**
     * The image url.
     *
     * @since 0.1.0
     */
    url: string;

    /**
     * Optional video thumbnail.
     *
     * @since 0.1.0
     */
    thumbnail?: string;

    /**
     * Aspect ratio for the video. Both numbers and strings (with the specified pattern)
     * are acceptable.
     *
     * @see {@link AspectRatio}
     * @since 0.1.0
     */
    aspectRatio?: AspectRatio;
  };

  /**
   * Carousel item. When the 'video' property is defined, it presents a video item, otherwise, it
   * is an image item.
   *
   * @since 0.1.0
   */
  export type CarouselItem = {
    /**
     * The image url, if the 'video' property is defined, it presents the video thumbnail.
     *
     * @since 0.1.0
     */
    image?: string;

    /**
     * The video url.
     *
     * @since 0.1.0
     */
    video?: string;
  };

  /**
   * @since 0.1.0
   */
  export type CarouselEleProps = {
    /**
     * The carousel items.
     *
     * @since 0.1.0
     */
    items: CarouselItem[];

    /**
     * Aspect ratio for the carousel. Both numbers and strings (with the specified pattern)
     * are acceptable.
     *
     * @see {@link AspectRatio}
     * @since 0.1.0
     */
    aspectRatio?: AspectRatio;
  };

  /**
   * @since 0.1.0
   */
  export type SectionEleProps = {
    /**
     * The section title.
     *
     * @since 0.1.0
     */
    title: string;
  };

  /**
   * Post content element.
   *
   * @since 0.1.0
   */
  export class ContentElement {
    /**
     * The content type.
     *
     * @since 0.1.0
     */
    type: ContentElement.Type;

    /**
     * The element content.
     *
     * @since 0.1.0
     */
    value: string;

    constructor({ type, value }: { type: ContentElement.Type; value: string }) {
      this.type = type;
      this.value = value;
    }

    /**
     * Create a text element.
     *
     * @returns {ContentElement} The text element.
     * @since 0.1.0
     */
    static text({ text }: TextEleProps): ContentElement {
      return new ContentElement({
        type: ContentElement.Type.Text,
        value: text,
      });
    }

    /**
     * Create a html element.
     *
     * @returns {ContentElement} The html element.
     * @since 0.1.0
     */
    static html({ html }: HtmlEleProps): ContentElement {
      return new ContentElement({
        type: ContentElement.Type.Html,
        value: html,
      });
    }

    /**
     * Create an image element.
     *
     * @returns {ContentElement} The image element.
     * @since 0.1.0
     */
    static image({
      url,
      aspectRatio = undefined,
    }: ImageEleProps): ContentElement {
      const value = JSON.stringify({
        url: url,
        aspectRatio: aspectRatio?.toString() ?? null,
      });
      return new ContentElement({
        type: ContentElement.Type.Image,
        value: value,
      });
    }

    /**
     * Create a full-width image element.
     *
     * @returns {ContentElement} The full-width image element.
     * @since 0.1.0
     */
    static fullWidthImage({
      url,
      aspectRatio = undefined,
    }: ImageEleProps): ContentElement {
      const value = JSON.stringify({
        url: url,
        aspectRatio: aspectRatio?.toString() ?? null,
      });
      return new ContentElement({
        type: ContentElement.Type.FullWidthImage,
        value: value,
      });
    }

    /**
     * Create a video element.
     *
     * @param {object} videoProps Properties of the video element.
     * @returns {ContentElement} The video element.
     * @since 0.1.0
     */
    static video({
      url,
      thumbnail = undefined,
      aspectRatio = undefined,
    }: VideoEleProps): ContentElement {
      const value = JSON.stringify({
        url: url,
        thumbnail: thumbnail ?? null,
        aspectRatio: aspectRatio?.toString() ?? null,
      });
      return new ContentElement({
        type: ContentElement.Type.Video,
        value: value,
      });
    }

    /**
     * Create a carousel element.
     *
     * @returns {ContentElement} The carousel element.
     * @since 0.1.0
     */
    static carousel({
      items,
      aspectRatio = undefined,
    }: CarouselEleProps): ContentElement {
      const value = JSON.stringify({
        items: items,
        aspectRatio: aspectRatio?.toString() ?? null,
      });
      return new ContentElement({
        type: ContentElement.Type.Carousel,
        value: value,
      });
    }

    /**
     * Create a section element for marking and navigating usage.
     *
     * @returns {ContentElement} The section element.
     * @since 0.1.0
     */
    static section({ title }: SectionEleProps): ContentElement {
      return new ContentElement({
        type: ContentElement.Type.Section,
        value: title,
      });
    }
  }

  export namespace ContentElement {
    /**
     * Post content element type
     *
     * @since 0.1.0
     */
    export enum Type {
      Text = "text",
      Html = "html",
      Image = "image",
      FullWidthImage = "full_width_image",
      Video = "video",
      Carousel = "carousel",
      Section = "section",
    }
  }

  /**
   * Wrapper of a referenced post.
   *
   * @since 0.1.0
   */
  export class Reference {
    /**
     * The reference type.
     *
     * @since 0.1.0
     */
    type: Reference.Type;

    /**
     * The reference post.
     *
     * @since 0.1.0
     */
    post: Post;

    constructor({ type, post }: { type: Reference.Type; post: Post }) {
      this.type = type;
      this.post = post;
    }

    /**
     * Create repost reference.
     *
     * @since 0.1.0
     */
    static repost({ post }: { post: Post }): Reference {
      return new Reference({ type: Reference.Type.Repost, post: post });
    }

    /**
     * Create quote reference.
     *
     * @since 0.1.0
     */
    static quote({ post }: { post: Post }): Reference {
      return new Reference({ type: Reference.Type.Quote, post: post });
    }

    /**
     * Create reply reference.
     *
     * @since 0.1.0
     */
    static reply({ post }: { post: Post }): Reference {
      return new Reference({ type: Reference.Type.Reply, post: post });
    }
  }

  export namespace Reference {
    /**
     * The post reference type
     *
     * @since 0.1.0
     */
    export enum Type {
      Repost = "repost",
      Quote = "quote",
      Reply = "reply",
    }
  }
}
