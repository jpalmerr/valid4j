plugins {
    id("com.gradleup.nmcp.settings") version "1.6.1"
}

rootProject.name = "valid4j"

nmcpSettings {
    centralPortal {
        // Read eagerly into plain strings: nmcp isolates this block into a GradleLifecycle action,
        // which cannot serialize a live Provider.
        //
        // The names are deliberately project-specific rather than the conventional
        // `mavenCentralUsername`/`mavenCentralPassword`, so a clone cannot pick up ambient
        // credentials from ~/.gradle/gradle.properties and attempt an upload under another account.
        username = providers.environmentVariable("CENTRAL_PORTAL_USERNAME").getOrElse("")
        password = providers.environmentVariable("CENTRAL_PORTAL_PASSWORD").getOrElse("")

        // Central publications are irrevocable, so stage the deployment and confirm it by hand in
        // the Portal UI rather than letting a pushed tag publish straight through.
        publishingType = "USER_MANAGED"
    }
}
