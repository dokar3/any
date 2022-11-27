import { Comment, PagedResult } from "any-service-api";

export function fetchPostComments(
  postId: string,
  pageKey: string | null
): PagedResult<Comment[]> {
  const countPerPage = 20;

  let query: string;
  if (pageKey == null) {
    query =
      "appId=a_dd8f2b7d304a10edaf6f29517ea0ca4100a43d1b&" +
      `count=${countPerPage}&` +
      "type=hot&" +
      `url=http://9gag.com/gag/${postId}&` +
      "origin=https://9gag.com";
  } else {
    query = pageKey;
  }
  const url =
    "https://comment-cdn.9gag.com/v2/cacheable/comment-list.json?" + query;

  const json = http.get(url).text;
  if (json == null) {
    return PagedResult.err({ error: "Cannot fetch comments" });
  }

  let result: any;
  try {
    result = JSON.parse(json);
  } catch (e) {
    return PagedResult.err({ error: "Cannot parse comments" });
  }

  if (result.payload?.comments == null) {
    return PagedResult.err({ error: "Unsupported response" });
  }

  return parseComments(result.payload);
}

function parseComments(data: any): PagedResult<Comment[]> {
  const comments: Comment[] = [];
  for (const item of data.comments) {
    const images: string[] = [];
    if (item.media != null) {
      for (const image of item.media) {
        const url = image.imageMetaByType?.image?.url;
        if (url != null) {
          images.push(url);
        }
      }
    }
    const comment = new Comment({
      username: item.user.displayName,
      avatar: item.user.avatarUrl,
      content: item.text,
      upvotes: item.likeCount,
      downvotes: item.dislikeCount,
      images: images,
    });
    comments.push(comment);
  }
  const nextKey = data.next;
  return PagedResult.ok({ data: comments, nextKey: nextKey });
}
