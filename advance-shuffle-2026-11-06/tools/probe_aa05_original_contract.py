"""Source/pure probe for the AA-05 original weighted-shuffle restart contract.

Read-only: parses the production files and simulates the restart plan.
It does not build, install, or run the app.
"""

import json
import os
import sys

ROOT = r"C:\Users\geode\Documents\Repos\ssilencce"
VM = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "ui", "screen", "player", "PlayerViewModel.kt")
SVC = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "playback", "PlaybackService.kt")
PLAYBACK = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "playback", "Playback.kt")
SHUFFLE = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "playback", "shuffle", "ShuffleManager.kt")
SPECIAL = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "core", "model", "shuffle", "SpecialShuffleMode.kt")
AVAILABILITY = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "core", "model", "shuffle", "ShuffleModeAvailabilityPolicy.kt")
SHEET = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "ui", "screen", "other", "ShuffleModeBottomSheet.kt")
OUT = os.path.join(ROOT, "advance-shuffle-2026-11-06", "evidence-original-contract-2026-11-10", "probe_order_contract.json")


def section(text, start, end):
    i = text.index(start)
    j = text.index(end, i + len(start))
    return text[i:j]


def restart_plan(new_queue):
    """The selected contract: always start the rebuilt weighted queue at item 0."""
    return {"id": new_queue[0], "index": 0, "position": 0, "play": True}


def main():
    vm = open(VM, encoding="utf-8").read()
    svc = open(SVC, encoding="utf-8").read()
    playback = open(PLAYBACK, encoding="utf-8").read()
    shuffle = open(SHUFFLE, encoding="utf-8").read()
    special = open(SPECIAL, encoding="utf-8").read()
    availability = open(AVAILABILITY, encoding="utf-8").read()
    sheet = open(SHEET, encoding="utf-8").read()

    vm_block = section(vm, "fun openSpecialShuffle(", "fun openPlaylist(")
    open_shuffle_block = section(vm, "fun openShuffle(", "fun openSpecialShuffle(")
    set_block = section(svc, "Playback.SET_UNSHUFFLED_ORDER ->", "Playback.SET_STOP_POSITION ->")
    on_set_block = section(svc, "override fun onSetMediaItems(", "override fun onCustomCommand(")
    timeline_block = section(svc, "override fun onTimelineChanged(", "override fun onPlayWhenReadyChanged(")

    checks = {
        "vm_builds_weighted_queue": "shuffleManager.applySmartShuffle(songs, mode).toMediaItems()" in vm_block,
        "vm_enables_shuffle_before_order": (
            vm_block.index("controller.shuffleModeEnabled = true")
            < vm_block.index("controller.sendCustomCommand(")
        ),
        "vm_requests_plain_unshuffled_order": (
            "SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY)" in vm_block
            and "Playback.EXTRA_MEDIA_ITEM_COUNT" not in vm_block
            and "Playback.EXTRA_START_INDEX" not in vm_block
            and "Playback.EXTRA_FIRST_MEDIA_ID" not in vm_block
        ),
        "vm_installs_new_queue_at_first": "controller.setMediaItems(mediaItems, true)" in vm_block,
        "vm_prepares_and_plays": "controller.prepare()" in vm_block and "controller.play()" in vm_block,
        "vm_does_not_capture_current_item": not any(
            token in vm_block
            for token in ("currentMediaItem", "currentPosition", "playWhenReady", "wasPlaying", "wasShuffleEnabled")
        ),
        "vm_does_not_seek_or_restore": "seekTo(" not in vm_block,
        "vm_has_no_aa05_diagnostics": (
            "AA05Shuffle" not in vm
            and "BuildConfig.DEBUG" not in vm_block
            and "Log.d(" not in vm_block
        ),
        "service_plain_unshuffled_order": (
            "hasSetUnshuffledOrder = true" in set_block
            and "UnshuffledShuffleOrder(player.mediaItemCount)" in set_block
            and "SessionResult.RESULT_SUCCESS" in set_block
            and "pendingUnshuffledOrder" not in set_block
            and "EXTRA_" not in set_block
        ),
        "service_has_no_pending_order_state": "pendingUnshuffledOrder" not in svc,
        "service_has_no_aa05_diagnostics": (
            "AA05Shuffle" not in svc
            and "BuildConfig.DEBUG" not in svc
            and "Log.d(" not in svc
        ),
        "service_onset_uses_original_shuffle_guard": (
            "exoPlayer.shuffleOrder !is ImprovedShuffleOrder && !hasSetUnshuffledOrder" in on_set_block
            and "player.shuffleModeEnabled" not in on_set_block
            and "pendingSpecialOrder" not in on_set_block
        ),
        "service_timeline_only_persists": (
            "persistentStorage.saveState(true)" in timeline_block
            and "pendingUnshuffledOrder" not in timeline_block
        ),
        "playback_has_no_preservation_extras": not any(
            token in playback
            for token in ("EXTRA_MEDIA_ITEM_COUNT", "EXTRA_START_INDEX", "EXTRA_FIRST_MEDIA_ID")
        ),
        "non_special_open_shuffle_unchanged": (
            "controller.setMediaItems(mediaItems)" in open_shuffle_block
            and "controller.prepare()" in open_shuffle_block
            and "controller.play()" in open_shuffle_block
            and "currentMediaItem" not in open_shuffle_block
        ),
        "weighted_album_artist_mode_preserved": (
            "MostPlayedAlbumArtists" in special
            and "MostPlayedAlbumArtists ->" in shuffle
            and "albumArtistWeights" in shuffle
        ),
        "weighted_composer_mode_preserved": (
            "MostPlayedComposers" in special
            and "MostPlayedComposers ->" in shuffle
            and "composerWeights" in shuffle
        ),
        "composer_availability_preserved": (
            "COMPOSER_MIN_SDK = 30" in availability
            and "ShuffleModeAvailabilityPolicy.available(" in sheet
        ),
    }

    old_queue = [60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79]
    new_queue = [70, 65, 78, 76, 67, 180, 77, 72, 68, 69, 71, 66, 62, 79, 74, 63, 61, 75, 73, 64, 60]
    restart = restart_plan(new_queue)
    pure = {
        "restart_id": restart["id"],
        "restart_index": restart["index"],
        "restart_position": restart["position"],
        "restart_play": restart["play"],
        "old_queue_length": len(old_queue),
        "new_queue_length": len(new_queue),
    }
    checks["pure_restart_at_zero"] = restart == {"id": 70, "index": 0, "position": 0, "play": True}
    checks["pure_does_not_preserve_old_current"] = restart["id"] != 180
    checks["pure_uses_new_queue_count"] = len(new_queue) == 21 and len(old_queue) == 20

    passed = all(checks.values())
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump({"checks": checks, "pure": pure, "passed": passed}, f, indent=2)
    print("AA05 ORIGINAL CONTRACT", "PASS" if passed else "FAIL")
    for k, v in checks.items():
        print(("PASS " if v else "FAIL ") + k)
    print(json.dumps(pure, indent=2))
    if not passed:
        sys.exit(1)


if __name__ == "__main__":
    main()
