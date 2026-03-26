package com.verdant.core.model

enum class VisualizationType {
    /** GitHub-style contribution grid — best for Binary streaks */
    PIXEL_GRID,
    /** Topographic contour map — best for Checkpoint milestones */
    TOPO_MAP,
    /** Audio waveform bars — best for Duration & Intensity sessions */
    AUDIO_WAVEFORM,
    /** Liquid-filling jar — best for Volume accumulation */
    PHYSICS_JAR,
    /** Radar/spider chart — best for multi-dimensional comparison */
    RPG_RADAR,
}

/** Returns the default visualization for a given tracking type. */
fun TrackingType.defaultVisualization(): VisualizationType = when (this) {
    TrackingType.BINARY -> VisualizationType.PIXEL_GRID
    TrackingType.CHECKPOINT -> VisualizationType.TOPO_MAP
    TrackingType.DURATION -> VisualizationType.AUDIO_WAVEFORM
    TrackingType.QUANTITATIVE -> VisualizationType.PHYSICS_JAR
    TrackingType.LOCATION -> VisualizationType.PIXEL_GRID
    TrackingType.FINANCIAL -> VisualizationType.PHYSICS_JAR
}

/** Returns the set of tracking types this visualization works best with. */
fun VisualizationType.recommendedFor(): Set<TrackingType> = when (this) {
    VisualizationType.PIXEL_GRID -> setOf(TrackingType.BINARY, TrackingType.LOCATION)
    VisualizationType.TOPO_MAP -> setOf(TrackingType.CHECKPOINT, TrackingType.BINARY)
    VisualizationType.AUDIO_WAVEFORM -> setOf(TrackingType.DURATION, TrackingType.QUANTITATIVE)
    VisualizationType.PHYSICS_JAR -> setOf(TrackingType.QUANTITATIVE, TrackingType.FINANCIAL, TrackingType.DURATION)
    VisualizationType.RPG_RADAR -> setOf(TrackingType.CHECKPOINT, TrackingType.QUANTITATIVE)
}
