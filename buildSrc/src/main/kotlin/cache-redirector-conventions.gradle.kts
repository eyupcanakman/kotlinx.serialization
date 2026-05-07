/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

if (project.findProperty("disable_js_cache_redirector")?.toString()?.toBooleanStrictOrNull() != true) {
    project.configureJsCacheRedirector()
}
