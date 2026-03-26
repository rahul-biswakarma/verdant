package com.verdant.feature.habits.create

import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType

fun suggestVisualization(trackingType: TrackingType): VisualizationType = when (trackingType) {
    TrackingType.BINARY -> VisualizationType.CONTRIBUTION_GRID
    TrackingType.QUANTITATIVE -> VisualizationType.PHYSICS_JAR
    TrackingType.DURATION -> VisualizationType.COMPLETION_RING
    TrackingType.FINANCIAL -> VisualizationType.COMPLETION_RING
    TrackingType.LOCATION -> VisualizationType.CONTRIBUTION_GRID
}
