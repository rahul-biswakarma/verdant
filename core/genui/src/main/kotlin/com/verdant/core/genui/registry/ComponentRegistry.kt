package com.verdant.core.genui.registry

import com.verdant.core.genui.model.ComponentConfig
import com.verdant.core.genui.model.ComponentType
import com.verdant.core.genui.model.ResolvedData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a single Gen UI component. Implementations live in :feature:home
 * and wrap existing card composables.
 *
 * Note: This is a plain interface (not @Composable) because the registry
 * lives in a non-Compose module. The actual [Render] function is @Composable
 * when called from the renderer in :feature:home.
 */
interface ComponentRenderer {
    /**
     * Render this component given its [config] and resolved [data].
     * Called from a @Composable context in [DynamicDashboard].
     */
    fun render(
        config: ComponentConfig,
        data: ResolvedData,
        onNavigate: (String) -> Unit,
    )
}

/**
 * Maps [ComponentType] values to their [ComponentRenderer] implementations.
 * Populated at app startup via Hilt multi-binding or manual registration.
 */
@Singleton
class ComponentRegistry @Inject constructor() {

    private val renderers = mutableMapOf<ComponentType, Any>()

    /**
     * Register a renderer for a component type. The renderer is stored as [Any]
     * because the actual @Composable rendering interface lives in the feature module.
     */
    fun register(type: ComponentType, renderer: Any) {
        renderers[type] = renderer
    }

    fun get(type: ComponentType): Any? = renderers[type]

    fun hasRenderer(type: ComponentType): Boolean = type in renderers
}
