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
import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.collection.valueIterator
import androidx.navigation.serialization.generateRoutePattern
import androidx.navigation.serialization.generateRouteWithArgs
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

public actual open class NavGraph actual constructor(
    navGraphNavigator: Navigator<out NavGraph>
) : NavDestination(navGraphNavigator), Iterable<NavDestination> {

    public val nodes: SparseArrayCompat<NavDestination> = SparseArrayCompat()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun matchDeepLink(route: String): DeepLinkMatch? {
        // First search through any deep links directly added to this NavGraph
        val bestMatch = super.matchDeepLink(route)
        // Then search through all child destinations for a matching deep link
        val bestChildMatch = mapNotNull { child ->
            child.matchDeepLink(route)
        }.maxOrNull()

        return listOfNotNull(bestMatch, bestChildMatch).maxOrNull()
    }

    public actual fun addDestination(node: NavDestination) {
        val id = node.id
        val innerRoute = node.route
        require(id != 0 || innerRoute != null) {
            "Destinations must have an id or route."
        }
        if (route != null) {
            require(innerRoute != route) {
                "Destination $node cannot have the same route as graph $this"
            }
        }
        require(id != this.id) { "Destination $node cannot have the same id as graph $this" }
        val existingDestination = nodes[id]
        if (existingDestination === node) {
            return
        }
        check(node.parent == null) {
            "Destination already has a parent set. Call NavGraph.remove() to remove the previous " +
                "parent."
        }
        if (existingDestination != null) {
            existingDestination.parent = null
        }
        node.parent = this
        nodes.put(node.id, node)
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        for (node in nodes) {
            if (node == null) {
                continue
            }
            addDestination(node)
        }
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        for (node in nodes) {
            addDestination(node)
        }
    }

    public fun findNode(id: Int): NavDestination? {
        return findNode(id, true)
    }

    public actual fun findNode(route: String?): NavDestination? {
        return if (!route.isNullOrBlank()) findNode(route, true) else null
    }

    public actual inline fun <reified T> findNode(): NavDestination? {
        return findNode(serializer<T>().hashCode())
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    public actual fun <T> findNode(route: T?): NavDestination? {
        return if (route != null) findNode(route!!::class.serializer().hashCode()) else null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findNode(id: Int, searchParents: Boolean): NavDestination? {
        val destination = nodes[id]
        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(id) else null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        // first try matching with routePattern
        val id = createRoute(route).hashCode()
        val destination = nodes[id] ?: nodes.valueIterator().asSequence().firstOrNull {
            // if not found with routePattern, try matching with route args
            it.matchDeepLink(route) != null
        }

        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(route) else null
    }

    // searches through child nodes, does not search through parents
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findChildNode(
        id: Int,
    ): NavDestination? {
        // first search through children directly added to graph
        var destination = nodes[id]
        if (destination != null) return destination

        // then search through child graphs
        destination = nodes.valueIterator().asSequence().firstNotNullOfOrNull { child ->
            if (child is NavGraph) {
                child.findChildNode(id)
            } else null
        }
        return destination
    }

    public actual final override fun iterator(): MutableIterator<NavDestination> {
        return object : MutableIterator<NavDestination> {
            private var index = -1
            private var wentToNext = false
            override fun hasNext(): Boolean {
                return index + 1 < nodes.size()
            }

            override fun next(): NavDestination {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                wentToNext = true
                return nodes.valueAt(++index)
            }

            override fun remove() {
                check(wentToNext) { "You must call next() before you can remove an element" }
                with(nodes) {
                    valueAt(index).parent = null
                    removeAt(index)
                }
                index--
                wentToNext = false
            }
        }
    }

    public actual fun addAll(other: NavGraph) {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val destination = iterator.next()
            iterator.remove()
            addDestination(destination)
        }
    }

    public actual fun remove(node: NavDestination) {
        val index = nodes.indexOfKey(node.id)
        if (index >= 0) {
            nodes.valueAt(index).parent = null
            nodes.removeAt(index)
        }
    }

    public actual fun clear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    override val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() = if (!route.isNullOrBlank()) super.displayName else "the root navigation"

    public actual fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    public actual inline fun <reified T : Any> setStartDestination() {
        setStartDestination(serializer<T>()) { startDestination ->
            startDestination.route!!
        }
    }

    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> setStartDestination(startDestRoute: T) {
        setStartDestination(startDestRoute::class.serializer()) { startDestination ->
            val args = startDestination.arguments.mapValues {
                it.value.type
            }
            startDestRoute.generateRouteWithArgs(args)
        }
    }

    // unfortunately needs to be public so reified setStartDestination can access this
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(ExperimentalSerializationApi::class)
    public actual fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String,
    ) {
        val id = serializer.hashCode()
        val startDest = findNode(id)
        checkNotNull(startDest) {
            "Cannot find startDestination ${serializer.descriptor.serialName} from NavGraph. " +
                "Ensure the starting NavDestination was added with route from KClass."
        }
        startDestinationRoute = parseRoute(startDest)
    }

    /**
     * The route for the starting destination for this NavGraph. When navigating to the
     * NavGraph, the destination represented by this route is the one the user will initially see.
     */
    public actual var startDestinationRoute: String? = null
        private set(startDestRoute) {
            require(startDestRoute != route) {
                "Start destination $startDestRoute cannot use the same route as the graph $this"
            }
            require(!startDestRoute.isNullOrBlank()) {
                "Cannot have an empty start destination route"
            }
            field = startDestRoute
        }

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append(super.toString())
        val startDestination = findNode(startDestinationRoute)
        sb.append(" startDestination=")
        if (startDestination == null) {
            sb.append(startDestinationRoute)
        } else {
            sb.append("{")
            sb.append(startDestination.toString())
            sb.append("}")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavGraph) return false
        return super.equals(other) &&
            nodes == other.nodes
    }

    override fun hashCode(): Int {
        var result = 0
        nodes.forEach { key, value ->
            result = 31 * result + key
            result = 31 * result + value.hashCode()
        }
        return result
    }

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination =
            generateSequence(findNode(startDestinationRoute)) {
                if (it is NavGraph) {
                    it.findNode(it.startDestinationRoute)
                } else {
                    null
                }
            }.last()
    }
}

