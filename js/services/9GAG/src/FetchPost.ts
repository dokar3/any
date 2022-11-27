import { PagedResult, Post } from "any-service-api";

export function fetchPosts(url: string): PagedResult<Post[]> {
  const json = http.get(url).text;
  if (json == null) {
    return PagedResult.err({ error: "No response" });
  }

  let result: any = null;
  try {
    result = JSON.parse(json);
  } catch (e) {
    return PagedResult.err({ error: "Cannot parse the response" });
  }

  if (result.data?.posts === undefined) {
    return PagedResult.err({ error: "Unsupported response" });
  }

  return parsePosts(result.data);
}

function parsePosts(data: any): PagedResult<Post[]> {
  const nextKey = data.nextCursor;
  const items = data.posts;
  const posts: Post[] = [];
  for (const item of items) {
    const media: Post.Media[] = [];
    const images = item.images;
    if (images.image460sv != null) {
      // Video
      const video = images.image460sv;
      media.push(
        Post.Media.video({
          url: video.url,
          aspectRatio: `${parseInt(video.width)}:${parseInt(video.height)}`,
          thumbnail: images.image700.url,
        })
      );
    } else {
      // Image
      const image = images.image700;
      media.push(
        Post.Media.photo({
          url: image.url,
          aspectRatio: `${parseInt(image.width)}:${parseInt(image.height)}`,
        })
      );
    }

    const post = new Post({
      title: item.title,
      url: item.url,
      summary: item.description,
      author: item.creator?.username,
      authorId: item.creator?.userId,
      avatar: item.creator?.avatarUrl,
      commentCount: item.commentsCount,
      commentsKey: item.id,
      category: item.postSection.name,
      tags: item.tags.map((tag: any) => tag.key),
      media: media,
    });
    posts.push(post);
  }

  return PagedResult.ok({ data: posts, nextKey: nextKey });
}
