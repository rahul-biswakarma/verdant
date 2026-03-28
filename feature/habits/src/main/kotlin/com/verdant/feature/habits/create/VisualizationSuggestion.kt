package com.verdant.feature.habits.create

import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType
import com.verdant.core.model.defaultVisualization

fun suggestVisualization(trackingType: TrackingType): VisualizationType =
    trackingType.defaultVisualization()
