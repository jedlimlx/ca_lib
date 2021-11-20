path = require("path")

config.module.rules.push({
    test: /\.(csv|txt)$/i,
    type: "asset/source"
})

config.resolve.modules.push("src/commonMain/resources")
config.resolve.modules.push("src/commonTest/resources")
config.resolve.modules.push("src/commonTest/resources/simulation")