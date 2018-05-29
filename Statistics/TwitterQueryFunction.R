#MySQL Query Function
TwitterDataQuery <- function(currentDate, daysNumber){
  
  tableList <- c("twitter_content_extraction","fake_sa_db")
  for (i in 1:length(tableList)){
    mydb = dbConnect(MySQL(), user="root", password="root", dbname = tableList[i], host="192.168.0.64")
    if (tableList[i] == "twitter_content_extraction"){
      fieldList <- c("chart_twitter_canali", "chart_twitter_retweet_canali")#,"chart_twitter", "chart_twitter_retweet")
      channelList <- c("Firenze", "Firenze")#, "#Firenze", "#Firenze")
      whereList <- c("canale LIKE", "canale LIKE")#, "request LIKE", "request LIKE")
      fieldPerChannel <- as.matrix(cbind(fieldList,channelList,whereList))
      
      for (j in 1:dim(fieldPerChannel)[1]){
        dbListFields(mydb, fieldList[j])
        query <- paste("SELECT * FROM ",tableList[i], ".", fieldPerChannel[j, "fieldList"],
                       " WHERE ",fieldPerChannel[j, "whereList"], " '" ,fieldPerChannel[j, "channelList"],"' AND data >= '",currentDate,
                       " 00:00:00' - INTERVAL ", daysNumber ," DAY ORDER BY data DESC",
                       sep="")
        rs = dbSendQuery(mydb, query)
        twitterDataTemp <- fetch(rs, n=-1)
        dbClearResult(rs)
        
        twitterDataTemp[, "variable"] <- "TotalCount"
        twitterDataTemp[, "channel_name"] <- fieldPerChannel[j, "channelList"]
        twitterData <- twitterDataTemp[, c("data", "count", "channel_name", "variable")]
        
        setwd("~/Snap4City/Sensors Data/TwitterCSVFiles")
        write.csv(twitterData, paste(fieldPerChannel[j, "fieldList"],".csv",sep=""))
      } 
      
      
    }else if (tableList[i] == "fake_sa_db"){
      fieldList <- c("channel_sum_double", "channel_sum")#, "search_sum_double", "search_sum")
      channelList <- c(" LIKE 'Firenze'", " LIKE 'Firenze'")#, " = 7", " = 7")
      whereList <- c("channel_name", "channel_name")#, "search_id", "search_id")
      fieldPerChannel <- as.matrix(cbind(fieldList,channelList,whereList))
      
      for (h in 1:dim(fieldPerChannel)[1]){
        dbListFields(mydb, fieldList[h]) #This will return a list of the fields in CHART_TWITTER_CANALI 
        query <- paste("SELECT * FROM ", tableList[i], ".", fieldPerChannel[h, "fieldList"],
                       " WHERE ", fieldPerChannel[h, "whereList"] , fieldPerChannel[h, "channelList"]," AND date_time >= '",currentDate,
                       " 00:00:00' - INTERVAL ", daysNumber ," DAY ORDER BY date_time DESC",
                       sep="")
        rs = dbSendQuery(mydb, query)
        twitterDataTemp <- fetch(rs, n=-1)
        dbClearResult(rs)

        #automatic data melting
        twitterData <- matrix(NA, nrow = 1, ncol = 4)
        colnames(twitterData) <- c("date_time", "value", whereList[h], "variable")
        twitterData <- as.data.frame(twitterData)
        variableNames <- colnames(twitterDataTemp)[which(colnames(twitterDataTemp) != "id" &
                                                           colnames(twitterDataTemp) != "date_time" & 
                                                           colnames(twitterDataTemp) != "insert_date_time" &
                                                           colnames(twitterDataTemp) != whereList[h])]
        # integration between melting datasets
        for (k in 1:length(variableNames)) {
          dataTemp <- twitterDataTemp[, c("date_time", whereList[h], variableNames[k])]
          meltingData <- melt(dataTemp,
                              id = c(whereList[h], "date_time"),
                              na.rm = F)
          twitterData <- rbind(twitterData, meltingData)
        }
        twitterData <- twitterData[-1, ]
        setwd("~/Snap4City/Sensors Data/TwitterCSVFiles")
        write.csv(twitterData, paste(fieldPerChannel[h, "fieldList"],"_SA.csv",sep=""))

      }
    }

    dbDisconnect(mydb)

  }
  

  #Twitter CSV Files integration
  #reading of all csv from TwitterCSVFiles folder
  directory <- "~/Snap4City/Sensors Data/TwitterCSVFiles"
  files <- list.files(directory)                                              
  
  twData <- read.csv(paste(directory, files[1], sep="/"))
  if (length(grep("value", colnames(twData))) == 0){
    colnames(twData)[grep("count", colnames(twData))] = "value"
  }
  if (length(grep("date_time", colnames(twData))) == 0){
    colnames(twData)[grep("data", colnames(twData))] = "date_time"
  }
  
  if(files[1] == "channel_sum_double_SA.csv"){
    twData[ ,"identifier"] <- "PosNegSA"
  }else if(files[1] == "channel_sum_SA.csv"){
    twData[ ,"identifier"] <- "PosNegSA"
  }else if(files[1] == "chart_twitter_canali.csv"){
    twData[ ,"identifier"] <- "TweetRetweet"
  }else if(files[1] == "chart_twitter_retweet_canali.csv"){
    twData[ ,"identifier"] <- "Retweet"
  }


  for (i in 2:length(files)){
    twDataTemp <- read.csv(paste(directory, files[i], sep="/"))
    if (length(grep("value", colnames(twDataTemp))) == 0){
      colnames(twDataTemp)[grep("count", colnames(twDataTemp))] = "value"
    }
    if (length(grep("date_time", colnames(twDataTemp))) == 0){
      colnames(twDataTemp)[grep("data", colnames(twDataTemp))] = "date_time"
    }
    if(files[i] == "channel_sum_double_SA.csv"){
      twDataTemp[ ,"identifier"] <- "PosNegSA"
    }else if(files[i] == "channel_sum_SA.csv"){
      twDataTemp[ ,"identifier"] <- "PosNegSA"
    }else if(files[i] == "chart_twitter_canali.csv"){
      twDataTemp[ ,"identifier"] <- "TweetRetweet"
    }else if(files[i] == "chart_twitter_retweet_canali.csv"){
      twDataTemp[ ,"identifier"] <- "Retweet"
    }
    
    twData <- rbind(twData, twDataTemp)
  }
  #non considero channel_name 
  twData <- twData[ ,c("date_time","value","variable","identifier")] 
  twData$date_time <- format(strptime(twData$date_time, "%Y-%m-%d %H:%M"), "%Y-%m-%d %H:%M")
  
  #--------
  newDataset <- matrix(NA, ncol = 5, nrow = 1)
  newDataset <- as.data.frame(newDataset)
  colnames(newDataset) <- c("identifier","date_time","alignDateTime","value","variable")
  
  uniqueID <- unique(twData$identifier)
  
  for (k in 1:length(uniqueID)){
    index <- which(twData[,"identifier"] == uniqueID[k])
    dataTemp <- twData[index, ]
    uniqueVar <- unique(dataTemp$variable)
    
    dateTimeTemp <- as.POSIXct(dataTemp[ , "date_time"])
    dataTemp[ , "alignDateTime"] <- format(align.time(dateTimeTemp, n=60*10), "%Y-%m-%d %H:%M")  # n is in seconds
    
    dataTemp <- dataTemp[, c("identifier","date_time","alignDateTime","value","variable")]
    newDataset <- rbind(newDataset, dataTemp)
  }
  newDataset <- newDataset[-1, ]
  indexDup <- row.names(newDataset[which(duplicated(newDataset)), c("identifier","alignDateTime","variable")])
  indexDup <- as.numeric(indexDup)
  
  if (length(indexDup) != 0){
    DatasetCleanded <- newDataset[-indexDup, ]
  }else{
    DatasetCleanded <- newDataset
  }
  setwd("~/Snap4City/Sensors Data/FinalDatasets")
  write.csv(DatasetCleanded, "TwitterDataset.csv")

}