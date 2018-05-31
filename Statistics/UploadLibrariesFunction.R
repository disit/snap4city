# /* UPLOAD LIBRERIES FUNCTION CONTRIB SNAP4CITY USER
# Copyright (C) 2018 DISIT Lab http://www.disit.org - University of Florence
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>. */


#UPLOAD LIBRERIES FUNCTION
packagesInstallation <- function(){
   if("ggplot2" %in% rownames(installed.packages()) == FALSE) {install.packages("ggplot2")}
   if("gridExtra" %in% rownames(installed.packages()) == FALSE) {install.packages("gridExtra")}
   if("ggalt" %in% rownames(installed.packages()) == FALSE) {install.packages("ggalt")}
   if("gtable" %in% rownames(installed.packages()) == FALSE) {install.packages("gtable")}
   if("reshape2" %in% rownames(installed.packages()) == FALSE) {install.packages("reshape2")}
   if("psych" %in% rownames(installed.packages()) == FALSE) {install.packages("psych")}
   if("forecast" %in% rownames(installed.packages()) == FALSE) {install.packages("forecast")}
   if("corrplot" %in% rownames(installed.packages()) == FALSE) {install.packages("corrplot")}
   if("broom" %in% rownames(installed.packages()) == FALSE) {install.packages("broom")}
   if("RMySQL" %in% rownames(installed.packages()) == FALSE) {install.packages("RMySQL")}
   if("dplyr" %in% rownames(installed.packages()) == FALSE) {install.packages("dplyr")}
   if("xts" %in% rownames(installed.packages()) == FALSE) {install.packages("xts")}
   if("plumber" %in% rownames(installed.packages()) == FALSE) {install.packages("plumber")}
   if("caret" %in% rownames(installed.packages()) == FALSE) {install.packages("caret")}
   if("grid" %in% rownames(installed.packages()) == FALSE) {install.packages("grid")}
   if("randomForest" %in% rownames(installed.packages()) == FALSE) {install.packages("randomForest")}
   if("AnomalyDetection" %in% rownames(installed.packages()) == FALSE) {install.packages("AnomalyDetection")}
   if("jsonlite" %in% rownames(installed.packages()) == FALSE) {install.packages("jsonlite")}
   if("RODBC" %in% rownames(installed.packages()) == FALSE) {install.packages("RODBC")}
   
  library(ggplot2)
  library(caret)
  library(gridExtra)
  library(ggalt)
  library(gtable)
  library(reshape2)
  library(psych)
  library(forecast)
  library(corrplot)
  library(broom)
  library(jsonlite)
  library(RODBC)
  library(RMySQL)
  library(dplyr)
  library(xts)
  library(plumber)
  library(grid)
  library(randomForest)
  library(AnomalyDetection)
}