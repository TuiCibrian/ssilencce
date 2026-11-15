"""Source/pure probe for the AA-05 special-shuffle ordering contract.

Read-only: parses the two production files and simulates the start-item plan.
It does not build, install, or run the app.
"""

import json
import os
import re
import sys

ROOT = r"C:\Users\geode\Documents\Repos\ssilencce"
VM = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "ui", "screen", "player", "PlayerViewModel.kt")
SVC = os.path.join(ROOT, "app", "src", "main", "java", "com", "ssilencce", "playback", "PlaybackService.kt")
OUT = os.path.join(ROOT, "advance-shuffle-2026-11-06", "evidence-4da", "probe_order_contract.json")


def section(text, start, end):
    i = text.index(start)
    j = text.index(end, i + len(start))
    return text[i:j]


def plan_start(new_queue, current_id, position):
    """The contract: resolve the index on the new queue, then carry position."""
    try:
        index = new_queue.index(current_id)
    except ValueError:
        return {"id": new_queue[0], "index": 0, "position": 0}
    return {"id": new_queue[index], "index": index, "position": position}


def restore_after_order(new_queue, current_id, position):
    """Post-order contract: re-resolve by media id, then seek to the exact position."""
    try:
        index = new_queue.index(current_id)
    except ValueError:
        return {"id": new_queue[0], "index": 0, "position": 0}
    return {"id": new_queue[index], "index": index, "position": position}


