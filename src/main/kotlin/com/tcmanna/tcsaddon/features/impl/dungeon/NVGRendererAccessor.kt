package com.tcmanna.tcsaddon.features.impl.dungeon

interface NVGRendererAccessor {
    fun `tcsaddon$ringSector`(cx: Float, cy: Float, innerRadius: Float, outerRadius: Float, startAngle: Float, endAngle: Float, color: Int)
}