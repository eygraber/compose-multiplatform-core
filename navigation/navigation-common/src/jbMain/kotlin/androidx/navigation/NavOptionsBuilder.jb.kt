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

import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

@NavOptionsDsl
public actual class NavOptionsBuilder {
    private val builder = NavOptions.Builder()
    public actual var launchSingleTop: Boolean = false

    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public actual var restoreState: Boolean = false

    public actual var popUpToRoute: String? = null
        private set(value) {
            if (value != null) {
                require(value.isNotBlank()) { "Cannot pop up to an empty route" }
                field = value
                inclusive = false
            }
        }
    private var inclusive = false
    private var saveState = false

    public actual var popUpToRouteClass: KClass<*>? = null
        private set(value) {
            if (value != null) {
                field = value
                inclusive = false
            }
        }

    public actual var popUpToRouteObject: Any? = null
        private set(value) {
            if (value != null) {
                field = value
                inclusive = false
            }
        }

    public actual fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        popUpToRoute = route
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    public actual inline fun <reified T : Any> popUpTo(
        noinline popUpToBuilder: PopUpToBuilder.() -> Unit
    ) {
        popUpTo(T::class, popUpToBuilder)
    }

    // this restricted public is needed so that the public reified [popUpTo] can call
    // private popUpToRouteClass setter
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun <T : Any> popUpTo(
        klass: KClass<T>,
        popUpToBuilder: PopUpToBuilder.() -> Unit
    ) {
        popUpToRouteClass = klass
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    // align with other popUpTo overloads where this is suppressed in baseline lint ignore
    @Suppress("BuilderSetStyle", "MissingJvmstatic")
    public actual fun <T : Any> popUpTo(route: T, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        popUpToRouteObject = route
        popUpToRoute = null
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    internal actual fun build() = builder.apply {
        setLaunchSingleTop(launchSingleTop)
        setRestoreState(restoreState)
        if (popUpToRoute != null) {
            setPopUpTo(popUpToRoute, inclusive, saveState)
        } else if (popUpToRouteClass != null) {
            setPopUpTo(popUpToRouteClass!!, inclusive, saveState)
        } else if (popUpToRouteObject != null) {
            setPopUpTo(popUpToRouteObject!!, inclusive, saveState)
        }
    }.build()
}
