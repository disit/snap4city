setwd("~/Snap4City/Snap4CityStatistics")
source('~/Snap4City/Snap4CityStatistics/Stat4CityFunctions.R')
api <- plumber::plumb("Stat4CityFunctions.R")
api$run(host = "0.0.0.0", port=8080)
