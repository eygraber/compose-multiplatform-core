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
import androidx.navigation.serialization.generateNavArguments
import androidx.navigation.serialization.generateRoutePattern
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * DSL for constructing a new [NavDestination]
 */
@NavDestinationDsl
public actual open class NavDestinationBuilder<out D : NavDestination>

/**
 * DSL for constructing a new [NavDestination] with a unique route.
 *
 * @param navigator navigator used to create the destination
 * @param route the destination's unique route
 *
 * @return the newly constructed [NavDestination]
 */
public actual constructor(
    /**
     * The navigator the destination was created from
     */
    protected actual val navigator: Navigator<out D>,

    /**
     * The destination's unique route.
     */
    public actual val route: String?
) {
    @OptIn(InternalSerializationApi::class)
    public actual constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, NavType<*>>,
    ) : this(
        navigator,
        route?.serializer()?.generateRoutePattern(typeMap)
    ) {
        route?.apply {
            serializer().generateNavArguments(typeMap).forEach {
                arguments[it.name] = it.argument
            }
        }
        this.typeMap = typeMap
    }

    private lateinit var typeMap: Map<KType, NavType<*>>

    /**
     * The descriptive label of the destination
     */
    public actual var label: CharSequence? = null

    private var arguments = mutableMapOf<String, NavArgument>()

    /**
     * Add a [NavArgument] to this destination.
     */
    public actual fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        arguments[name] = NavArgumentBuilder().apply(argumentBuilder).build()
    }

    /**
     * Add a [NavArgument] to this destination.
     */
    @Suppress("BuilderSetStyle")
    public actual fun argument(name: String, argument: NavArgument) {
        arguments[name] = argument
    }

    private var deepLinks = mutableListOf<NavDeepLink>()

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param uriPattern The uri pattern to add as a deep link
     * @see deepLink
     */
    public actual fun deepLink(uriPattern: String) {
        deepLinks.add(NavDeepLink(uriPattern))
    }

    /**
     * Add a deep link to this destination.
     *
     * The arguments in [T] are expected to be identical (in name and type) to the arguments
     * in the [route] from KClass that was used to construct this [NavDestinationBuilder].
     *
     * Extracts deeplink arguments from [T] and appends it to the [basePath]. See docs on the
     * safe args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's
     * generation logic.
     *
     * In addition to a direct Uri match, [basePath]s without a scheme are assumed
     * as http and https. For example, `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     *
     * @param T The deepLink KClass to extract arguments from
     * @param basePath The base uri path to append arguments onto
     *
     * @see NavDeepLink.Builder.setUriPattern for the final uriPattern's
     * generation logic.
     */
    @Suppress("BuilderSetStyle")
    @JvmName("deepLinkSafeArgs")
    public actual inline fun <reified T : Any> deepLink(
        basePath: String,
    ) {
        deepLink(basePath, T::class) { }
    }

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param navDeepLink the NavDeepLink to be added to this destination
     */
    public actual fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit) {
        deepLinks.add(NavDeepLinkDslBuilder().apply(navDeepLink).build())
    }

    /**
     * Add a deep link to this destination.
     *
     * The arguments in [T] are expected to be identical (in name and type) to the arguments
     * in the [route] from KClass that was used to construct this [NavDestinationBuilder].
     *
     * Extracts deeplink arguments from [T] and appends it to the [basePath]. See docs on the
     * safe args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's
     * generation logic.
     *
     * In addition to a direct Uri match, [basePath]s without a scheme are assumed
     * as http and https. For example, `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     *
     * @param T The deepLink KClass to extract arguments from
     * @param basePath The base uri path to append arguments onto
     * @param navDeepLink the NavDeepLink to be added to this destination
     *
     * @see NavDeepLink.Builder.setUriPattern for the final uriPattern's
     * generation logic.
     */
    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> deepLink(
        basePath: String,
        noinline navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    ) {
        deepLink(basePath, T::class, navDeepLink)
    }

    /**
     * Public delegation for the reified deepLink overloads.
     *
     * Checks for deepLink validity:
     * 1. They used the safe args constructor since we rely on that constructor
     * to add arguments to the destination
     * 2. DeepLink does not contain extra arguments not present in the destination
     * KClass. We will not have its NavType. Even if we do, the destination is not aware of the
     * argument and will just ignore it. In general we don't want safe args deeplinks to
     * introduce new arguments.
     * 3. DeepLink does not contain different argument type for the same arg name
     *
     * For the case where the deepLink is missing required arguments in the [route], existing
     * checks will catch it.
     */
    @OptIn(InternalSerializationApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun <T : Any> deepLink(
        basePath: String,
        route: KClass<T>,
        navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    ) {
        // make sure they used the safe args constructors which automatically adds
        // argument to the destination
        check(this::typeMap.isInitialized) {
            "Cannot add deeplink from KClass [$route]. Use the NavDestinationBuilder " +
                "constructor that takes a KClass with the same arguments."
        }
        val deepLinkArgs = route.serializer().generateNavArguments(typeMap)
        deepLinkArgs.forEach {
            val arg = arguments[it.name]
            // make sure deep link doesn't contain extra arguments not present in the route KClass
            // and that it doesn't contain different arg type
            require(arg != null && arg.type == it.argument.type) {
                "Cannot add deeplink from KClass [$route]. DeepLink contains unknown argument " +
                    "[${it.name}]. Ensure deeplink arguments matches the destination's " +
                    "route from KClass"
            }
        }
        deepLink(navDeepLink(basePath, route, typeMap, navDeepLink))
    }

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param navDeepLink the NavDeepLink to be added to this destination
     */
    @Suppress("BuilderSetStyle")
    public actual fun deepLink(navDeepLink: NavDeepLink) {
        deepLinks.add(navDeepLink)
    }

    /**
     * Instantiate a new instance of [D] that will be passed to [build].
     *
     * By default, this calls [Navigator.createDestination] on [navigator], but can
     * be overridden to call a custom constructor, etc.
     */
    @Suppress("BuilderSetStyle")
    protected actual open fun instantiateDestination(): D = navigator.createDestination()

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    public actual open fun build(): D {
        return instantiateDestination().also { destination ->
            destination.label = label
            arguments.forEach { (name, argument) ->
                destination.addArgument(name, argument)
            }
            if (route != null) {
                destination.route = route
            }
        }
    }
}
