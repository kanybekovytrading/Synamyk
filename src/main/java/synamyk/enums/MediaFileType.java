package synamyk.enums;

public enum MediaFileType {
    /** User profile avatar → avatars/{userId}/{uuid}.ext */
    AVATAR,
    /** News article cover image → news/{newsId}/{uuid}.ext */
    NEWS_COVER,
    /** Video lesson thumbnail → thumbnails/{videoId}/{uuid}.ext */
    VIDEO_THUMBNAIL
}