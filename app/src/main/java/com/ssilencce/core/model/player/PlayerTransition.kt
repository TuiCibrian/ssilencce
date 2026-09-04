package com.ssilencce.core.model.player

import com.ssilencce.R

enum class PlayerTransition(
    val nameRes: Int,
) {
    Simple(R.string.simple),
    Cascading(R.string.cascading),
    Depth(R.string.depth),
    Hinge(R.string.hinge),
    HorizontalFlip(R.string.horizontal_flip),
    VerticalFlip(R.string.vertical_flip),
    Stack(R.string.stack),
    ZoomOut(R.string.zoom_out),
    Parallax(R.string.parallax),
    ;
}
