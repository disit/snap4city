get_scenario_device_data_from_suri <- function(access_token, service_uri, use_snap4city=TRUE, microx_ip="192.168.1.235", microx_use_https=FALSE) {
  
  if("jsonlite" %in% rownames(installed.packages()) == FALSE) {install.packages("jsonlite")}
  suppressMessages(library(jsonlite))
  if("httr" %in% rownames(installed.packages()) == FALSE) {install.packages("httr")}
  suppressMessages(library(httr))
  
  # Get scenario data from the service URI.
  
  # Args:
  #   access_token (str): The access token for authorization.
  #   service_uri (str): The service URI for the request.
  #   use_snap4city (bool): Flag to determine whether to use the Snap4City URL. Default is TRUE.
  #   microx_ip (str): The IP address of the microx server. Default is "192.168.1.235".
  #   microx_use_https (bool): Flag to determine if HTTPS should be used for microx. Default is FALSE.       
  
  # Returns:
  #   dict: The JSON response containing the scenario data.
  
  if (use_snap4city) {
    url <- paste0(Sys.getenv("BASE_URL", unset = "https://www.snap4city.org"),"/superservicemap/api/v1/")
  } else {
    protocol <- if (microx_use_https) "https" else "http"
    url <- paste0(protocol, "://", microx_ip, "/superservicemap/api/v1/")
  }
  
  headers <- add_headers(Authorization = paste('Bearer', access_token))
  
  complete_url <- paste0(url, "?serviceUri=", service_uri)
  
  response <- try(GET(complete_url, headers))
  
  if (class(response) == "try-error") {
    stop("An error occurred during the HTTP request: ", response)
  }
  
  if (status_code(response) >= 400) {
    stop("HTTP request failed with status: ", status_code(response))
  }
  
  return(content(response, type="application/json"))
}

# Example usage:
# result <- get_scenario_device_data_from_suri(accesstoken, urlScenario, use_snap4city=FALSE)
# print(result)


get_graph_data_from_suri <- function(access_token, service_uri, use_snap4city=TRUE, microx_ip="192.168.1.235", microx_use_https=FALSE) {
  
  if("jsonlite" %in% rownames(installed.packages()) == FALSE) {install.packages("jsonlite")}
  suppressMessages(library(jsonlite))
  if("httr" %in% rownames(installed.packages()) == FALSE) {install.packages("httr")}
  suppressMessages(library(httr))
  
  
  # Costruisci l'URL base in base ai parametri forniti
  if (use_snap4city) {
    base_url <- Sys.getenv("BASE_URL", unset = "https://www.snap4city.org")
  } else {
    protocol <- ifelse(microx_use_https, "https", "http")
    base_url <- paste0(protocol, "://", microx_ip)
  }
  
  # Costruisci l'URL completo
  url <- paste0(base_url, "/processloader/api/bigdatafordevice/getOneSpecific.php?suri=", service_uri, "&accessToken=", access_token)
  
  # Definisci l'header di autorizzazione
  headers <- add_headers(Authorization = paste("Bearer", access_token))
  
  # Effettua la richiesta GET
  response <- tryCatch(
    {
      res <- GET(url, headers)
      stop_for_status(res)  # Solleva un'eccezione per errori HTTP
      content(res, type="application/json")
    },
    error = function(e) {
      stop("An error occurred during the HTTP request: ", e$message)
    }
  )
  
  return(response)
}

