rootProject.name = "dbn-plugin"
include("modules:dbn-api", "modules:dbn-spi")
project(":modules:dbn-api").projectDir = file("modules/dbn-api")
project(":modules:dbn-spi").projectDir = file("modules/dbn-spi")