def main():
    vm = open(VM, encoding="utf-8").read()
    svc = open(SVC, encoding="utf-8").read()

    vm_block = section(vm, "fun openSpecialShuffle(", "fun openPlaylist(")
    set_block = section(svc, "Playback.SET_UNSHUFFLED_ORDER ->", "Playback.SET_STOP_POSITION ->")
    on_set_block = section(svc, "private fun applyPendingUnshuffledOrder(", "override fun onCustomCommand(")
    timeline_block = section(svc, "override fun onTimelineChanged(", "override fun onPlayWhenReadyChanged(")

    checks = {
        "vm_captures_position_and_play_state": all(
            token in vm_block
            for token in ("val currentPosition = controller.currentPosition", "val wasPlaying = controller.playWhenReady")
        ),
        "vm_computes_index_on_new_items": "mediaItems.indexOfFirst { it.mediaId == mediaId }" in vm_block,
        "vm_suspends_old_shuffle_before_install": (
            "val wasShuffleEnabled = controller.shuffleModeEnabled" in vm_block
            and vm_block.index("if (wasShuffleEnabled)") < vm_block.index("controller.setMediaItems(mediaItems")
        ),
        "vm_installs_new_timeline_before_play_state": (
            vm_block.index("controller.setMediaItems(mediaItems, currentIndex") < vm_block.index("controller.playWhenReady = wasPlaying")
        ),
        "vm_installs_new_timeline_before_order_request": (
            vm_block.index("controller.setMediaItems(mediaItems, currentIndex") < vm_block.index("controller.sendCustomCommand(")
        ),
        "vm_applies_order_before_play_state": (
            vm_block.index("resultFuture.await()") < vm_block.index("controller.playWhenReady = wasPlaying")
        ),
        "vm_restores_shuffle_before_play_state": (
            vm_block.index("controller.shuffleModeEnabled = true") < vm_block.index("controller.playWhenReady = wasPlaying")
        ),
        "vm_carries_new_count_in_order_command": "Playback.EXTRA_MEDIA_ITEM_COUNT, mediaItems.size" in vm_block,
        "vm_carries_new_index_in_order_command": "Playback.EXTRA_START_INDEX, currentIndex.coerceAtLeast(0)" in vm_block,
        "vm_carries_first_media_id": "Playback.EXTRA_FIRST_MEDIA_ID, mediaItems.first().mediaId" in vm_block,
        "service_count_path_does_not_build_old_order": (
            "pendingUnshuffledOrderCount = requestedCount" in set_block
            and "UnshuffledShuffleOrder(player.mediaItemCount)" not in set_block.split("} else {")[0]
        ),
        "service_skips_improved_order_while_pending": (
            "val pendingSpecialOrder = pendingUnshuffledOrderCount > 0" in on_set_block
            and "if (!pendingSpecialOrder)" in on_set_block
        ),
        "service_skips_order_when_shuffle_disabled": "player.shuffleModeEnabled &&" in on_set_block,
        "service_immediate_apply_checks_new_index": "player.currentMediaItemIndex == requestedIndex" in set_block,
        "service_immediate_apply_checks_new_first_id": "player.getMediaItemAt(0).mediaId == requestedFirstId" in set_block,
        "service_applies_order_after_new_timeline": (
            "timeline.windowCount == pendingUnshuffledOrderCount" in timeline_block
            and "applyPendingUnshuffledOrder(timeline.windowCount)" in timeline_block
        ),
        "service_order_uses_new_count": "UnshuffledShuffleOrder(count)" in on_set_block,
        "vm_restores_media_id_after_order": (
            "controller.getMediaItemAt(index).mediaId == mediaId" in vm_block
            and "controller.seekTo(restoredIndex, currentPosition)" in vm_block
        ),
        "vm_restores_position_after_order": "controller.seekTo(restoredIndex, currentPosition)" in vm_block,
        "vm_restores_after_order_request": (
            vm_block.index("resultFuture.await()") < vm_block.index("controller.seekTo(restoredIndex")
        ),
        "vm_restores_before_play_state": (
            vm_block.index("controller.seekTo(restoredIndex") < vm_block.index("controller.playWhenReady = wasPlaying")
        ),
        "vm_falls_back_to_zero_when_id_absent": "controller.seekTo(0, 0L)" in vm_block,
    }

    # Pure contract: old queue must not decide the new start item.
    old_queue = [60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79]
    new_queue = [70, 65, 78, 76, 67, 180, 77, 72, 68, 69, 71, 66, 62, 79, 74, 63, 61, 75, 73, 64, 60]
    preserved = plan_start(new_queue, 180, 2921)
    fallback = plan_start(new_queue, 999, 2921)
    post_order = restore_after_order(new_queue, 180, 2921)
    pure = {
        "preserved_id": preserved["id"],
        "preserved_index": preserved["index"],
        "preserved_position": preserved["position"],
        "post_order_id": post_order["id"],
        "post_order_index": post_order["index"],
        "post_order_position": post_order["position"],
        "fallback_id": fallback["id"],
        "fallback_index": fallback["index"],
        "old_queue_length": len(old_queue),
        "new_queue_length": len(new_queue),
    }
    checks["pure_preserves_current_item"] = preserved == {"id": 180, "index": 5, "position": 2921}
    checks["pure_does_not_select_next_item"] = preserved["id"] != 77
    checks["pure_fallback_is_index_zero"] = fallback == {"id": 70, "index": 0, "position": 0}
    checks["pure_uses_new_queue_count"] = len(new_queue) == 21 and len(old_queue) == 20
    checks["pure_post_order_restores_item"] = post_order == {"id": 180, "index": 5, "position": 2921}
    checks["pure_post_order_index_points_to_id"] = new_queue[post_order["index"]] == 180
    checks["pure_post_order_position_is_exact"] = post_order["position"] == 2921
    checks["vm_debug_diagnostics_guarded"] = (
        vm.count("if (BuildConfig.DEBUG)") == 5
        and vm.count("Log.d(") == 5
        and "AA05Shuffle" in vm
    )
    checks["service_debug_diagnostics_guarded"] = (
        svc.count("if (BuildConfig.DEBUG)") == 5
        and svc.count("Log.d(") == 5
        and "AA05Shuffle" in svc
    )
    checks["debug_tag_stable"] = "AA05Shuffle" in vm and "AA05Shuffle" in svc

    passed = all(checks.values())
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump({"checks": checks, "pure": pure, "passed": passed}, f, indent=2)
    print("AA05 ORDER CONTRACT", "PASS" if passed else "FAIL")
    for k, v in checks.items():
        print(("PASS " if v else "FAIL ") + k)
    print(json.dumps(pure, indent=2))
    if not passed:
        sys.exit(1)


if __name__ == "__main__":
    main()