send_data_to_device <- function(broker, device_name, device_type, payload, access_token, use_snap4city = TRUE, microx_ip = "192.168.X.X", microx_use_https = FALSE) {
  # Load necessary libraries
  if("jsonlite" %in% rownames(installed.packages()) == FALSE) {install.packages("jsonlite")}
  suppressMessages(library(jsonlite))
  if("httr" %in% rownames(installed.packages()) == FALSE) {install.packages("httr")}
  suppressMessages(library(httr))
  
  # Construct the URL based on the input parameters
  if (use_snap4city) {
    url <- sprintf("%s/%s/v2/entities/%s/attrs?elementid=%s&type=%s", 
                   Sys.getenv("ORIONFILTER_BASE_URL", unset = "https://www.snap4city.org/orionfilter"), broker, device_name, device_name, device_type)
  } else {
    protocol <- ifelse(microx_use_https, "https", "http")
    url <- sprintf("%s://%s/orion-filter/%s/v2/entities/%s/attrs?elementid=%s&type=%s", 
                   protocol, microx_ip, broker, device_name, device_name, device_type)
  }
  
  # Define the headers
  headers <- add_headers(
    "Content-Type" = "application/json",
    "Accept" = "application/json",
    "Authorization" = paste("Bearer", access_token)
  )
  
  # Convert payload to JSON format
  #payload_json <- toJSON(payload, auto_unbox = TRUE)
  
  # Perform the PATCH request
  response <- tryCatch(
    {
      PATCH(url, body = payload, encode = "json", headers)
    },
    error = function(e) {
      stop(sprintf("An error occurred during the HTTP request: %s", e$message))
    }
  )
  
  # Check for HTTP errors
  if (http_status(response)$category != "Success") {
    stop(sprintf("An error occurred during the HTTP request: %s", http_status(response)$message))
  }
  
  return(response$status_code)
}


send_graph_data_to_db <- function(access_token, service_uri, payload, use_snap4city = TRUE, microx_ip = "192.168.X.X", microx_use_https = FALSE, dateObserved = "") {
  
  # Funzione per inviare dati al dispositivo specificato
  
  # Args:
  # access_token (str): Il token di accesso per l'autorizzazione.
  # service_uri (str): L'URI del servizio per la richiesta.
  # payload (str): Il payload da inviare al dispositivo. Deve essere una STRINGA json e.g. '{"grandidati":{"roadGraph":[{"road":"http://www.d'...
  # use_snap4city (bool): Flag per usare l'URL Snap4City o un IP microx personalizzato.
  # microx_ip (str): L'indirizzo IP del server microx.
  # microx_use_https (bool): Flag per determinare se utilizzare HTTPS per microx.
  # dateObserved (str): la data osservata che deve essere nel formato '%Y-%m-%d %H:%M:%S'
  
  # Returns:
  # str: Il messaggio della risposta HTTP.
  
  # Raises:
  # stop: Se si verifica un errore durante la richiesta HTTP.
  
  if (dateObserved == "") {
    dateObserved <- format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  } else {
    tryCatch({
      as.POSIXct(dateObserved, format = "%Y-%m-%d %H:%M:%S")
    }, error = function(e) {
      message("dateObserved non Ã¨ nel formato corretto ('%Y-%m-%d %H:%M:%S')")
      message("aggiornato all'ora corrente")
      dateObserved <- format(Sys.time(), "%Y-%m-%d %H:%M:%S")
    })
  }
  
  if (use_snap4city) {
    base_url <- Sys.getenv("BASE_URL", unset = "https://www.snap4city.org")
  } else {
    protocol <- if (microx_use_https) "https" else "http"
    base_url <- sprintf("%s://%s", protocol, microx_ip)
  }
  
  url <- sprintf("%s/processloader/api/bigdatafordevice/postIt.php?suri=%s&accessToken=%s&dateObserved=%s", base_url, service_uri, access_token, dateObserved)
  
  headers <- c(
    Authorization = sprintf('Bearer %s', access_token)
  )
  
  response <- POST(url, body = payload, add_headers("Content-Type" = "application/json","Authorization" = headers))
  
  if (http_error(response)) {
    stop(sprintf("Si Ã¨ verificato un errore durante la richiesta HTTP: %s", httr::http_status(response)$message))
  }
  
  return(response$status_code)
}

#' @get /tfr
#' @serializer unboxedJSON

