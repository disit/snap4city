TrendPlots <- function(currentDate){
  
  inputWD <- "~/Snap4City/Sensors Data"
  outWD <- "~/Snap4City/StatisticsOutput/TrendPlot"
  csvFileName <- "SensorsDatasetFinal.csv"
  
  #setwd("~/Snap4City/Sensors Data")
  setwd(inputWD)
  #dataset <- read.csv("SensorsDatasetFinal.csv", sep=",")
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
  dataset[indexWe, "dayOfTheWeek"] <- "Weekend"
  dataset[-indexWe, "dayOfTheWeek"] <- "Working Days"
  
  
  #------------------------------------------------------------------------------------------------------------------------------
  #-------------------------- TREND ON THE ENTIRE PERIOD, SPLITTING BY WEEKEND AND WORKING DAYS ---------------------------------
  #------------------------------------------------------------------------------------------------------------------------------
  uniqueST <- unique(dataset$sensorType)
  indfolder = 1
  indResult = 1
  statisticsResult = list()
  statisticsResult[indfolder]$statisticsOutputName = unbox("TrendPlot")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("TrendPlot")
  statisticsResult[[indfolder]]$resultFiles = list()
  
  for (i in 1: length(uniqueST))  {
    
    dataSTemp <- dataset[which(dataset$sensorType == uniqueST[i]), ]  
    
    for (s in 1: length(unique(dataSTemp$variable)))  {
      
      uniqueVar <- as.character(unique(dataSTemp$variable)[s])
      
      dataST <- dataSTemp[which(dataSTemp$variable == uniqueVar), ]
      
      for (h in 1:length(unique(dataST$identifier))){
        dataTemp = dataST[which(dataST$identifier == unique(dataST$identifier)[h]), ]
        means <- NA
        means <- describeBy(dataTemp$value, list(dataTemp$dayOfTheWeek, dataTemp$time)) #means and std deviation for weekend and workdays
        
        tab <- NA
        tab <- do.call("rbind", means)
        tab <- tab[, c("mean","sd")]
        tab[,"time"] <- NA
        tab[,"day"] <- NA
        
        k = 1
        for (j in 1:length(colnames(means))){
          tab[k,"time"] = as.character(colnames(means)[j])
          tab[k+1,"time"] = as.character(colnames(means)[j])
          k = k+2
        }
        
        for (j in seq(1, dim(tab)[1], by = 2)){
          tab[j,"day"] = row.names(means)[1]
          tab[j+1,"day"] = row.names(means)[2]
        }
        
        tab <- tab[ ,c("mean","sd","time","day")]
        
        CPdat = tab[ , c("time","day","mean","sd")]
        CPdat <- CPdat[order(CPdat$time), ]
        Legend= CPdat$day
        setwd(outWD)
        
        ymin = CPdat$mean-CPdat$sd
        ymin[which(ymin < 0)] = 0
        maxlim = max(CPdat$mean+CPdat$sd)+10
        
        ggplot(CPdat, aes(time, mean, group=Legend, color=Legend)) +
          geom_point(color="black", size=1) +
          geom_ribbon(aes(ymin = ymin, ymax = mean+sd), alpha = 0.05) +
          geom_xspline(spline_shape=-0.1, size = 1.5)+
          scale_x_discrete(limits = c("00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                                      "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                                      "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"))+
          xlab("Time of the day") + 
          ylab(uniqueVar) + 
          ylim(0, maxlim) + 
          theme(axis.title = element_text(size=28),axis.text = element_text(size=23), axis.text.x = element_text(angle = 90))+
          theme(#legend.position = c(0.15, 0.15), 
            legend.text = element_text(size=20), 
            legend.title = element_text(size=25))+ 
          theme(plot.title = element_text(size=30, face="bold", hjust = 0.5))+
          ggtitle(paste(unique(dataST$identifier)[h], "Average Trend", sep = " "))
        
        setwd(outWD)
        ggsave(paste(unique(dataST$identifier)[h], uniqueVar, ".png", sep=""), width=5, height=3, units="in", scale=5)
        
        statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
        statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(paste(unique(dataST$identifier)[h], uniqueVar, sep=""))
        statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, paste(unique(dataST$identifier)[h], uniqueVar, ".png", sep=""), sep="/"))
        indResult = indResult + 1
      } 
    }
  }
  
  #------------------------------------------------------------------------------------------------------------------------------
  #-------------------------------------------- DAILY TREND PLOT ----------------------------------------------------------------
  #-------------------------------------------- ON CURRENT DATE ------------------------------------------------------------------ 
  statisticsResult[indfolder]$statisticsOutputName = unbox("TrendPlot")
  statisticsResult[[indfolder]]$statisticsOutputName = unbox("TrendPlot")

  for (i in 1: length(uniqueST))  {
    
    dataST <- dataset[which(dataset$sensorType == uniqueST[i] & dataset$date == currentDate), c("variable","identifier","value","date","time")]  
    dataST <- dataST[order(dataST$time), ]
    
    for (j in 1: length(unique(dataST$variable)))  {
      
      uniqueVar <- as.character(unique(dataST$variable)[j])
      dataSTemp <- dataST[which(dataST$variable == uniqueVar), ]
      
      Legend = dataSTemp$identifier
      
      setwd(outWD)
      
      ggplot(dataSTemp, aes(time, value, group=Legend, color=Legend)) +
        geom_point(color="black", size=0.7) +
        geom_smooth(se=F, linetype="dashed", size=0.5) +
        geom_xspline(spline_shape=-0.1, size=0.3)+
        scale_x_discrete(limits = c("00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                                    "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                                    "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"))+
        xlab("Time of the day") + 
        ylab(uniqueVar) + 
        theme(axis.title = element_text(size=28),axis.text = element_text(size=23), axis.text.x = element_text(angle = 90))+
        theme(legend.text = element_text(size=20), 
              legend.title = element_text(size=25))+ 
        theme(plot.title = element_text(size=30, face="bold", hjust = 0.5))+
        ggtitle(paste(uniqueST[i], uniqueVar, "Daily Trend", sep = " "))
      
      setwd(outWD)
      ggsave(paste(uniqueST[i], uniqueVar, "DailyTrendPlot.png", sep=""), width=5, height=3, units="in", scale=5)
      statisticsResult[[indfolder]]$resultFiles[indResult]$sensor=NULL
      statisticsResult[[indfolder]]$resultFiles[[indResult]]$sensor=unbox(paste(unique(dataST$identifier)[h], uniqueVar, sep=""))
      statisticsResult[[indfolder]]$resultFiles[[indResult]]$png=unbox(paste(outWD, paste(uniqueST[i], uniqueVar, "DailyTrendPlot.png", sep=""), sep="/"))
      indResult = indResult + 1
      
      setwd("~/Snap4City")
      write(jsonlite::toJSON(statisticsResult), "JsonStatisticsResult.json")
      
    } 
  }
  return(statisticsResult[[1]])
}


