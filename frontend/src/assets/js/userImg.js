// Resolve user avatar: custom image > default image > empty string
export default function userImg(u, blobUrl) {
    if (!u) return "";
    return u.imgUrl
        ? blobUrl(u.imgUrl)
        : u.defaultImgUrl
            ? blobUrl(u.defaultImgUrl)
            : "";
}
