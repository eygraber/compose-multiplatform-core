/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String?,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route).apply(builder)
    .build()

public actual inline fun NavigatorProvider.navigation(
    startDestination: KClass<*>,
    route: KClass<*>?,
    typeMap: Map<KType, NavType<*>>,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route, typeMap).apply(builder)
    .build()

public actual inline fun NavigatorProvider.navigation(
    startDestination: Any,
    route: KClass<*>?,
    typeMap: Map<KType, NavType<*>>,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route, typeMap).apply(builder)
    .build()

public actual inline fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, route).apply(builder))

public actual inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: KClass<*>,
    typeMap: Map<KType, NavType<*>>,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, T::class, typeMap)
    .apply(builder))

public actual inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: Any,
    typeMap: Map<KType, NavType<*>>,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, T::class, typeMap)
    .apply(builder))

@NavDestinationDsl
public actual open class NavGraphBuilder : NavDestinationBuilder<NavGraph> {
    public actual val provider: NavigatorProvider
    private var startDestinationRoute: String? = null
    private var startDestinationClass: KClass<*>? = null
    private var startDestinationObject: Any? = null

    public actual constructor(
        provider: NavigatorProvider,
        startDestination: String,
        route: String?
    ) : super(provider[NavGraphNavigator.name], route) {
        this.provider = provider
        this.startDestinationRoute = startDestination
    }

    public actual constructor(
        provider: NavigatorProvider,
        startDestination: KClass<*>,
        route: KClass<*>?,
        typeMap: Map<KType, NavType<*>>
    ) : super(provider[NavGraphNavigator.name], route, typeMap) {
        this.provider = provider
        this.startDestinationClass = startDestination
    }

    public actual constructor(
        provider: NavigatorProvider,
        startDestination: Any,
        route: KClass<*>?,
        typeMap: Map<KType, NavType<*>>
    ) : super(provider[NavGraphNavigator.name], route, typeMap) {
        this.provider = provider
        this.startDestinationObject = startDestination
    }

    private val destinations = mutableListOf<NavDestination>()

    public actual fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>) {
        destinations += navDestination.build()
    }

    public actual operator fun NavDestination.unaryPlus() {
        addDestination(this)
    }

    public actual fun addDestination(destination: NavDestination) {
        destinations += destination
    }

    @OptIn(InternalSerializationApi::class)
    override fun build(): NavGraph = super.build().also { navGraph ->
        navGraph.addDestinations(destinations)
        if (startDestinationRoute == null &&
            startDestinationClass == null && startDestinationObject == null) {
            if (route != null) {
                throw IllegalStateException("You must set a start destination route")
            } else {
                throw IllegalStateException("You must set a start destination id")
            }
        }
        if (startDestinationRoute != null) {
            navGraph.setStartDestination(startDestinationRoute!!)
        } else if (startDestinationClass != null) {
            navGraph.setStartDestination(startDestinationClass!!.serializer()) { it.route!! }
        } else if (startDestinationObject != null) {
            navGraph.setStartDestination(startDestinationObject!!)
        }
    }
}
