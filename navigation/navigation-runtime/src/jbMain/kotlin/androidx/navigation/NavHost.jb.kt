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

public actual inline fun NavHost.createGraph(
    startDestination: String,
    route: String?,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(startDestination, route, builder)

public actual inline fun NavHost.createGraph(
    startDestination: KClass<*>,
    route: KClass<*>?,
    typeMap: Map<KType, NavType<*>>,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(startDestination, route, typeMap, builder)

public actual inline fun NavHost.createGraph(
    startDestination: Any,
    route: KClass<*>?,
    typeMap: Map<KType, NavType<*>>,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(startDestination, route, typeMap, builder)