tfr <- function(SURI,Token,DateObserved){
  print("chiamato ricostruttore")
  #------------------------------------------------
  #install.packages
  #------------------------------------------------
  if("jsonlite" %in% rownames(installed.packages()) == FALSE) {install.packages("jsonlite")}
  suppressMessages(library(jsonlite))
  if("httr" %in% rownames(installed.packages()) == FALSE) {install.packages("httr")}
  suppressMessages(library(httr))
  if("tictoc" %in% rownames(installed.packages()) == FALSE) {install.packages("tictoc")}
  suppressMessages(library(tictoc))
  if("stringr" %in% rownames(installed.packages()) == FALSE) {install.packages("stringr")}
  suppressMessages(library(stringr))
  if("RCurl" %in% rownames(installed.packages()) == FALSE) {install.packages("RCurl")}
  suppressMessages(library(RCurl))
  if("varhandle" %in% rownames(installed.packages()) == FALSE) {install.packages("varhandle")}
  suppressMessages(library(varhandle))
  if("sgeostat" %in% rownames(installed.packages()) == FALSE) {install.packages("sgeostat")}
  suppressMessages(library(sgeostat))
  if("geosphere" %in% rownames(installed.packages()) == FALSE) {install.packages("geosphere")}
  suppressMessages(library(geosphere))
  if("rlist" %in% rownames(installed.packages()) == FALSE) {install.packages("rlist")}
  suppressMessages(library(rlist))
  
  options(digits=10)
  ######################################################################################
  #Get the Scenario X in status==TDM at DateObserved==DT
  
  resultScenario=get_scenario_device_data_from_suri(Token,SURI)
  
  ######################################################################################
  
  #dataJson
  dataJson=resultScenario$realtime$results$bindings[[1]]
  
  if(is.null(dataJson)){
    
    return("KO - error GET data, Data is NULL")
    
  }
  else{
    
    ######################################################################################
    #Get GRAPH of Scenario X in status==TDM at DateObserved==DT
    
    resultGraph=get_graph_data_from_suri(Token,SURI)
    
    DTseries=c()
    
    for(i in 1:length(resultGraph)){
      
      DTseries=c(DTseries,resultGraph[[i]]$dateObserved)
      
    }
    
    DTindex=which(DTseries==DateObserved)
    
    if(length(DTindex)!=1){
      return("KO - DateObserved is not correct")
    }
    
    ######################################################################################
    
    options(digits=10)
    
    #parsing:
    areaOfInterest=dataJson$areaOfInterest$value
    
    dateObserved=dataJson$dateObserved$value
    
    description=dataJson$description$value
    
    startTime=dataJson$startTime$value
    
    endTime=dataJson$endTime$value
    
    locality=dataJson$location$value
    
    measuredTime=dataJson$measuredTime$value
    
    modality=dataJson$modality$value
    
    scenario_name=dataJson$name$value
    
    referenceKB=dataJson$referenceKB$value
    
    sourceData=dataJson$sourceData$value
    
    status=dataJson$status$value
    
    #FROM DB
    options(digits=12)
    dataDB=fromJSON(resultGraph[[DTindex]]$data)
    
    roadGraph=dataDB$grandidati$roadGraph
    
    if(is.null(roadGraph)){
      return("KO - roadGraph is not present")
    }
    
    TDM=dataDB$grandidati$TDM
    
    if(is.null(TDM)){
      return("KO - TDM is not present")
    }
    
    JS20=dataDB$grandidati$JS20
    if(is.null(JS20)){
      return("KO - JS20 is not present")
    }
    
    
    sensors=dataDB$grandidati$sensors
    
    
    if(is.null(sensors)){
      return("KO - data sensors is not present")
    }
    
    turnRestric=dataDB$grandidati$restrictions
    #:::::::::::::::::::::::::::
    
    indexCheck=0
    
    options(digits=10)
    
    messageError=c("ERROR: some features are not considered",
                   "ERROR: scenarioName must be a character",
                   "ERROR: DatetimeStart must be more recent with respect to DatetimeEnd",
                   "ERROR: at least 1 hour must be occurred from DatetimeStart to DatetimeEnd"
    )
    
    #scenarioDimension
    if(length(dataJson)>0){
      
      #scenarioName
      if((is.character(scenario_name)==TRUE)){
        
        timeStart=strptime(startTime,"%Y-%m-%dT%H:%M")
        timeEnd=strptime(endTime,"%Y-%m-%dT%H:%M")
        
        #DateTime
        if(timeStart<timeEnd){
          
          print("rounding of the hourly rate")
          timeStartRound=round.POSIXt(timeStart,units = "hours")
          timeEndRound=round.POSIXt(timeEnd,units = "hours")
          
          numHours=as.numeric(difftime(timeEndRound,timeStartRound,units = "hours"))
          
          #numberOfHour
          #if(numHours==0){indexCheck=4}
        }else{indexCheck=3}
      }else{indexCheck=2}
    }else{indexCheck=1}
    
    
    if(indexCheck==0){
      print("INPUT PARAMETERS ARE CORRECT")
      
      
      #head
      base_url= Sys.getenv("BASE_URL", unset = "https://www.snap4city.org") #parameter
      
      access_token=Token
      
      broker=resultScenario$Service$features[[1]]$properties$brokerName
      
      organization=resultScenario$Service$features[[1]]$properties$organization
      
      baseSURI="http://www.disit.org/km4city/resource/iot/" #da estrarre da SURI - meglio come parameter 
      
      
      #metadata
      # fluxName=scenario_name
      scenarioDT=DateObserved
      timestamp_dev = format(Sys.time(), tz = "UTC", "%Y-%m-%dT%H:%M:%OS3Z")
      timestamp <- sub("Z$", "", timestamp_dev)
      timestamp <- sub("\\.[0-9]+", "", timestamp)
      timestamp <- sub(":", "-", timestamp)
      timestamp <- sub(":", "-", timestamp)
      # timestamp = format(Sys.time(), "%Y-%m-%dT%H-%M-%OS")

      fluxName <- paste(scenario_name, timestamp, sep = "_")
      
      locality=locality
      
      organization=organization
      
      scenarioID= fluxName 
      
      scenarioSURI=SURI
      
      
      
      duration="60min"
      
      metricName="TrafficDensity"
      
      unitOfMeasure="vehicle per 20m"
      
      colorMap="densityTrafficMap"
      
      staticGraphName=paste(fluxName,"StaticGraph",sep="")
      
      
      #TIME LIST:
      timeList=c()
      timeSavedList=c()
      for(i in 1:(numHours+1)){
        
        temp=timeStartRound+(i-1)*3600
        
        href=substr(temp,12,19)
        if(href==""){
          href="00:00:00"
        }
        timeList=c(timeList,paste(substr(temp,1,10),href,sep="T"))
        
      }
      
      timeSavedList=paste(substr(timeList,1,4),
                          substr(timeList,6,7),
                          substr(timeList,9,10),
                          "T",
                          substr(timeList,12,13),
                          substr(timeList,15,16),
                          substr(timeList,18,19),
                          sep="")
      
      #GET START FOR INITIAL CONDITIONS
      #getDAY
      day=as.numeric(format(timeStart, "%w"))
      # 0 - Sunday..6 - Saturday
      daysel=day
      
      if(daysel==0){
        daysel=7
      }
      
      timeHour=as.numeric(substr(timeStart,12,13))
      
      print("DateTime correctly inserted")
      
      nodesMergedSensors=as.data.frame(lapply(TDM, function(x) as.character(unlist(x))), stringsAsFactors = FALSE)
      
      indexWeigthNA=which(is.na(nodesMergedSensors[,"weight"]))
      
      if(length(indexWeigthNA)>0){
        nodesMergedSensors[indexWeigthNA,"weight"]=6
      }
      
      #controllo velocità
      indexVmax20=which(as.numeric(nodesMergedSensors[,"vmax"])>20)
      if(length(indexVmax20)>0){
        return(paste("KO - speed not admitted in ",nodesMergedSensors[indexVmax20,"realSegment"],sep=""))
      }

      #new sensor definition INV case:
      indexSensorINV=which(nodesMergedSensors[,"sensor"]!=0)
      for(i in 1:length(indexSensorINV)){
        indexInternalINV=which(nodesMergedSensors[,"realSegment"]==nodesMergedSensors[indexSensorINV[i],"realSegment"])
        indexInternalINV=indexInternalINV[-which(indexInternalINV==indexSensorINV[i])]
        if(length(indexInternalINV)>0){
          if(grepl("INV",nodesMergedSensors[indexInternalINV[1],"segment"])){
            nodesMergedSensors[indexSensorINV[i],"sensor"]=nodesMergedSensors[indexInternalINV,"segment"]
            nodesMergedSensors[indexSensorINV[i],"segment"]=nodesMergedSensors[indexInternalINV,"segment"]
            print(indexSensorINV[i])
          }
        }
      }
      
      # densit? critiche:
      
      dc_1=35          # car/km
      dc_2=70
      dc_3=105
      dc_4=140
      dc_5=175
      dc_6=210
      
      
      ##############################################################
      # PROCEDURA DI CREAZIONE LISTA DELLE STRADE DEL GRAFO - INIZIO
      
      #-------------------------------------------------------------
      
      #lista strade: inizializzazione
      roadList = unique(nodesMergedSensors[which(nodesMergedSensors[, "sensor"] == 0), "road"])
      roads <- vector("list", length(roadList))
      names(roads) <- roadList
      
      #-------------------------------------------------------------
      
      for(i in 1: length(roadList)){
        segmentsWithLength =  unique(nodesMergedSensors[which(nodesMergedSensors[,"road"] == roadList[i]), c("segment", "length", "lanes", "vmax", "delta_x","sensor")])
        sensorInput=segmentsWithLength[unique(which(segmentsWithLength[,"sensor"] != 0)),]
        segmentsWithLength=segmentsWithLength[unique(which(segmentsWithLength[,"sensor"] == 0)),]
        segmentsWithLength[which(segmentsWithLength[,"length"] == 0),"length"] = 1
        segmentsMatrix=matrix(rep(NA),5,dim(segmentsWithLength)[1])
        colnames(segmentsMatrix)<- segmentsWithLength[,"segment"]
        for(j in 1:dim(segmentsWithLength)[1]){
          if((as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))<1){
            
            if(segmentsWithLength[j,"lanes"]==1){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_1*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_1*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_1*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==2){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_2*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_2*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_2*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            } 
            else if(segmentsWithLength[j,"lanes"]==3){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_3*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_3*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_3*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==4){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_4*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_4*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_4*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==5){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_5*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_5*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_5*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==6){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_6*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_6*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_6*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            
          }else{
            
            if(segmentsWithLength[j,"lanes"]==1){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_1*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_1*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_1*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==2){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_2*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_2*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_2*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            } 
            else if(segmentsWithLength[j,"lanes"]==3){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_3*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_3*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_3*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==4){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_4*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_4*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_4*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==5){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_5*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_5*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_5*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            else if(segmentsWithLength[j,"lanes"]==6){
              segmentsMatrix[,segmentsWithLength[j,"segment"]] = matrix(c(as.numeric(segmentsWithLength[j,"length"]),ceiling(as.numeric(segmentsWithLength[j,"length"])/as.numeric(segmentsWithLength[j,"delta_x"]))+1,dc_6*as.numeric(segmentsWithLength[j,"delta_x"])/1000,2*dc_6*as.numeric(segmentsWithLength[j,"delta_x"])/1000,as.numeric(segmentsWithLength[j,"vmax"])/2*(dc_6*as.numeric(segmentsWithLength[j,"delta_x"])/1000)),5)
            }
            
          }
          
          
        }
        
        r_iniziali=rep(0.001, dim(segmentsWithLength)[1])
        
        # inizializzazione lista densit? e lista approssimazioni
        
        densityList=list()
        
        approxList=list()
        
        fluxaList=list()
        
        fluxbList=list()
        
        for (k in 1:dim(segmentsMatrix)[2]){
          
          densityList[[colnames(segmentsMatrix)[k]]]=matrix(rep(r_iniziali[k],segmentsMatrix[2,k]),1,segmentsMatrix[2,k])      #prima riga con condizioni iniziali
          
          approxList[[colnames(segmentsMatrix)[k]]]=matrix(rep(NA,segmentsMatrix[2,k]),1,segmentsMatrix[2,k])                        #inizializzazione lista approssimazioni 
          
          fluxaList[[colnames(segmentsMatrix)[k]]]=NA
          
          fluxbList[[colnames(segmentsMatrix)[k]]]=NA
        }
        
        roads[[roadList[i]]] = list(segments = segmentsMatrix, density=densityList, approx=approxList, fluxa=fluxaList, fluxb=fluxbList)
        
        roads[[roadList[i]]]$sensorInput=sensorInput
      }
      
      tic()
      for(k in 1:length(roads)){
        for(z in 1:dim(roads[[k]]$segments)[2]){
          roads[[k]]$lanes[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "lanes"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
          roads[[k]]$weight[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "weight"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
          roads[[k]]$vmax[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "vmax"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
          roads[[k]]$deltax[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "delta_x"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
        }
      }
      toc()
      # PROCEDURA DI CREAZIONE LISTA DELLE STRADE DEL GRAFO - FINE
      ##############################################################
      
      ##############################################################
      #ACQUISIZIONE SENSORI
      
      #ESTRAZIONE SENSORI::
      indexSensorsBig=which(nodesMergedSensors[,"sensor"]!=0)
      
      #ATTENZIONE: adesso i sensori si considerano puntuali (nodeA=nodeB) per le logiche INPUT - OUTPUT:
      #in questo modo i sensori VIRTUALI E REALI non si confondono con quelli di INPUT/OUTPUT
      for(i in 1:length(indexSensorsBig)){
        nodesMergedSensors[indexSensorsBig[i],"nodeB"]=nodesMergedSensors[indexSensorsBig[i],"nodeA"]
        nodesMergedSensors[indexSensorsBig[i],"nBLat"]=nodesMergedSensors[indexSensorsBig[i],"nALat"]
        nodesMergedSensors[indexSensorsBig[i],"nBLong"]=nodesMergedSensors[indexSensorsBig[i],"nALong"]
      }
      
      sensorsBig=nodesMergedSensors[indexSensorsBig,]
      
      #REAL SENSORS
      realSensors=sensorsBig
      
      #################################################
      #DISTRIBUZIONI SUGLI INCROCI : RANDOM
      #################################################
      
      #################################################
      #STRUTTURE PER VALIDAZIONE OMESSE PER IL MOMENTO#
      #################################################
      
      delta_t=1
      
      
      print("START RECONSTRUCTION PROCEDURE")
      
      #OLD TrafficFLowManager
      
      #VISUALIZATION: TRAFFIC MANAGER _ STATIC GRAPH
      dataGraph=list()
      listAttribGraph=list("nameGraphID"=list("staticGraphName"=staticGraphName),
                           "dataGraph"=JS20)
      
      dataGraph[[1]]=listAttribGraph
      
      request_body_json <- toJSON(dataGraph[[1]], auto_unbox = TRUE,digits=10)
      
      ###POST SECTION####
      resultPOST <- POST(url = paste0(Sys.getenv("BASE_URL", unset = "http://wmsserver.snap4city.org"),"/trafficflowmanager/api/upload?type=staticGraph"),
                         body = request_body_json,
                         encode = "json", add_headers("Content-Type" = "application/json"), config(ssl_verifypeer=0))
      
      outputFTot=list()
      
      TFmanagerMess=c()
      #RECONSTRUCTION STEPS
      for(indexStep in 1:(numHours+1)){ 
        
        tic()  
        
        ########################################################
        #DATE SELECTION:
        selDate=timeList[indexStep]
        standardDate=strptime(selDate,"%Y-%m-%dT%H:%M:%S")
        #getDAY
        day=as.numeric(format(standardDate, "%w"))
        # 0 - Sunday..6 - Saturday
        daysel=day
        
        if(daysel==0){
          daysel=7
        }
        
        messStart=paste("RUN RECONSTRUCTION METHOD FOR ",scenario_name," AT ",timeList[indexStep],sep="")
        
        print(messStart)
        
        #MISURAZIONE SENSORI E INSERIMENTO GRAFO: OMESSO
        
        timeHour=as.numeric(timeHour)
        
        #AGGIORNAMENTO dei PESI in ROADS che dovrebbe essere nel file TDM: OMESSO
        
        #DEMO RICOSTRUZIONE: ALGORITMO PARALLELO: OMESSO
        
        #DEMO - DEMO - DEMO
        
        #Inserimento dei valori di density all'interno delle roads: RANDOM
        countdG=0
        for(k in 1:length(roads)){
          for(z in 1:length(roads[[k]]$density)){
            for(y in 1:as.numeric(roads[[k]]$segments[2,z])){
              countdG=countdG+1
              
              roads[[k]]$density[[z]][1,y]=runif(1, min = 0.01, max = 4.01)
            }
          }
        }
        
        print("COMPUTATION - COMPLETED")
        ###########################
        #JSON DINAMICO SE MODIFICATO
        ###########################
        
        # TRAFFIC MANAGEMENT SYSTEMS STANDARD
        options(digits=4)
        tic()
        indexDB=1
        roadsToJsonDensity <- list()
        for (i in 1:length(roads)){
          density <- vector("list", 1)
          roadDensity = list()
          segmentsNames = as.character(colnames(roads[[i]]$segments))
          #####################
          #realName
          realName=c()
          for(r in 1:length(segmentsNames)){
            indexReal=which(nodesMergedSensors[,"segment"]==segmentsNames[r] & nodesMergedSensors[,"sensor"]==0)
            realName=c(realName,
                       nodesMergedSensors[indexReal,"realSegment"])
          }
          #####################
          for (l in 1: length(segmentsNames)){
            tempDensity = roads[[i]]$density[[segmentsNames[l]]]
            vf=as.numeric(roads[[i]]$vmax[[segmentsNames[l]]])
            dmax=as.numeric(roads[[i]]$segments[4,l])
            deltax=as.numeric(roads[[i]]$deltax[[segmentsNames[l]]])
            colnames(tempDensity)<-paste(realName[l], seq(1:(dim(tempDensity)[2])), sep=".")
            for(h in 1: (dim(tempDensity)[2]-1)){
              roadDensity[[colnames(tempDensity)[h]]] = unbox(as.character(tempDensity[1,h]))
              
              # vel=as.numeric(vf)*(1-tempDensity[h]/as.numeric(dmax))
              # if(vel<=0){
              #   time=100
              # }else{
              #   time=as.numeric(deltax)/vel
              # }
              
              indexDB=indexDB+1
              
            }
          }
          density[[1]] = roadDensity
          roadsToJsonDensity[[unbox(names(roads[i]))]] = list(data = density);
          
        }
        #jsonDynamic = toJSON(roadsToJsonDensity)
        toc()
        
        
        ###########################################################################
        
        
        #head
        base_url= Sys.getenv("BASE_URL", unset = "https://www.snap4city.org")
        
        access_token=Token
        
        broker=resultScenario$Service$features[[1]]$properties$brokerName
        
        organization=resultScenario$Service$features[[1]]$properties$organization
        
        baseSURI="http://www.disit.org/km4city/resource/iot/"
        
        
        #metadata
        # fluxName=scenario_name
        
        locality=locality
        
        organization=organization
        
        scenarioID=fluxName
        
        scenarioSURI=SURI
        
        scenarioDT=DateObserved
        
        
        
        duration="60min"
        
        metricName="TrafficDensity"
        
        unitOfMeasure="vehicle per 20m"
        
        colorMap="densityTrafficMap"
        
        staticGraphName=paste(fluxName,"StaticGraph",sep="")
        
        ###############TRAFFIC_MANAGER#######################
        #data .con ingestion:
        toDateTime=gsub("-", "", selDate)
        toDateTimeBis=gsub(":", "", toDateTime)
        #####################################################
        
        metadataTraffic=list()
        listAttribTemp=list("fluxName"=fluxName,
                            "locality"=locality,
                            "organization"=organization,
                            "scenarioID"=scenarioID,
                            "dateTime"=selDate,
                            "duration"=duration,
                            "metricName"=metricName,
                            "unitOfMeasure"=unitOfMeasure,
                            "colorMap"=colorMap,
                            "staticGraphName"=staticGraphName)
        
        metadataTraffic[[1]]=listAttribTemp
        
        dataMerged=list()
        
        listAttribFinal=list("metadata"=metadataTraffic[[1]],
                             "reconstructionData"=roadsToJsonDensity)
        
        dataMerged[[1]]=listAttribFinal
        
        jsonDynamicGraph <- toJSON(dataMerged[[1]], auto_unbox = TRUE,digits=10)
        
        #############################################################################
        #VISUALIZATION: TRAFFIC FLOW MANAGER
        request_body_json <- jsonDynamicGraph
        
        ###############TRAFFIC_MANAGER#######################
        metadataTraffic=list()
        listAttribTemp=list("base_url"=base_url,
                            "access_token"=access_token,
                            "broker"=broker,
                            "organization"=organization,
                            "baseSURI"=baseSURI,
                            "fluxName"=fluxName,
                            "locality"=locality,
                            "scenarioID"=scenarioID,
                            "scenarioSURI"=scenarioSURI,
                            "scenarioDT"=scenarioDT,
                            "dateTime"=selDate,
                            "duration"=duration,
                            "metricName"=metricName,
                            "unitOfMeasure"=unitOfMeasure,
                            "colorMap"=colorMap,
                            "staticGraphName"=staticGraphName)
        
        metadataTraffic[[1]]=listAttribTemp
        
        
        
        
        dataMerged=list()
        
        listAttribFinal=list("metadata"=metadataTraffic[[1]],
                             "reconstructionData"=roadsToJsonDensity)
        
        dataMerged[[1]]=listAttribFinal
        
        jsonDynamicGraph <- toJSON(dataMerged[[1]], auto_unbox = TRUE,digits=10)
        
        #############################################################################
        #VISUALIZATION: TRAFFIC FLOW MANAGER
        request_body_json <- jsonDynamicGraph
        
        ####POST SECTION NEW NEW OPENSEARCH####
        urlPOST <- paste0(Sys.getenv("BASE_URL", unset = "https://wmsserver.snap4city.org"),"/trafficflowmanager/api/upload?type=reconstruction") 
        resultPOST <- POST(url = urlPOST,
                           body = request_body_json,
                           encode = "json", add_headers("Content-Type" = "application/json"),
                           config(ssl_verifypeer=0))

        TFmanagerMess=c(TFmanagerMess,paste(timeList[indexStep], "TFR Manager status:", resultPOST$status_code, sep=" "))
        print(TFmanagerMess)
        
        print(content(resultPOST, "text"))
        
       ############################################################################
        
        toc()
        
      } 
      print("END RECONSTRUCTION PROCEDURE")
      
      outFinal=toJSON(outputFTot,pretty=TRUE,auto_unbox=TRUE, digits = 20)
      
      #return(outFinal)
      device_name=resultScenario$Service$features[[1]]$properties$name
      
      broker=resultScenario$Service$features[[1]]$properties$brokerName
      
      device_type="scenario"
      
      # timestamp = format(Sys.time(), "%Y-%m-%d%H:%M:%OS")
      
      payload = toJSON(list(
        dateObserved=list(
          type=unbox("string"),
          value=unbox(timestamp_dev)
        ),
        status=list(
          type=unbox("string"),
          value=unbox("tfr")
        ),fatherDateObserved=list(
          type=unbox("string"),
          value=unbox(DateObserved)
        )
      ))
      
      send_data_to_device(broker, device_name, device_type, payload, Token)
      
      finalResult=list(
        status="OK",
        date=timestamp_dev,
        additional_info=TFmanagerMess
      )
      
      
      # Restituiamo il JSON
      return(toJSON(finalResult, auto_unbox = TRUE))
      
      #return(append(paste("OK", timestamp_dev, sep=" "), TFmanagerMess))
      
    }
    else{
      return(paste("KO - ",messageError[indexCheck],sep=""))
    }
  }
  
}
