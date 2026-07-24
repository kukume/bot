rootProject.name = "bot"
include("qq")
include("telegram")
include("logic")
include("onebot")

if (file("headless").isDirectory) {
    include("headless")
}

if (file("coremail").isDirectory) {
    include("coremail")
}
