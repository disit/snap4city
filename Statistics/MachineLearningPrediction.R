MachineLearingCarParkPredictions <- function(CarParkToPredict){ 

  inputWD <- "~/Snap4City/Sensors Data"
  outWD <- "~/Snap4City/StatisticsOutput/MachineLearningPredictions"
  csvFileName <- "SensorsDatasetFinal.csv"
  
  setwd(inputWD)
  dataset <- read.csv(csvFileName, sep=",")
  dataset <- dataset[-1 , -grep("X", colnames(dataset))]
  
  #minute, hours, time and date
  dataset$minutes <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%M")
  dataset$hour <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%H")
  dataset$time <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%H:%M")
  dataset$date <- format(strptime(dataset$alignDateTime, "%Y-%m-%d %H:%M"), "%Y-%m-%d")
  
  #day of the week
  dataset[ , "days"] <- as.POSIXlt(dataset$date)$wday 
  indexWe <- which(dataset$days  == 6 | dataset$days == 0)
  dataset[indexWe, "weekend"] <- 1
  dataset[-indexWe, "weekend"] <- 0
  
  dataset <- dataset[order(dataset$alignDateTime), ]
  
  #----------------------------------------------------------
  # MACHINE LEARNING - prediction on the current day --------
  #----------------------------------------------------------
  uniqueST <- as.character(unique(dataset$sensorType))
  
  dataFinal <- dcast(dataset, alignDateTime+time+date+days+hour+minutes+weekend ~ identifier+variable, fun.aggregate=mean)
  
  #DATA "IMPUTATION":
  uniqueSensorType <- colnames(dataFinal)[which(colnames(dataFinal)!="time")]
  options(digits = 3)
  for(i in 1:length(uniqueSensorType)){
    for(j in 1:dim(dataFinal)[1]){
      if(is.na(dataFinal[j, uniqueSensorType[i]]) == T){
        index <- which(dataFinal[, uniqueSensorType[i]]!= 0)
        currentValue = dataFinal[index[which(j < index)][1] , uniqueSensorType[i]]
        dataFinal[j, uniqueSensorType[i]] = currentValue
      }
    }
  }
  
  carParkY = paste(CarParkToPredict, "free", sep="_")
  
  carParkNames <- paste(as.character(unique(dataset[which(dataset$sensorType == "CarPark"), "identifier"])), "free", sep="_")
  notconsideredCP <- carParkNames[which(carParkNames != carParkY)]
  
  for (i in 1:length(notconsideredCP)){
    index <- which(colnames(dataFinal) == notconsideredCP[i])
    dataFinal <- dataFinal[, -index]
  }
  
  colnames(dataFinal)[colnames(dataFinal) == carParkY] <- "free"
  colnames(dataFinal)[colnames(dataFinal)== "alignDateTime"] <- "dateTime"
  dataFinal[, "dateTime"] <- as.character(dataFinal[, "dateTime"])
  
  lastDate <- as.character(unique(dataFinal$dateTime)[length(unique(dataFinal$dateTime))])
  
  lastDate <- as.POSIXct(lastDate)
  testDateVec <- as.POSIXct(lastDate)
  addMinVec <- rep(600, 24*6)
  lastDateNew <- lastDate
  for (i in 1:length(addMinVec)){
    lastDateNew <- as.POSIXct(lastDateNew) + addMinVec[i]
    testDateVec <- c(testDateVec, lastDateNew)
  }
  
  testMatrix  <- matrix(NA, ncol=7, nrow=length(testDateVec))
  colnames(testMatrix) <- c("dateTime", "minutes","hour","time","date", "days","weekend")
  
  testMatrix[,"dateTime"] <- format(strptime(testDateVec, "%Y-%m-%d %H:%M"), "%Y-%m-%d %H:%M")
  testMatrix[,"minutes"] <- format(strptime(testDateVec, "%Y-%m-%d %H:%M"), "%M")
  testMatrix[,"hour"] <- format(strptime(testDateVec, "%Y-%m-%d %H:%M"), "%H")
  testMatrix[,"time"] <- format(strptime(testDateVec, "%Y-%m-%d %H:%M"), "%H:%M")
  testMatrix[,"date"] <- format(strptime(testDateVec, "%Y-%m-%d %H:%M"), "%Y-%m-%d")
  
  testMatrix[ ,"days"] <- as.POSIXlt(testMatrix[,"date"])$wday 
  
  indexWe <- which(testMatrix[ ,"days"]  == "6" | testMatrix[ ,"days"] == "0")
  
  if (length(indexWe)==0){
    testMatrix[, "weekend"] <- 0
  }else if(length(indexWe)>0){
    testMatrix[indexWe, "weekend"] <- 1
    testMatrix[-indexWe, "weekend"] <- 0
  }
  
  testMatrix <- as.data.frame(testMatrix)
  testMatrix[, "weekend"] = as.numeric(testMatrix[, "weekend"])
  testMatrix[, "days"] = as.numeric(testMatrix[, "days"])
  
  ind <- which(as.character(testMatrix[,"minutes"]) == "10" | as.character(testMatrix[,"minutes"]) == "40")
  testMatrix <- testMatrix[-ind, ]
  
  ##
  testMatrix[, "prevWeekDateTime"] <-  as.POSIXct(testMatrix[, "dateTime"])-604800
  testMatrix[, "prevWeekDateTime"] <- format(strptime(testMatrix[, "prevWeekDateTime"], "%Y-%m-%d %H:%M"), "%Y-%m-%d %H:%M")
  
  covNames <- NA
  covNames <- colnames(dataFinal)[which(colnames(dataFinal)!="dateTime" & colnames(dataFinal)!="time" & 
                                        colnames(dataFinal)!="days" & colnames(dataFinal)!="free" &
                                        colnames(dataFinal)!="date" & colnames(dataFinal)!="hour" &
                                        colnames(dataFinal)!="weekend" & colnames(dataFinal)!="minutes")]
  for (i in 1:length(covNames)){
    for (j in 1:dim(testMatrix)[1]){
      testMatrix[j, covNames[i]] <- dataFinal[which(dataFinal[,"dateTime"] == testMatrix[j ,"prevWeekDateTime"]),  covNames[i]]
    }
  }
  testMatrix[,"free"] <- NA
  testMatrix[, "hour"] <- as.numeric(testMatrix[, "hour"])
  testMatrix[, "minutes"] <- as.numeric(testMatrix[, "minutes"])
  
  dataFinal[, "hour"] <- as.numeric(dataFinal[, "hour"])
  dataFinal[, "minutes"] <- as.numeric(dataFinal[, "minutes"])
  
  dataTest <- testMatrix[, c("free","days","hour","weekend","minutes",covNames)]
  dataTrain <- na.omit(dataFinal[, c("free","days","hour","weekend","minutes", covNames)])
  
  #TRAINING MODELS LOADING FROM MachineLearningTrainingModels FOLDER
  if (CarParkToPredict == "CarParkBeccaria"){
    setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
    trainMod <- readRDS("Beccaria.rds")
  }else if (CarParkToPredict == "CarParkCareggi"){
    setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
    trainMod <- readRDS("Careggi.rds")
  }else if (CarParkToPredict == "CarParkPieracciniMeyer"){
    setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
    trainMod <- readRDS("Pieraccini.rds")
  }else if (CarParkToPredict == "CarParkS.Lorenzo"){
    setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
    trainMod <- readRDS("S.Lorenzo.rds")
  }else if (CarParkToPredict == "CarParkStazioneFirenzeS.M.N."){
    setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
    trainMod <- readRDS("SMN.rds")
  }
  
  ##TRAINING MODELS
  # train_control <- trainControl(method="cv", number= 10)
  
  # trainModBeccaria <- train(free ~ . , data = dataTrain,  trControl=train_control,  method = "rf")
  # trainModCareggi <- train(free ~ . , data = dataTrain,  trControl=train_control,  method = "rf")
  # trainModPieraccini <- train(free ~ . , data = dataTrain,  trControl=train_control,  method = "rf")
  # trainModS.Lorenzo <- train(free ~ . , data = dataTrain,  trControl=train_control,  method = "rf")
  # trainModSMN <- train(free ~ . , data = dataTrain,  trControl=train_control,  method = "rf")

  # setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
  # saveRDS(trainModBeccaria, "Beccaria.rds")
  # setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
  # saveRDS(trainModCareggi, "Careggi.rds")
  # setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
  # saveRDS(trainModPieraccini, "Pieraccini.rds")
  # setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
  # saveRDS(trainModS.Lorenzo, "S.Lorenzo.rds")
  # setwd("~/Snap4City/Sensors Data/MachineLearningTrainingModels")
  # saveRDS(trainModSMN, "SMN.rds")
  
  
  # plot(trainMod)
  # resampleHist(trainMod)
  
  # variable importance
  #importance = varImp(trainMod)
  # setwd(outWD)
  # png(paste(CarParkToPredict, "VariableImportance.png", sep=""), 
  #     width = 500, height = 500, units = "px", pointsize = 20)
  # plot(importance, main = paste(CarParkToPredict, "\nvariable importance"))
  # dev.off() 

  #predictions
  setwd(outWD)
  predML <- predict(trainMod, dataTest) 
  dVal <- round(predML, 0) 
  
  testMatrix[, "Predicted"] <- dVal 
  dataTestFinal <- testMatrix[ ,c("date","time","Predicted")]

  meltDataTest <- melt(dataTestFinal, id=c("time", "date"), na.rm = F) 
  
  indfolder = 1
  indResult = 1
  statisticsResult = list()
  statisticsResult[indfolder]$statisticsOutputName = unbox("MachineLearningPredictions")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("MachineLearningPredictions")
  statisticsResult[[indfolder]]$resultFiles = list()

  #table
  setwd(outWD)
  r <- c("Date", "Time","Predicted Value")
  h <- dataTestFinal[,"time"]
  png(paste(CarParkToPredict,"MachineLearningPredictionTable.png", sep=""), width = 3000, height = 3000, units = "px", pointsize = 20)
  options(digits=0)
  p3 <- tableGrob(dataTestFinal,  rows = h,
                  cols = r, theme = ttheme_default())
  title <- textGrob(paste("Prediction Values:", "\nRSquare = ",
                          round(max(trainMod$results$Rsquared),2), "\nRMSE = ",
                          round(min(trainMod$results$RMSE),0), sep=" "), gp=gpar(fontsize=18))
  padding <- unit(5,"mm")
  tab <- gtable_add_rows(p3, heights = grobHeight(title) + padding , pos = 0)
  tab <- gtable_add_grob(tab, title, 1, 1, 1, ncol(tab))
  grid.draw(tab)
  grid.arrange(tab)
  grid.draw(tab)
  grid.arrange(tab)
  dev.off()

  statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(CarParkToPredict))
  statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, paste(CarParkToPredict, "MachineLearningPredictionTable.png", sep=""), sep="/"))
  indResult = indResult + 1
  
  #graph
  Legend <- meltDataTest$variable 
  plt <- ggplot(meltDataTest, aes(time, value, group=Legend, color=Legend)) +
    # geom_point(color="grey", size=0.5) +
    geom_line(size=0.8, color="blue")+
    geom_smooth(se=F, linetype="dashed", size=0.8, color="green") +
    xlab("Time of the day") + 
    ylab("Predicted Free Parking Lots") + 
    
    theme(axis.title = element_text(size=21), axis.text = element_text(size=18), axis.text.x = element_text(angle = 90))+
    theme(legend.text = element_text(size=20), 
          legend.title = element_text(size=25))+ 
    theme(plot.title = element_text(size=30, face="bold", hjust = 0.5))+
    ggtitle(paste(CarParkToPredict, "Daily Trend Prediction", sep=" ")) 

   ggsave(paste(CarParkToPredict,"MachineLearningPredictionPlot.png", sep=""),
          width=3, height=2,units="in",scale=10)

    statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(as.character(CarParkToPredict))
    statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, paste(CarParkToPredict, "MachineLearningPredictionPlot.png", sep=""), sep="/"))
    indResult = indResult + 1
   
    setwd("~/Snap4City")
    write(jsonlite::toJSON(statisticsResult), "JsonStatisticsResult.json")
    return(statisticsResult[[1]])
    
 }  