/**
 * Returns the destination with `route`.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.get(route: String): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/**
 * Returns the destination with `route` from [KClass].
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")

public actual inline operator fun <reified T : Any> NavGraph.get(route: KClass<T>): NavDestination =
    findNode<T>()
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/**
 * Returns the destination with `route` from an Object.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : Any> NavGraph.get(route: T): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public actual operator fun NavGraph.contains(route: String): Boolean = findNode(route) != null

/** Returns `true` if a destination with `route` is found in this navigation graph. */
@Suppress("UNUSED_PARAMETER")
public actual inline operator fun <reified T : Any> NavGraph.contains(route: KClass<T>): Boolean =
    findNode<T>() != null

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public actual operator fun <T : Any> NavGraph.contains(route: T): Boolean = findNode(route) != null

/**
 * Adds a destination to this NavGraph. The destination must have a route set.
 *
 * The destination must not have a [parent][NavDestination.parent] set. If
 * the destination is already part of a [NavGraph], call
 * [NavGraph.remove] before calling this method.</p>
 *
 * @param node destination to add
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.plusAssign(node: NavDestination) {
    addDestination(node)
}

/**
 * Add all destinations from another collection to this one. As each destination has at most
 * one parent, the destinations will be removed from the given NavGraph.
 *
 * @param other collection of destinations to add. All destinations will be removed from the
 * parameter graph after being added to this graph.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.plusAssign(other: NavGraph) {
    addAll(other)
}

/** Removes `node` from this navigation graph. */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.minusAssign(node: NavDestination) {
    remove(node)
}
