rootProject.name = "bot"
include("qq")
include("telegram")
include("logic")
include("headless")

include("onebot")

if (file("coremail").isDirectory) {
    include("coremail")
}
