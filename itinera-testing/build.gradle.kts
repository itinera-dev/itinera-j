plugins { id("itinera.kotlin-library") }

description =
    "Itinera's test support: recording reporters, fake providers and scripted runs, for an application's tests."

dependencies { api(project(":itinera")) }
