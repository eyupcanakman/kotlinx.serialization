/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.*
import org.gradle.kotlin.dsl.the
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.replace

const val DEFAULT_YARN_REGISTRY = "https://registry.yarnpkg.com"
const val NPM_REGISTRY_CACHE = "https://cache-redirector.jetbrains.com/registry.npmjs.org"
const val NODE_DIST_CACHE = "https://cache-redirector.jetbrains.com/nodejs.org/dist"
const val YARN_DIST_CACHE = "https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download"

fun Project.configureJsCacheRedirector() {
    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
            restoreYarnLockTaskProvider.configure {
                doLast {
                    // yarn 1.x doesn't and won't support overriding registry used in yarn.lock, so we need to replace it manually
                    // https://github.com/yarnpkg/yarn/issues/6436#issuecomment-426728911
                    val lockFile = outputFile.get()
                    lockFile.writeText(lockFile.readText().replace(DEFAULT_YARN_REGISTRY, NPM_REGISTRY_CACHE))
                }
            }
            // A hacky workaround helping to keep the original URLs in the lock file.
            storeYarnLockTaskProvider.configure {
                val originalLockFile = inputFile.get().asFile.toPath()
                val tempReplacement = originalLockFile.resolveSibling("${originalLockFile.fileName}.${System.nanoTime()}")

                doFirst {
                    // Create a copy of the lock file with preserved URLs,
                    // replace cache-redirector URLs with the original once in the yarn.lock file.
                    originalLockFile.moveTo(tempReplacement)
                    originalLockFile.writeText(
                        tempReplacement.readText().replace(NPM_REGISTRY_CACHE, DEFAULT_YARN_REGISTRY)
                    )
                }
                // In between doFirst and doLast, the task will compare the lock file with the checked-in one.
                doLast {
                    // Rename the lock file with cache-redirector URLs back to yarn.lock
                    tempReplacement.moveTo(originalLockFile, true)
                }
            }
        }

        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(
            YARN_DIST_CACHE
        )
    }

    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(
            NODE_DIST_CACHE
        )
    }
}
