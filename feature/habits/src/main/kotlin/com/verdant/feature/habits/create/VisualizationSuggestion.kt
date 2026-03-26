package com.verdant.feature.habits.create

import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType

fun suggestVisualization(trackingType: TrackingType): VisualizationType = when (trackingType) {
    TrackingType.BINARY -> VisualizationType.CONTRIBUTION_GRID
    TrackingType.NUMERIC -> VisualizationType.PROGRESS_FILL
    TrackingType.LOCATION -> VisualizationType.CONTRIBUTION_GRID
}
