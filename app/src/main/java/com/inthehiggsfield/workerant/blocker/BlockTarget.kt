package com.inthehiggsfield.workerant.blocker

/**
 * Something the blocker can block.
 *
 * [markerViewIds] are Android view IDs (the part after ":id/") that only appear on the
 * doomscrolling screen. When one is visible, the blocker presses Back.
 * Apps rename these in updates — if a target stops working, turn on
 * "Log screen IDs" in the Blocker tab, open the feature, and look in Logcat for the new ID.
 *
 * If [blocksWholeApp] is true, the whole app is the feed, so the blocker sends you Home instead.
 */
enum class BlockTarget(
    val label: String,
    val description: String,
    val packages: List<String>,
    val markerViewIds: List<String> = emptyList(),
    val blocksWholeApp: Boolean = false,
) {
    InstagramReels(
        label = "Instagram Reels",
        description = "Blocks the Reels viewer. Feed, Stories, and DMs still work.",
        packages = listOf("com.instagram.android"),
        markerViewIds = listOf("clips_viewer_view_pager", "clips_video_container", "clips_swipe_refresh_container"),
    ),
    YouTubeShorts(
        label = "YouTube Shorts",
        description = "Blocks the Shorts player. Regular videos and search still work.",
        packages = listOf("com.google.android.youtube"),
        markerViewIds = listOf("reel_recycler", "reel_player_page_container", "reel_watch_player"),
    ),
    TikTok(
        label = "TikTok",
        description = "TikTok is all feed, so this blocks the whole app and sends you Home.",
        packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        blocksWholeApp = true,
    );

    /** Full view IDs as Android reports them, e.g. "com.instagram.android:id/clips_viewer_view_pager". */
    fun fullViewIds(packageName: String): List<String> = markerViewIds.map { "$packageName:id/$it" }
}
