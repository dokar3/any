import {
  FetchResult,
  FetchUserByIdParams,
  FetchUserPostsParams,
  PagedResult,
  Post,
  User,
  AnyUserFeature,
} from "any-service-api";
import { BASE_URL } from "./Service";
import { parseShots } from "./ParseShot";

export class UserFeature extends AnyUserFeature {
  fetchById(params: FetchUserByIdParams): FetchResult<User> {
    const id = params.userId;
    const url = BASE_URL + (id[0] === "/" ? "" : "/") + id + "/about";
    const html = http.get(url).text;
    if (html === null) {
      return FetchResult.err({ error: "Cannot fetch the user page" });
    }

    const user = this.parseUser(id, html);
    if (user === null) {
      return FetchResult.err({ error: "Cannot parse the user page" });
    }

    return FetchResult.ok({ data: user });
  }

  fetchPosts(params: FetchUserPostsParams): PagedResult<Post[]> {
    const page = (params.pageKey as number) ?? 1;
    const url = `${BASE_URL}${params.userId}/shots?page=${page}&per_page=24`;
    const options = {
      headers: {
        "x-requested-with": "XMLHttpRequest",
        accept:
          "text/html,application/xhtml+xml,application/xml;q=0.9," +
          "image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange; " +
          "v=b3;q=0.9",
      },
    };
    const html = http.get(url, options).text;
    if (html === null) {
      return PagedResult.err({ error: "Cannot fetch shots" });
    }

    const result = parseShots(html, page);

    if (result.isOk()) {
      let user = this.parseUser(params.userId, html);
      if (user === null) {
        const userResult = this.fetchById(
          new FetchUserByIdParams({ userId: params.userId })
        );
        if (userResult.isErr()) {
          return PagedResult.err({ error: userResult.error });
        } else {
          user = userResult.data;
        }
      }

      for (const post of result.data) {
        post.author = user.name;
        post.authorId = user.id;
        post.avatar = user.avatar;
      }
    }

    return result;
  }

  private parseUser(userId: string, html: string): User | null {
    const doc = DOM.createDocument(html);
    if (doc === null) {
      return null;
    }
    const header = doc.select(".profile-masthead");
    if (header === null) {
      return null;
    }

    const username = header.select(".masthead-profile-name").text().trim();
    const avatar = header.select("img.profile-avatar").attr("src");
    const banner =
      header.select(".masthead-banner-image")?.attr("data-bg") ?? null;

    const shotCountStr = doc.select(".scrolling-subnav .shots .count").text();
    const shotCount = parseInt(shotCountStr);

    let description: string = null;
    let followerCount: number = null;
    let followingCount: number = null;
    const content = doc.select(".about-content");
    if (content !== null) {
      const bioTexts = content.selectAll(".profile-section-bio .bio-text");
      if (bioTexts.length > 0) {
        description = bioTexts
          .map((ele) => {
            return ele.html().replace(/\<br\>/g, "\n");
          })
          .join("\n");
      }
      const counts = content.selectAll(
        ".about-content-main .profile-stats-section .count"
      );
      followerCount = parseInt(counts[0].text().replace(/,/g, ""));
      followingCount = parseInt(counts[1].text().replace(/,/g, ""));
    }

    return new User({
      id: userId,
      name: username,
      avatar: avatar,
      url: BASE_URL + userId,
      banner: banner,
      description: description,
      followerCount: followerCount,
      followingCount: followingCount,
      postCount: shotCount,
    });
  }
}
