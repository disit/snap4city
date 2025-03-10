#   Computing ACC service.
#   Copyright (C) 2024 DISIT Lab https://www.disit.org - University of Florence
#
#   This program is free software: you can redistribute it and/or modify
#   it under the terms of the GNU Affero General Public License as
#   published by the Free Software Foundation, either version 3 of the
#   License, or (at your option) any later version.
#   This program is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU Affero General Public License for more details.
#   You should have received a copy of the GNU Affero General Public License
#   along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
    stop("HTTP request failed with status: ", status_code(response), complete_url)
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
    dateObserved <- format(Sys.time(), tz = "UTC","%Y-%m-%d %H:%M:%S")
  }
  
  # URL encode del dateObserved
  dateObserved <- URLencode(dateObserved)
  
  if (use_snap4city) {
    base_url <- Sys.getenv("BASE_URL", unset = "https://www.snap4city.org")
  } else {
    protocol <- if (microx_use_https) "https" else "http"
    base_url <- sprintf("%s://%s", protocol, microx_ip)
  }
  
  #url <- sprintf("%s/processloader/api/bigdatafordevice/postIt.php?suri=%s&accessToken=%s&dateObserved=%s", base_url, service_uri, access_token, dateObserved)
  
  url <- sprintf("%s/processloader/api/bigdatafordevice/postIt.php?suri=%s&dateObserved=%s&accessToken=%s", base_url, service_uri, dateObserved, access_token)
  print("INVIO REQ INSERIMENTO NEL MYSQL")
  print(url)
  headers <- c(
    Authorization = sprintf('Bearer %s', access_token)
  )
  
  response <- POST(url, body = payload, add_headers("Content-Type" = "application/json","Authorization" = headers))
  
  if (http_error(response)) {
    stop(sprintf("Si è verificato un errore durante la richiesta HTTP: %s", httr::http_status(response)$message))
  }
  
  return(response$status_code)
}

#' @get /acc
#' @serializer unboxedJSON

acc <- function(SURI,Token,DateObserved){
  print("PROCESSING")

  print(SURI)
  
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
  
  ######################################################################################
  #FUNZ
  #Get the Scenario X in status==INIT at DateObserved==DT
  
  resultScenario=get_scenario_device_data_from_suri(Token,SURI)
  
  ######################################################################################
  
  #dataJson
  dataJson=resultScenario$realtime$results$bindings[[1]]
  
  if(is.null(dataJson)){
    
    return("KO - error GET data, Data is NULL")
    
  }
  else{
    
    ######################################################################################
    #FUN: 
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
    #serviceUri=serviceUri
    
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
    
    # if(status!="tdm"){
    #   return("KO - status is not TDM")
    # }
    
    #FROM DB
    options(digits=12)
    dataDB=fromJSON(resultGraph[[DTindex]]$data)
    
    roadGraph=dataDB$grandidati$roadGraph
    
    if(is.null(roadGraph)){
      return("KO - roadGraph is not present")
    }
    
    sensors=dataDB$grandidati$sensors
    if(is.null(sensors)){
      return("KO - sensors are not present")
    }
    
    indexCheck=0
    
    options(digits=10)
    
    messageError=c("ERROR: some features are not considered",
                   "ERROR: scenarioName must be a character",
                   "ERROR: DatetimeStart must be more recent with respect to DatetimeEnd",
                   "ERROR: at least 1 hour must be occurred from DatetimeStart to DatetimeEnd"
    )
    
    #scenarioDimension
    if(length(dataJson)<=18){
      
      #scenarioName
      if((is.character(scenario_name)==TRUE)){
        
        fromDateTime=startTime
        toDateTime=endTime
        
        timeStart=strptime(fromDateTime,"%Y-%m-%dT%H:%M")
        timeEnd=strptime(toDateTime,"%Y-%m-%dT%H:%M")
        
        #DateTime
        if(timeStart<timeEnd){
          
          print("rounding of the hourly rate")
          timeStartRound=round.POSIXt(timeStart,units = "hours")
          timeEndRound=round.POSIXt(timeEnd,units = "hours")
          # #numberOfHour
          # numHours=as.numeric(difftime(timeEndRound,timeStartRound,units = "hours"))
          # if(numHours==0){indexCheck=4}
        }else{indexCheck=3}
      }else{indexCheck=2}
    }
    else{indexCheck=1}
    
    #DATA PARAMETERS OK
    if(indexCheck==0){
      
      print("INPUT PARAMETERS ARE CORRECT")
      
      # ROADGRAPH
      # nodesMergedSensors Creation:
      nodesMerged=data.frame(roadGraph)
      
      # Si riducono le stringhe dei nomi evitando serviceUri path "http://www.disit.org/km4city/resource/" 
      nodesMerged[,"road"]=substr(nodesMerged[,"road"],39,60)
      nodesMerged[,"segment"]=substr(nodesMerged[,"segment"],39,60)
      nodesMerged[,"nodeA"]=substr(nodesMerged[,"nodeA"],39,60)
      nodesMerged[,"nodeB"]=substr(nodesMerged[,"nodeB"],39,60)
      nodesMerged[,"sensor"]=0  
      nodesMerged[,"considered"]=0
      nodesMerged[,"virtual"]=0
      #
      
      #FILTERING
      #solo strade veicolabili/carreggiabili 
      indexMotorway=grep("motorway",nodesMerged[,"type"])
      indexTrunk=grep("trunk",nodesMerged[,"type"])
      indexPrimary=grep("primary",nodesMerged[,"type"])
      indexSecondary=grep("secondary",nodesMerged[,"type"])
      indexTertiary=grep("tertiary",nodesMerged[,"type"])
      indexUnclassified=grep("unclassified",nodesMerged[,"type"])
      indexResidential=grep("residential",nodesMerged[,"type"])
      
      carStreetIndex=c(indexMotorway,
                  indexTrunk,
                  indexPrimary,
                  indexSecondary,
                  indexTertiary,
                  indexUnclassified,
                  indexResidential)
      
      nodesMerged=nodesMerged[carStreetIndex,]
      
      #esclusione strade contenenti il pattern "chiuso" in dir:
      indexClose=grep("chiuso",nodesMerged[,"dir"])
      
      if(length(indexClose)>0){
        nodesMerged=nodesMerged[-indexClose,]
      }
      
      
      
      indexEntrambe=grep("entrambe",nodesMerged[,"dir"])
      
      if(length(indexEntrambe)>0){
        nodesMerged=nodesMerged[-indexEntrambe,]
      }
      
      ###########provvisorio
      #########segmenti univoci:##############
      uniqueSeg=unique(nodesMerged[,"segment"])
      delSeg=c()
      for(i in 1:length(uniqueSeg)){
        index=which(nodesMerged[,"segment"]==uniqueSeg[i])
        if(length(index)>1){
          delSeg=c(delSeg,index[-1])
        }
      }
      
      if(length(delSeg)>0){
        nodesMerged=nodesMerged[-delSeg,]
      }
      
      #######################
      
      
      nodesMergedOrigin=nodesMerged
      nodesMergedSensors=nodesMerged
      
      
      #Sensors:
      sensorsDataset=sensors
      
      #Sensors: esclusione strade contenenti il pattern "chiuso" in dir:
      indexCloseS=grep("chiuso",sensorsDataset$nearestRoad[,"dir"])
      
      if(length(indexCloseS)>0){
        sensorsDataset=sensorsDataset[-indexCloseS,]
      }
      
      #FILTERING
      
      #sensori considered TRUE
      indexTRUE=which(sensorsDataset[,"considered"]=="TRUE")
      if(length(indexTRUE)==0){
        return("KO - sensors are not considered")
      }
      
      if(length(indexTRUE)>0){
        sensorsDataset=sensorsDataset[indexTRUE,]
      }
      
      #OFF - esclusi
      #indexStatusOFF=which(sensorsDataset[,"status"]=="off")
      #
      #che non contengono METRO
      #
      indexStatusOFF=which(sensorsDataset[,"status"]=="off")
      if(length(indexStatusOFF)>0){
        sensorsDataset=sensorsDataset[-indexStatusOFF,]
      }
      
      #SENSORI INTERNI
      indexSensInternal=which(sensorsDataset[,"trafficSensor"]!="")
      
      #SENSORI AL BORDO
      indexSensMotorway=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="motorway" & sensorsDataset[,"trafficSensor"]=="")
      indexSensTrunk=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="trunk" & sensorsDataset[,"trafficSensor"]=="")
      indexSensPrimary=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="primary" & sensorsDataset[,"trafficSensor"]=="")
      indexSensSecondary=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="secondary" & sensorsDataset[,"trafficSensor"]=="")
      indexSensTertiary=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="tertiary" & sensorsDataset[,"trafficSensor"]=="")
      indexSensUnclassified=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="unclassified" & sensorsDataset[,"trafficSensor"]=="")
      indexSensResidential=which(sensorsDataset[,"tipo_del_sensore_in_base_alla_strada"]=="residential" & sensorsDataset[,"trafficSensor"]=="")
      
      #total index validi
      carStreetSensIndex=c(indexSensInternal,
                           indexSensMotorway,
                       indexSensTrunk,
                       indexSensPrimary,
                       indexSensSecondary,
                       indexSensTertiary,
                       indexSensUnclassified,
                       indexSensResidential)
      
      
      sensorsDataset=sensorsDataset[carStreetSensIndex,]
      
      ##############
      #eclusione OUTFLOW tra sensori al bordo
      indexSensorOUTflow=which(sensorsDataset$direction=="OutFlow" & sensorsDataset$trafficDensity!="")
      if(length(indexSensorOUTflow)>0){
        sensorsDataset=sensorsDataset[-indexSensorOUTflow,]
      }
      
      indexSensorBIflow=which(sensorsDataset$direction=="Bidirectional" & sensorsDataset$trafficDensity!="")
      if(length(indexSensorBIflow)>0){
        sensorsDataset=sensorsDataset[-indexSensorBIflow,]
      }
      ##############
      
      if(dim(sensorsDataset)[1]==0){
        return("KO - no sensors after filtering")
      }

      ##############

      #UNIQUE SENSOR in a RoadElement:

      uniqueSensor=unique(sensorsDataset$nearestRoad$segment)

      delSensor=c()

      for(i in 1:length(uniqueSensor)){

        index=which(sensorsDataset$nearestRoad$segment==uniqueSensor[i])

        if(length(index)>1){

          delSensor=c(delSensor,index[-1])

        }

      }

      

      if(length(delSensor)>0){

        sensorsDataset=sensorsDataset[-delSensor,]

      }

      

      if(dim(sensorsDataset)[1]==0){

        return("KO - no sensors after filtering")

      }

      ############## SENSORS ARE OK
      
      sensorSegments=c()
      for(j in 1 : dim(sensorsDataset)[1]){
        segSensor=data.frame(matrix(NA,1,dim(nodesMergedSensors)[2]))
        colnames(segSensor)=colnames(nodesMergedSensors)
        
        segSensor$road=substr(sensorsDataset[j,"nearestRoad"][,"road"],39,60)
        segSensor$segment=substr(sensorsDataset[j,"nearestRoad"][,"segment"],39,60)
        segSensor$type=sensorsDataset[j,"nearestRoad"][,"type"]
        segSensor$nALat=sensorsDataset[j,"nearestRoad"][,"nALat"]
        segSensor$nALong=sensorsDataset[j,"nearestRoad"][,"nALong"]
        segSensor$nBLat=sensorsDataset[j,"nearestRoad"][,"nBLat"]
        segSensor$nBLong=sensorsDataset[j,"nearestRoad"][,"nBLong"]
        segSensor$dir="none" #dir: si considera "none" altrimenti incoerente durante sliptting
        segSensor$length=0 #length: si considera 0 per non contarlo 2 volte durante accorpamento
        segSensor$nodeA=substr(sensorsDataset[j,"nearestRoad"][,"nodeA"],39,60)
        segSensor$nodeB=substr(sensorsDataset[j,"nearestRoad"][,"nodeB"],39,60)
        segSensor$lanes=sensorsDataset[j,"nearestRoad"]["lanes"]
        segSensor$roadElmSpeedLimit=NA
        segSensor$elemType=sensorsDataset[j,"nearestRoad"]["elemType"]
        segSensor$operatingstatus=sensorsDataset[j,"nearestRoad"]["operatingstatus"]
        segSensor$id=sensorsDataset[j,"nearestRoad"]["id"]
        segSensor$sensor=substr(sensorsDataset[j,"sensorUri"],39,60)
        segSensor$considered=sensorsDataset[j,"considered"]
        segSensor$virtual=sensorsDataset[j,"virtual"]
        
        nodesMergedSensors=rbind(nodesMergedSensors,segSensor)
        sensorSegments=c(sensorSegments,substr(sensorsDataset[j,"nearestRoad"][,"segment"],39,60))
      }
      
      
      #########################################################################
      #I SEGMENTI CHE CONTENGONO I SENSORI NON VENGONO ACCORPATI PER IL MOMENTO
      #########################################################################
      
      #per i sensori puntuali si considera che il nodeA conincida con il nodeB
      
      # indexConsidered_noVirtual=which(nodesMergedSensors$considered==1 & nodesMergedSensors$virtual==0)
      # if(length(indexConsidered_noVirtual)>0){
      #   nodesMergedSensors[indexConsidered_noVirtual,"nodeB"]=nodesMergedSensors[indexConsidered_noVirtual,"nodeA"]
      # }
      
      
      print("STATIC GRAPH - CREATION")
      
      #identificazione dei Reciproci:
      
      nodesMergedSensorsRec=nodesMergedSensors
      
      nodesMergedSensorsRec[,"recSeg"]=0
      nodesMergedSensorsRec[,"recRoad"]=0
      RecSeg=matrix(NA,1,2)
      
      for(i in 1:dim(nodesMergedSensors)[1]){
        indexRec=which(nodesMergedSensors[i,"nodeA"]==nodesMergedSensors[,"nodeB"])
        indexRec2=which(nodesMergedSensors[indexRec,"nodeA"]==nodesMergedSensors[i,"nodeB"] & nodesMergedSensors[indexRec,"sensor"]==0)
        inR=indexRec[indexRec2]
        if(length(inR)==1){
          #print(i)
          nodesMergedSensorsRec[i,"recSeg"]=nodesMergedSensorsRec[inR,"segment"]
          nodesMergedSensorsRec[i,"recRoad"]=nodesMergedSensorsRec[inR,"road"]
          segR=substr(nodesMergedSensorsRec[i,"segment"],1,15)
          segI=substr(nodesMergedSensorsRec[inR,"segment"],1,15)
          if(length(which(RecSeg==segR))==0){
            RecSeg=rbind(RecSeg,c(segR,segI))
          }
        }
      }
      
      #eslusione delle starde con reciproco: dopo l'accorpamento si duplica inverso accorpato
      
      if(dim(RecSeg)[1]>1){
        #RecSeg=RecSeg[-1,]
        for(i in 2:dim(RecSeg)[1]){
          indX=grep(RecSeg[i,1],nodesMergedSensorsRec[,"segment"])
          #sensors:
          #non si escludono i reciproci che ammettono sensore
          if(length(indX)>0){
            indX1=c()
            for(j in 1:length(indX)){
              if(length(which(nodesMergedSensorsRec[indX[j],"segment"]==sensorSegments))>0){
                print(j)
                indX1=c(indX1,j)
              }
            }
            if(length(indX1)>0){
              #si escludono i reciproci che non ammettono sensore rispetto a indX1

              indX1c=indX[indX1]

              indX2=c()

              for(z in 1:length(indX1c)){

                partInd=which(nodesMergedSensorsRec[indX1c[z],"recSeg"]==nodesMergedSensorsRec[,"segment"] & nodesMergedSensorsRec[,"sensor"]==0)

                if(length(partInd)>0){

                  indX2=c(indX2,partInd)

                }

              }

              if(length(indX2)>0){

                indX=c(indX[-indX1],indX2)

              }else{

                indX=indX[-indX1]

              }
            }
          }
          if(length(indX)>0){
            nodesMergedSensorsRec=nodesMergedSensorsRec[-indX,]
          }
        }
      }
      nodesMergedSensorsOrigin=nodesMergedSensors
      nodesMergedSensors=nodesMergedSensorsRec
      
      # PROCEDURA DI SEMPLIFICAZIONE DEL GRAFO STRADALE:
      # Poich? il grafo stradale, a questo livello, ? composto da nodi del tipo 1-1 che connettono tratti interni di una stessa strada 
      # si determina l'argoritmo che consente la semplificazione del grafo in modo che i suddetti nodi vengano eleminati mantenendo la propriet?
      # di connessione tra i relativi incroci.
      # 
      # l'algoritmo prevede di controllare i nodi di end che sono unici. Una volta selezionato un nodo di end unico si controlla se questo ? unico
      # anche come nodo di start. Se ci? si verifica allora il nodo ? eliminabile e le sue propriet? vengono traslate sui nodi adiacenti.
      
      #CREARE UNA LISTA CHE TIENE CONTO DELLE COORDINATE DEI SINGOLI SEGMENTI ACCORPATI
      
      i = 1
      repeat{
        #print(i)
        #indice dei nodi di start che sono unici:
        indexNodesStart = which(!(duplicated(nodesMergedSensors$nodeA) | duplicated(nodesMergedSensors$nodeA, fromLast= TRUE)))
        #etichetta dei nodi di end che sono unici:
        endNodesSingle = nodesMergedSensors[which(!(duplicated(nodesMergedSensors$nodeB) | duplicated(nodesMergedSensors$nodeB, fromLast= TRUE))), "nodeB"]
        #indice dei nodi di end che sono unici:
        indexNodesEnd = which(!(duplicated(nodesMergedSensors$nodeB) | duplicated(nodesMergedSensors$nodeB, fromLast= TRUE)))
        merged = FALSE
        while (i <length(endNodesSingle)){
          index = which(endNodesSingle[i] == nodesMergedSensors[indexNodesStart, "nodeA"])
          #memoCoordMatrix=matrix(c(nodesMergedSensors[index,"nALat"],nodesMergedSensors[index,"nALong"],nodesMergedSensors[index,"nBLat"],nodesMergedSensors[index,"nBLong"],nodesMergedSensors[index,"length"]),1,5)
          
          if (length(index) > 0){
            
            #if((nodesMergedSensors[indexNodesEnd[i], "road"] == nodesMergedSensors[indexNodesStart[index], "road"]) && nodesMergedSensors[indexNodesEnd[i],"sensor"]==0){
            # se la strada ? la stessa le informazioni vengono fuse insieme:
            
            nodesMergedSensors[indexNodesEnd[i], "segment"] = paste(nodesMergedSensors[indexNodesEnd[i], "segment"], nodesMergedSensors[indexNodesStart[index], "segment"], sep="--")
            if(nodesMergedSensors[indexNodesStart[index], "recSeg"]!=0){
              nodesMergedSensors[indexNodesEnd[i], "recSeg"] = paste(nodesMergedSensors[indexNodesEnd[i], "recSeg"], nodesMergedSensors[indexNodesStart[index], "recSeg"], sep="--")
            }
            #nodesMerged[indexNodesEnd[i], "road"] = paste(nodesMerged[indexNodesEnd[i], "road"], nodesMerged[indexNodesStart[index], "road"], sep="--")
            #nodesMergedSensors[indexNodesEnd[i], "roadName"] = paste(nodesMergedSensors[indexNodesEnd[i], "roadName"], nodesMergedSensors[indexNodesStart[index], "roadName"], sep="--")
            if(!grepl(nodesMergedSensors[indexNodesEnd[i], "road"], nodesMergedSensors[indexNodesStart[index], "road"]) && !grepl(nodesMergedSensors[indexNodesStart[index], "road"], nodesMergedSensors[indexNodesEnd[i], "road"])){
              nodesMergedSensors[indexNodesEnd[i], "road"] = paste(nodesMergedSensors[indexNodesEnd[i], "road"], nodesMergedSensors[indexNodesStart[index], "road"], sep="--")
            } else {
              if (nchar(nodesMergedSensors[indexNodesStart[index], "road"]) > nchar(nodesMergedSensors[indexNodesEnd[i], "road"])){
                nodesMergedSensors[indexNodesEnd[i], "road"] = nodesMergedSensors[indexNodesStart[index], "road"]
              }
            }
            
            nodesMergedSensors[indexNodesEnd[i], "length"] = as.numeric(nodesMergedSensors[indexNodesEnd[i], "length"]) + as.numeric(nodesMergedSensors[indexNodesStart[index], "length"])
            nodesMergedSensors[indexNodesEnd[i], "nodeB"] = nodesMergedSensors[indexNodesStart[index], "nodeB"]
            #nodesMergedSensors[indexNodesEnd[i], "g2"] = nodesMergedSensors[indexNodesStart[index], "g2"]
            nodesMergedSensors[indexNodesEnd[i], "nBLat"] = nodesMergedSensors[indexNodesStart[index], "nBLat"]
            nodesMergedSensors[indexNodesEnd[i], "nBLong"] = nodesMergedSensors[indexNodesStart[index], "nBLong"]
            
            nodesMergedSensors = nodesMergedSensors[-indexNodesStart[index],]
            merged = TRUE
            
            break
            #}
            
          }
          
          i = i+1;
          
          
        }
        
        if (merged == FALSE){
          break
        }
        
      }
      
      # Si eliminano da nodesMergedSensors i segmenti stradali che hanno nodeA=nodeB. In pratica quelli che formano un anello o grafo chiuso.
      # ATTENZIONE: la procedura di cui sotto elimina tutti gli elementi di nodesMergedSensors nel caso in cui il grafo di riferimento ? ammette
      # un numero di vertici molto limitato e di conseguenza il grafo risultante ? chiuso
      #nodesMergedSensors = nodesMergedSensors[ -intersect(which(nodesMergedSensors[,"nodeA"] == nodesMergedSensors[,"nodeB"]), which(nodesMergedSensors[,"sensor"]==0)),]
      
      
      #inserimento di INV:
      indexINVrec=which(nodesMergedSensors[,"recSeg"]!=0 & nodesMergedSensors[,"sensor"]==0)
      
      if(length(indexINVrec)>0){
        for(i in 1:length(indexINVrec)){
          newINV=data.frame(matrix(NA,1,dim(nodesMergedSensors)[2]))
          colnames(newINV)=colnames(nodesMergedSensors)
          
          newINV$road=nodesMergedSensors[indexINVrec[i],"recRoad"]
          newINV$segment=paste(nodesMergedSensors[indexINVrec[i],"segment"],"INV",sep="")
          newINV$type=nodesMergedSensors[indexINVrec[i],"type"]
          newINV$nALat=nodesMergedSensors[indexINVrec[i],"nBLat"]
          newINV$nALong=nodesMergedSensors[indexINVrec[i],"nBLong"]
          newINV$nBLat=nodesMergedSensors[indexINVrec[i],"nALat"]
          newINV$nBLong=nodesMergedSensors[indexINVrec[i],"nALong"]
          newINV$dir=nodesMergedSensors[indexINVrec[i],"dir"]
          newINV$length=nodesMergedSensors[indexINVrec[i],"length"]
          newINV$nodeA=nodesMergedSensors[indexINVrec[i],"nodeB"]
          newINV$nodeB=nodesMergedSensors[indexINVrec[i],"nodeA"]
          newINV$lanes=nodesMergedSensors[indexINVrec[i],"lanes"]
          newINV$roadElmSpeedLimit=nodesMergedSensors[indexINVrec[i],"roadElmSpeedLimit"]
          newINV$elemType=nodesMergedSensors[indexINVrec[i],"elemType"]
          newINV$operatingstatus=nodesMergedSensors[indexINVrec[i],"operatingstatus"]
          newINV$id=nodesMergedSensors[indexINVrec[i],"id"]
          newINV$sensor=nodesMergedSensors[indexINVrec[i],"sensor"]
          newINV$considered=nodesMergedSensors[indexINVrec[i],"considered"]
          newINV$virtual=nodesMergedSensors[indexINVrec[i],"virtual"]
          newINV$recSeg=nodesMergedSensors[indexINVrec[i],"segment"]
          newINV$recRoad=nodesMergedSensors[indexINVrec[i],"road"]

          nodesMergedSensors=rbind(nodesMergedSensors,newINV)
        }
        
      }
      
      AccSeg=c()
      
      #REAL NAMES:
      for(i in 1:dim(nodesMergedSensors)[1]){
        if(grepl("INV",nodesMergedSensors[i,"segment"])){
          if(nodesMergedSensors[i,"sensor"]==0){
            AccSeg=c(AccSeg,nodesMergedSensors[i,"recSeg"])
          }
          
          noINVsegID=strsplit(nodesMergedSensors[i,"segment"],"INV")[[1]]
          nodesMergedSensors[i,"recSeg"]=noINVsegID
          indXX=which(nodesMergedSensors[,"segment"]==noINVsegID & nodesMergedSensors[,"sensor"]==0)
          nodesMergedSensors[i,"recRoad"]=nodesMergedSensors[indXX,"road"]
          nodesMergedSensors[i,"realSegment"]=nodesMergedSensors[indXX,"recSeg"]
          nodesMergedSensors[i,"realRoad"]=nodesMergedSensors[indXX,"recRoad"]
        }else{
          nodesMergedSensors[i,"realSegment"]=nodesMergedSensors[i,"segment"]
          nodesMergedSensors[i,"realRoad"]=nodesMergedSensors[i,"road"]
          if(nodesMergedSensors[i,"sensor"]==0){
            AccSeg=c(AccSeg,nodesMergedSensors[i,"segment"])
          }
        }
      }
      
      #AGGIUNGERE altre properties
      
      #default "weight":
      for(i in 1:dim(nodesMergedSensors)[1]){
        
        # sulle strade
        if(nodesMergedSensors[i,"type"]=="motorway"){
          nodesMergedSensors[i,"weight"]=100
        }
        
        else if(nodesMergedSensors[i,"type"]=="trunk"){
          nodesMergedSensors[i,"weight"]=80
        }
        
        else if(nodesMergedSensors[i,"type"]=="primary"){
          nodesMergedSensors[i,"weight"]=70
        }
        
        else if(nodesMergedSensors[i,"type"]=="secondary"){
          nodesMergedSensors[i,"weight"]=40
        }
        
        else if(nodesMergedSensors[i,"type"]=="tertiary"){
          nodesMergedSensors[i,"weight"]=20
        }
        
        else if(nodesMergedSensors[i,"type"]=="unclassified"){
          nodesMergedSensors[i,"weight"]=12
        }
        
        else if(nodesMergedSensors[i,"type"]=="residential"){
          nodesMergedSensors[i,"weight"]=12
        }
        
        
        #strade di collegamento:
        
        else if(nodesMergedSensors[i,"type"]=="motorway_link"){
          nodesMergedSensors[i,"weight"]=100
        }
        else if(nodesMergedSensors[i,"type"]=="trunk_link"){
          nodesMergedSensors[i,"weight"]=80
        }
        
        else if(nodesMergedSensors[i,"type"]=="primary_link"){
          nodesMergedSensors[i,"weight"]=70
        }
        
        else if(nodesMergedSensors[i,"type"]=="secondary_link"){
          nodesMergedSensors[i,"weight"]=40
        }
        
        else if(nodesMergedSensors[i,"type"]=="tertiary_link"){
          nodesMergedSensors[i,"weight"]=20
        }
        
      }
      
      #default "vmax":
      for(i in 1:dim(nodesMergedSensors)[1]){
        if(is.na(nodesMergedSensors[i,"roadElmSpeedLimit"])){
          nodesMergedSensors[i,"vmax"]=13.885
        }else{
          #km/h to m/s
          nodesMergedSensors[i,"vmax"]=as.numeric(nodesMergedSensors[i,"roadElmSpeedLimit"])*0.2777
        }    
      }
      
      
      #default "delta_x":
      
      nodesMergedSensors[,"delta_x"]=20
      
      print("---SUBGRAPH MERGING---")
      
      ##########################################
      ##### SLITTING ENTRAMBE LE DIREZIONI #####
      ##########################################
      
      #SI AGGIUNGONO LE STRADE DEL TIPO "ENTRAMBE LE DIREZIONI" NEL VERSO OPPOSTO RISPETTO A QUELLO INDICATO DA NODESMERGEDSENSORS
      #In generale, si raddoppiano le strade etichettate con "entrambe le direzioni", salvo per quelle che hanno il nodo BIS dato che sono gi?
      #inserite in nodesmergedsensors per l'alloggio dei sensori.   
      #Tra tutte quelle che sono etichettate con "entrambe", si principia da quelle che hanno il nodo BIS e si inverte il senso di percorrenza
      tempEntrambe=which(grepl("entrambe",nodesMergedSensors[,"dir"]))
      segName=c()
      indexNote=c()
      if(length(tempEntrambe)>0){
        for(i in 1:length(tempEntrambe)){
          indexInvertion=0
          
          if(grepl("BIS",nodesMergedSensors[tempEntrambe[i],"nodeA"])){
            indexInvertion=1
            print(i)
          }
          
          else if(grepl("BIS",nodesMergedSensors[tempEntrambe[i],"nodeB"])){
            indexInvertion=2
            print(i)
          } 
          
          
          if(indexInvertion>0){
            #tempIndexEnt=c(nodesMergedSensors[tempEntrambe[i],"nodeA"],nodesMergedSensors[tempEntrambe[i],"g1"],nodesMergedSensors[tempEntrambe[i],"nALat"],nodesMergedSensors[tempEntrambe[i],"nALong"])
            tempIndexEnt=c(nodesMergedSensors[tempEntrambe[i],"nodeA"],nodesMergedSensors[tempEntrambe[i],"nALat"],nodesMergedSensors[tempEntrambe[i],"nALong"])
            nodesMergedSensors[tempEntrambe[i],"nodeA"]=nodesMergedSensors[tempEntrambe[i],"nodeB"]
            #nodesMergedSensors[tempEntrambe[i],"g1"]=nodesMergedSensors[tempEntrambe[i],"g2"]
            nodesMergedSensors[tempEntrambe[i],"nALat"]=nodesMergedSensors[tempEntrambe[i],"nBLat"]
            nodesMergedSensors[tempEntrambe[i],"nALong"]=nodesMergedSensors[tempEntrambe[i],"nBLong"]
            
            nodesMergedSensors[tempEntrambe[i],"nodeB"]=tempIndexEnt[1]
            #nodesMergedSensors[tempEntrambe[i],"g2"]=tempIndexEnt[2]
            #nodesMergedSensors[tempEntrambe[i],"nBLat"]=tempIndexEnt[3]
            nodesMergedSensors[tempEntrambe[i],"nBLat"]=tempIndexEnt[2]
            #nodesMergedSensors[tempEntrambe[i],"nBLong"]=tempIndexEnt[4]
            nodesMergedSensors[tempEntrambe[i],"nBLong"]=tempIndexEnt[3]
            
            segName=c(segName,nodesMergedSensors[tempEntrambe[i],"segment"])
            
            nodesMergedSensors[tempEntrambe[i],"segment"]=paste(nodesMergedSensors[tempEntrambe[i],"segment"],"INV",sep="")
            
            indexNote=c(indexNote,i)
          }
          
        }
        #si escludono quelle appena considerate con il nodo BIS e si procede con lo sdoppiamento delle altre, invertendo poi il senso di percorrenza
        if(length(indexNote)>0){
          tempEntrambe=tempEntrambe[-indexNote]
        }
        #sdoppiamento delle altre:
        coordNote=c()
        for(i in 1:length(tempEntrambe)){
          indexName=0
          
          tempName=strsplit(nodesMergedSensors[tempEntrambe[i],"segment"],"--")
          tempNameLast=tempName[[1]][length(tempName[[1]])]
          if(grepl(tempNameLast,"INV")){
            print("no")
          }
          if(length(segName)>0){
            for(j in 1:length(segName)){
              for(k in 1:length(tempName[[1]])){
                
                if(grepl(segName[j],tempName[[1]][k])&(nchar(segName[j])==nchar(tempName[[1]][k]))){
                  indexName=1
                  if(length(tempName[[1]]>1)){
                    coordNote=c(coordNote,tempName[[1]][k])
                    print(paste(segName[j],nodesMergedSensors[tempEntrambe[i],"segment"],sep=" "))
                  }
                  
                }
                
                
              }  
            }
          }
          
          
          
          if(indexName==0){
            
            nodesMergedSensors[dim(nodesMergedSensors)[1]+1,]=nodesMergedSensors[tempEntrambe[i],]
            #tempIndexEnt=c(nodesMergedSensors[tempEntrambe[i],"nodeA"],nodesMergedSensors[tempEntrambe[i],"g1"],nodesMergedSensors[tempEntrambe[i],"nALat"],nodesMergedSensors[tempEntrambe[i],"nALong"])
            tempIndexEnt=c(nodesMergedSensors[tempEntrambe[i],"nodeA"],nodesMergedSensors[tempEntrambe[i],"nALat"],nodesMergedSensors[tempEntrambe[i],"nALong"])
            nodesMergedSensors[dim(nodesMergedSensors)[1],"nodeA"]=nodesMergedSensors[tempEntrambe[i],"nodeB"]
            #nodesMergedSensors[dim(nodesMergedSensors)[1],"g1"]=nodesMergedSensors[tempEntrambe[i],"g2"]
            nodesMergedSensors[dim(nodesMergedSensors)[1],"nALat"]=nodesMergedSensors[tempEntrambe[i],"nBLat"]
            nodesMergedSensors[dim(nodesMergedSensors)[1],"nALong"]=nodesMergedSensors[tempEntrambe[i],"nBLong"]
            
            nodesMergedSensors[dim(nodesMergedSensors)[1],"nodeB"]=tempIndexEnt[1]
            #nodesMergedSensors[dim(nodesMergedSensors)[1],"g2"]=tempIndexEnt[2]
            nodesMergedSensors[dim(nodesMergedSensors)[1],"nBLat"]=tempIndexEnt[2] #3
            nodesMergedSensors[dim(nodesMergedSensors)[1],"nBLong"]=tempIndexEnt[3] #4
            
            nodesMergedSensors[dim(nodesMergedSensors)[1],"segment"]=paste(nodesMergedSensors[tempEntrambe[i],"segment"],"INV",sep="")
          }
          
        }
        
      }
      
      
      nodesMergedSensorsRestrAC=nodesMergedSensors
      
      #memorizzazione delle coordinate dei segmenti accorpati che non contegono "INV"
      segmentList = unique(nodesMergedSensors[which(nodesMergedSensors[, "sensor"] == 0), "segment"])
      memoCoords <- vector("list", length(segmentList))
      names(memoCoords) <- segmentList
      indexNOINV=c()
      for(i in 1:length(memoCoords)){
        
        if(!grepl("INV",names(memoCoords[i]))){
          
          tempName=strsplit(names(memoCoords[i]),"--")
          
          memoCoords[[i]]$realCoord=matrix(c(nodesMergedOrigin[which(nodesMergedOrigin[,"segment"]==tempName[[1]][1])[1],c("nALong","nALat")],0),1,3)
          
          for(j in 1:length(tempName[[1]])){
            
            tempNameMeasure=nodesMergedOrigin[which(nodesMergedOrigin[,"segment"]==tempName[[1]][j])[1],c("nBLong","nBLat","length")]
            memoCoords[[i]]$realCoord=rbind(memoCoords[[i]]$realCoord,c(tempNameMeasure[,"nBLong"],tempNameMeasure[,"nBLat"],tempNameMeasure[,"length"]))
            
          }
          
          indexNOINV=c(indexNOINV,i) 
          
        }
        
        
        
      }  
      
      #per il momento il segmento "diretto" e "inv" HANNO LE COORDINATE NELLA MEDESIMA DIREZIONE
      
      indexINV=c(1:length(memoCoords))
      indexINV=indexINV[-indexNOINV]
      
      if(length(indexINV)>0){
        for(i in 1:length(indexINV)){
          
          if(is.element(unlist(strsplit(names(memoCoords[indexINV[i]]),"INV")),names(memoCoords))){
            
            index=which(names(memoCoords)==unlist(strsplit(names(memoCoords[indexINV[i]]),"INV")))
            
            memoCoords[[indexINV[i]]]$realCoord=memoCoords[[index]]$realCoord
            
          }else{
            
            # altrimenti le coordinate sono nel senso di INV, ovvero invertendo l'ordine dei nodi a partire dai dati di origine "nodesMerdedOrigin"
            print(names(memoCoords[indexINV[i]]))
            
            
            tempName=strsplit(names(memoCoords[indexINV[i]]),"--")
            last=unlist(strsplit(tempName[[1]][length(tempName[[1]])],"INV"))
            
            if(length(tempName[[1]])>=2){
              
              memoCoords[[indexINV[i]]]$realCoord=matrix(c(nodesMergedOrigin[which(nodesMergedOrigin[,"segment"]==tempName[[1]][1]),c("nALong","nALat")],0),1,3)
              
              
              
              for(j in 1:(length(tempName[[1]])-1)){
                
                tempNameMeasure=nodesMergedOrigin[which(nodesMergedOrigin[,"segment"]==tempName[[1]][j]),c("nALong","nALat","length")]
                memoCoords[[indexINV[i]]]$realCoord=rbind(memoCoords[[indexINV[i]]]$realCoord,c(tempNameMeasure[,"nALong"],tempNameMeasure[,"nALat"],tempNameMeasure[,"length"]))
              }
              
            }else{
              
              memoCoords[[indexINV[i]]]$realCoord=matrix(c(nodesMergedOrigin[which(nodesMergedOrigin[,"segment"]==last)[1],c("nALong","nALat")],0),1,3)
              
              
            }
            
            
            memoCoords[[indexINV[i]]]$realCoord=rbind(memoCoords[[indexINV[i]]]$realCoord,matrix(nodesMergedOrigin[which(nodesMergedOrigin[,"segment"]==last)[1],c("nBLong","nBLat","length")],1,3))
            
            
          }
          
          
          
        }
      }
      
      
      nodes = unique(c(unique(nodesMergedSensors$nodeA), unique(nodesMergedSensors$nodeB)))
      
      #-------------------------------------------------------------------------------------  
      # Densit? critica in car/km per 1,2,3 e 4 corsie risp.
      # Dall'ossservazione dei dati relativi ai sensori in oggetto risulta che il parametro pi? plausibile ? quello del flusso veicolare, la velocit?
      # e la densit? sembrano invece derivate in modo errato. Dall'osservazione dei dati risulta inoltre che il flusso massimo si ha per i valori di cui 
      # sotto. Ipotizzando che (in relazione al modello parabola flusso/densit?) il flusso massimo si ha per vfh/2 si possono calcolare le seguenti 
      # densit? per n. corsie 1,2,3 e 4 rispetto ai flussi massimi osservati (riportati di seguito):
      
      # densit? critiche:
      
      dc_1=35          # car/km
      dc_2=70
      dc_3=105
      dc_4=140
      dc_5=175
      dc_6=210
      
      
      ##############################################################
      # PROCEDURA DI CREAZIONE LISTA DELLE STRADE DEL GRAFO - INIZIO
      
      # Descrizione:
      # Per ogni strada della rete presente in nodesMergedSensors si costruisce una lista denominata con l'ID della strada (es. OS00116428767SR)
      # contenente al suo interno tutti i segmenti da cui ? costituita relativamente alla ricerca in nodesMergedSensors 
      # (es. OS00116428767RE/0--OS00116428767RE/1,  OS00116428767RE/2--OS00116428767RE/3--OS00116428767RE/4--OS00116428767RE/5, ecc.)
      # Per ogni segmento (che risulter? essere un arco del grafo stradale che connette due nodi) si determina una matrice (dim 5x1) che
      # contiene tutte le informazioni relative a tale segmento, nell'ordine:
      # - lunghezza del segmento
      # - numero di intervalli delta x nella lunghezza
      # - densit? critica del segmento in gace al numero di corsie (DA VEDERE)
      # - densit? massima
      # - flusso massimo
      # Inoltre la lista di ogni strada della rete contiene al suo interno l'oggetto density, avente un numero di parametri uguale al numero 
      # di segmenti che immagazzina l'evoluzione delle densit? del segmento allo scorrere del tempo, all'interno di una matrice avente il numero
      
      # di colonne uguale al numero di intervalli da cui ? composto il segmento rispetto a deltax.  
      # Tale oggetto density viene inizializzato ad un valore iniziale (al momento 0.05).
      # Parallelamente, all'interno della lista di una strada viene inserito l'oggetto approx (analogo a density) che viene utilizzato 
      # per approssimazione dei calcoli nelle equazioni alle differenze finite.
      
      
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
        ####################################################################################################
        # NOTA: qui ? necessario attribuire una diversa densit? a seconda del numero di corsie della strada:
        ###########################################################
        #########################################
        # si suddivide l'arrotondamento dei segmenti stradali (nel senso JSON) a seconda se lung_seg/delta_x ? inferiore o meno a 0. 
        ####################################################################################################
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
      
      
      # PROCEDURA DI CREAZIONE LISTA DELLE STRADE DEL GRAFO - FINE
      ##############################################################
      ##############################################################
      
      #####################################################################################################
      #####################################################################################################
      # PROCEDURA DI MANIPOLAZIONE DELLA STRADE BIDIREZIONALI (PUNTI DI COORDINATE PER LA GRAFICA) - INIZIO
      
      #algoritmo per il calcolo delle coordinate risp a delta_x tenendo conto di memoCoord
      #per il momento il segmento "diretto" e "inv" HANNO LE COORDINATE NELLA MEDESIMA DIREZIONE
      
      for(k in 1:length(roads)){
        for(z in 1:dim(roads[[k]]$segments)[2]){
          roads[[k]]$lanes[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "lanes"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
          roads[[k]]$weight[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "weight"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
          roads[[k]]$vmax[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "vmax"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
          roads[[k]]$deltax[[(colnames(roads[[k]]$segments)[z])]]=nodesMergedSensors[which(nodesMergedSensors[,"segment"] == (colnames(roads[[k]]$segments))[z]), "delta_x"][1]  #[1] dato che uno stesso segmento pu? essere associato a pi? strade
        }
      }
      
      options(digits=20)
      
      for(k in 1:length(roads)){
        for(z in 1:(dim(roads[[k]]$segments)[2])){
          
          roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]]=matrix(c(as.numeric(memoCoords[[(colnames(roads[[k]]$segments))[z]]]$realCoord[1,1][[1]][1]),as.numeric(memoCoords[[(colnames(roads[[k]]$segments))[z]]]$realCoord[1,2][[1]][1])),1,2)
          coordinate=matrix(as.numeric(memoCoords[[(colnames(roads[[k]]$segments))[z]]]$realCoord),dim(memoCoords[[(colnames(roads[[k]]$segments))[z]]]$realCoord)[1],dim(memoCoords[[(colnames(roads[[k]]$segments))[z]]]$realCoord)[2])
          #ciclo
          delta_x=roads[[k]]$deltax[[(colnames(roads[[k]]$segments)[z])]]
          if((as.numeric(roads[[k]]$segments[2,z]))>2){
            
            #if(!grepl("INV",(colnames(roads[[k]]$segments))[z])){
            countDelta=1
            while(countDelta<(as.numeric(roads[[k]]$segments[2,z])-1)){
              tempLenght=0
              j=1
              while(tempLenght<=as.numeric(delta_x)){
                tempLongPrec=as.numeric(coordinate[countDelta+j-1,1])
                tempLatPrec=as.numeric(coordinate[countDelta+j-1,2])
                tempLengPrec=tempLenght
                tempLenght=tempLenght+as.numeric(coordinate[countDelta+j,3])
                j=j+1
                
              }
              
              new_coord=matrix(0,nrow=dim(coordinate)[1]-j+3,ncol=3)
              new_coord[1:(countDelta),]=coordinate[1:(countDelta),]
              new_coord[(countDelta+2):dim(new_coord)[1],]=coordinate[(countDelta+j-1):dim(coordinate)[1],]
              new_coord[countDelta+1,3]=delta_x
              new_coord[countDelta+2,3]=tempLenght-as.numeric(delta_x)
              new_coord[countDelta+1,1]=tempLongPrec+(as.numeric(delta_x)-tempLengPrec)*(as.numeric(new_coord[countDelta+2,1])-tempLongPrec)/tempLenght
              new_coord[countDelta+1,2]=tempLatPrec+(as.numeric(delta_x)-tempLengPrec)*(as.numeric(new_coord[countDelta+2,2])-tempLatPrec)/tempLenght
              roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]]=rbind(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]],c(new_coord[countDelta+1,1],new_coord[countDelta+1,2]))
              coordinate=new_coord
              countDelta=countDelta+1
              
            }
            
            
          } 
          
          #coordinate centrali:
          
          roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]]=rbind(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]],c(coordinate[dim(coordinate)[1],1],coordinate[dim(coordinate)[1],2]))
          
        }
      }
      
      #SPLITTING PROBLEM:
      #E RIBALTAMENTO COORDINATE NEL CASO INV
      
      for(k in 1:length(roads)){
        for(z in 1:(dim(roads[[k]]$segments)[2])){      
          #dato che roads con strade in entrambe le direzioni ? ordinato in modo che prima compaiono strade senza INV e poi le rispettive con INV
          
          indexSegment=which(nodesMergedSensors[,"segment"]==colnames(roads[[k]]$segments)[z] & nodesMergedSensors[,"sensor"]==0)
          
          if(length(indexSegment)>1){
            print(indexSegment)
          }
          
          if(grepl("entrambe",nodesMergedSensors[indexSegment,"dir"])){
            
            roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]]=roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]]
            
            #Si valuta la direzione del tratto di strada rispetto ai nodi di inizio e fine:
            #A seconda della sua collocazione all'interno del piano, rispetto ai 4 quadranti,
            #si individua la caratteristica del tratto di strada e si calcolano le coordinate
            #rispetto a noINV ed INV.
            
            noteINV=0
            
            if(grepl("INV",(colnames(roads[[k]]$segments))[z])){
              
              noteINV=1
            }
            
            
            
            dist=0.00003
            
            m=c() #coeff angolare
            
            
            for(lol in 1:(dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]-1)){
              
              if((as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))!=0){
                m[lol]=(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1]))/(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))
                
              }else{
                m[lol]=0
              }
              
              if(!is.infinite(m[lol]) & m[lol]!=0){
                
                #I quadrante e II quadrante
                if((as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])<as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))){
                  
                  #print(paste("l",lol,"I e II",sep==""))
                  
                  if(noteINV==0){
                    
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+(dist/(sqrt(m[lol]^2+1)))
                    
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))
                    
                  }else{
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-(dist/(sqrt(m[lol]^2+1)))
                    
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))
                    
                    
                  }
                  
                  
                  
                  if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){
                    
                    if(noteINV==0){
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+(dist/(sqrt(m[lol]^2+1)))
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))
                      
                    }else{
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-(dist/(sqrt(m[lol]^2+1)))
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))
                      
                      
                    }
                    
                  }
                  
                }
                
                
                #III e IV quadrante
                if((as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])>as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))){
                  
                  #print(paste("l",lol,"III e IV",sep==""))
                  
                  if(noteINV==0){
                    
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-(dist/(sqrt(m[lol]^2+1)))
                    
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))
                    
                  }else{
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+(dist/(sqrt(m[lol]^2+1)))
                    
                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))
                    
                    
                  }
                  
                  
                  
                  if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){
                    
                    if(noteINV==0){
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-(dist/(sqrt(m[lol]^2+1)))
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))
                      
                    }else{
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+(dist/(sqrt(m[lol]^2+1)))
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))
                      
                      
                    }
                    
                  }
                  
                }
                
                
                
              }else{
                
                #CASO ORIZZONTALE
                if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])==as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])){
                  
                  print(paste(k,"orizzontale", sep=" "))
                  
                  if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])<as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])){
                    
                    if(noteINV==0){
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-dist
                      
                      
                    }
                    else{
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+dist
                      
                    }
                    
                    
                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){
                      
                      if(noteINV==0){
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-dist
                        
                        
                      }else{
                        
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+dist
                        
                      }
                      
                      
                      
                    }
                    
                  }
                  
                  else{
                    
                    if(noteINV==0){
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+dist
                      
                      
                    }else{
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-dist
                      
                      
                    }
                    
                    
                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){
                      
                      if(noteINV==0){
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+dist
                        
                        
                      }else{
                        
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-dist
                        
                      }
                      
                      
                      
                    }
                    
                  }
                  
                  
                  
                }
                
                
                #CASO VERTICALE
                if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])==as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])){
                  
                  print(paste(k,"verticale", sep=" "))
                  
                  if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])<as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])){
                    
                    if(noteINV==0){
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-dist
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])
                      
                      
                    }else{
                      
                      
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+dist
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])
                      
                    }
                    
                    
                    
                    
                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){
                      
                      if(noteINV==0){
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-dist
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])
                        
                        
                      }else{
                        
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+dist
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])
                        
                        
                      }
                      
                    }  
                    
                    
                  }else{
                    
                    if(noteINV==0){
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+dist
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])
                      
                      
                    }else{
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-dist
                      
                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])
                      
                      
                    }
                    
                    
                    
                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){
                      
                      if(noteINV==0){
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+dist
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])
                        
                        
                      }else{
                        
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-dist
                        
                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])
                        
                        
                        
                      }
                      
                      
                      
                    }
                    
                  }
                  
                  
                  
                }
                
                
              }
              
              
              
            }
            
            
            # if(noteINV==1){
            #   
            #   #ribalta le matrici
            #   
            #   
            #   
            # }
            
          }

          if(nodesMergedSensors[indexSegment,"recSeg"]!=0){

            

            roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]]=roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]]

            

            #analogamente al precedente approccio

            

            noteINV=0

            

            if(grepl("INV",(colnames(roads[[k]]$segments))[z])){

              

              noteINV=1

            }

            

            

            

            dist=0.00003

            

            m=c() #coeff angolare

            

            

            for(lol in 1:(dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]-1)){

              

              if((as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))!=0){

                m[lol]=(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1]))/(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))

                

              }else{

                m[lol]=0

              }

              

              if(!is.infinite(m[lol]) & m[lol]!=0){

                

                #I quadrante e II quadrante

                if((as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])<as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))){

                  

                  #print(paste("l",lol,"I e II",sep==""))

                  

                  if(noteINV==0){

                    

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+(dist/(sqrt(m[lol]^2+1)))

                    

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))

                    

                  }else{

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-(dist/(sqrt(m[lol]^2+1)))

                    

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))

                    

                    

                  }

                  

                  

                  

                  if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){

                    

                    if(noteINV==0){

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+(dist/(sqrt(m[lol]^2+1)))

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))

                      

                    }else{

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-(dist/(sqrt(m[lol]^2+1)))

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))

                      

                      

                    }

                    

                  }

                  

                }

                

                

                #III e IV quadrante

                if((as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])>as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]))){

                  

                  #print(paste("l",lol,"III e IV",sep==""))

                  

                  if(noteINV==0){

                    

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-(dist/(sqrt(m[lol]^2+1)))

                    

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))

                    

                  }else{

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+(dist/(sqrt(m[lol]^2+1)))

                    

                    roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))

                    

                    

                  }

                  

                  

                  

                  if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){

                    

                    if(noteINV==0){

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-(dist/(sqrt(m[lol]^2+1)))

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-(m[lol]*dist/(sqrt(m[lol]^2+1)))

                      

                    }else{

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+(dist/(sqrt(m[lol]^2+1)))

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+(m[lol]*dist/(sqrt(m[lol]^2+1)))

                      

                      

                    }

                    

                  }

                  

                }

                

                

                

              }else{

                

                #CASO ORIZZONTALE

                if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])==as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])){

                  

                  print(paste(k,"orizzontale", sep=" "))

                  

                  if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])<as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])){

                    

                    if(noteINV==0){

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-dist

                      

                      

                    }

                    else{

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+dist

                      

                    }

                    

                    

                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){

                      

                      if(noteINV==0){

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-dist

                        

                        

                      }else{

                        

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+dist

                        

                      }

                      

                      

                      

                    }

                    

                  }

                  

                  else{

                    

                    if(noteINV==0){

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])+dist

                      

                      

                    }else{

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])-dist

                      

                      

                    }

                    

                    

                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){

                      

                      if(noteINV==0){

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])+dist

                        

                        

                      }else{

                        

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])-dist

                        

                      }

                      

                      

                      

                    }

                    

                  }

                  

                  

                  

                }

                

                

                #CASO VERTICALE

                if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])==as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])){

                  

                  print(paste(k,"verticale", sep=" "))

                  

                  if(as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])<as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])){

                    

                    if(noteINV==0){

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-dist

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])

                      

                      

                    }else{

                      

                      

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+dist

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])

                      

                    }

                    

                    

                    

                    

                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){

                      

                      if(noteINV==0){

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-dist

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])

                        

                        

                      }else{

                        

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+dist

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])

                        

                        

                      }

                      

                    }  

                    

                    

                  }else{

                    

                    if(noteINV==0){

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])+dist

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])

                      

                      

                    }else{

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,1])-dist

                      

                      roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][dim(roads[[k]]$coord[[colnames(roads[[k]]$segments)[z]]])[1]-lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol,2])

                      

                      

                    }

                    

                    

                    

                    if(lol+1==dim(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]])[1]){

                      

                      if(noteINV==0){

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])+dist

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][lol+1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])

                        

                        

                      }else{

                        

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,1]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,1])-dist

                        

                        roads[[k]]$coord[[(colnames(roads[[k]]$segments)[z])]][1,2]=as.numeric(roads[[k]]$coordCenter[[(colnames(roads[[k]]$segments)[z])]][lol+1,2])

                        

                        

                        

                      }

                      

                      

                      

                    }

                    

                  }

                  

                  

                  

                }

                

                

              }

              

              

              

            }

            

            

            # if(noteINV==1){

            #   

            #   #ribalta le matrici

            #   

            #   

            #   

            # }

            

          }
          
          
          
          
          
          
          
          
          
          
          
        }
        
      }
      
      print("---SPLITTING STREETS END---")
      
      # PROCEDURA DI MANIPOLAZIONE DELLA STRADE BIDIREZIONALI (PUNTI DI COORDINATE PER LA GRAFICA) - FINE
      #####################################################################################################
      
      
      ###################
      #OUTPUT
      
      # #PROCEDURE DI INVIO DATI TRAFFIC MANAGER O ALTRO REPOSITORY
      # 
      # ####################################################################################
      fluxName=scenario_name
      
      locality=locality
      
      organization="Toscana"
      
      scenarioID=scenario_name
      
      duration="60min"
      
      metricName="TrafficDensity"
      
      unitOfMeasure="vehicle per 20m"
      
      colorMap="densityTrafficMap"
      
      staticGraphName=paste(fluxName,"StaticGraph",sep="")
      
      
      tic()
      roadsToJson <- vector("list", length(roads))
      for (i in 1:length(roads)){
        segmentsNames = colnames(roads[[i]]$segments)
        #####################
        #realName
        realName=c()
        for(r in 1:length(segmentsNames)){
          indexReal=which(nodesMergedSensors[,"segment"]==segmentsNames[r] & nodesMergedSensors[,"sensor"]==0)
          realName=c(realName,
                     nodesMergedSensors[indexReal,"realSegment"])
        }
        #####################
        lengthSmallSegments = sum(roads[[i]]$segments[2,]) - length(roads[[i]]$segments[2,])
        segments <- vector("list", lengthSmallSegments)
        l = 1
        j = 1
        while(j <= lengthSmallSegments){
          coordinatesMatrix = roads[[i]]$coord[[segmentsNames[l]]]
          colnames(coordinatesMatrix) <- c("long", "lat")
          for(k in 1:(dim(coordinatesMatrix)[1]-1)){
            segments[[j]] =list(id = unbox(paste(realName[l], ".", k, sep="")))#, start = c(coordinatesMatrix[k,1],coordinatesMatrix[k,2]), end = c(coordinatesMatrix[k+1,1], coordinatesMatrix[k+1,2]), type="corsie")
            segments[[j]]$start$long = unbox(as.character(coordinatesMatrix[k,1]))
            segments[[j]]$start$lat = unbox(as.character(coordinatesMatrix[k,2]))
            segments[[j]]$end$long = unbox(as.character(coordinatesMatrix[k+1,1]))
            segments[[j]]$end$lat = unbox(as.character(coordinatesMatrix[k+1,2]))
            segments[[j]]$lanes = unbox(as.character(roads[[i]]$lanes[[segmentsNames[l]]]))
            
            #FIPILI check
            if(grepl(names(roads[i]),"OS00001587534LR")){
              segments[[j]]$FIPILI=unbox(1)
            }else{segments[[j]]$FIPILI=unbox(0)}
            
            j = j+1
          }
          l = l+1
        }
        
        roadsToJson[[i]] = list(road = unbox(names(roads[i])), segments = segments)
        
        
      }
      
      jsonStaticGraph = toJSON(roadsToJson)
      toc()
      
      dataGraph=list()
      
      listAttribGraph=list("nameGraphID"=list("staticGraphName"=staticGraphName),
                           "dataGraph"=roadsToJson)
      
      dataGraph[[1]]=listAttribGraph
      
      jsonStaticGraph <- toJSON(dataGraph[[1]], auto_unbox = TRUE,digits=10)
      
      #write.csv(nodesMergedSensorsRestrAC,file=paste(scenario_name,locality,"GraphAC.csv",sep="-"),row.names = FALSE)
      #write_json(jsonStaticGraph, paste(scenario_name,locality,"JS20.json",sep="-"), simplifyVector = TRUE)
      
      #OUTPUT JSON NODERED:
      
      outputAC=list()
      
      for(x in 1:dim(nodesMergedSensorsRestrAC)[1]){
        
        outputAC[[unbox(x)]]=list(
          road=nodesMergedSensorsRestrAC$road[x],
          segment=nodesMergedSensorsRestrAC$segment[x],
          type=nodesMergedSensorsRestrAC$type[x],
          nALat=nodesMergedSensorsRestrAC$nALat[x],
          nALong=nodesMergedSensorsRestrAC$nALong[x],
          nBLat=nodesMergedSensorsRestrAC$nBLat[x],
          nBLong=nodesMergedSensorsRestrAC$nBLong[x],
          dir=nodesMergedSensorsRestrAC$dir[x],
          length=nodesMergedSensorsRestrAC$length[x],
          nodeA=nodesMergedSensorsRestrAC$nodeA[x],
          nodeB=nodesMergedSensorsRestrAC$nodeB[x],
          lanes=nodesMergedSensorsRestrAC$lanes[x],
          elemType=nodesMergedSensorsRestrAC$elemType[x],
          sensor=nodesMergedSensorsRestrAC$sensor[x],
          considered=nodesMergedSensorsRestrAC$considered[x],
          virtual=nodesMergedSensorsRestrAC$virtual[x],
          recSeg=nodesMergedSensorsRestrAC$recSeg[x],
          recRoad=nodesMergedSensorsRestrAC$recRoad[x],
          realSegment=nodesMergedSensorsRestrAC$realSegment[x],
          realRoad=nodesMergedSensorsRestrAC$realRoad[x],
          weight=nodesMergedSensorsRestrAC$weight[x],
          vmax=nodesMergedSensorsRestrAC$vmax[x],
          delta_x=nodesMergedSensorsRestrAC$delta_x[x]
          
        ) 
        
      }
      
      # outAC=toJSON(outputAC,pretty=TRUE,auto_unbox=TRUE)
      # 
      # inData=toJSON(dataJson,pretty=TRUE,auto_unbox=TRUE)
      # 
      # outputDef=list(
      #   serviceUri=SURI,
      #   inputData=inData,
      #   graphAC=outAC,
      #   JS20=jsonStaticGraph
      # )
      
      #####################################################################
      #FUN
      #SAVE the Scenario X in status ACC at DateObserved==Current
      
      device_name=resultScenario$Service$features[[1]]$properties$name
      
      broker=resultScenario$Service$features[[1]]$properties$brokerName
      
      device_type="scenario"
      
      
      # # Create the header
      # header <- add_headers(
      #   "Content-Type" = "application/json",
      #   "Accept" = "application/json",
      #   "Authorization" = paste("Bearer", Token)
      # )
      
      #timestamp = format(Sys.time(), "%Y-%m-%d %H:%M:%OS")
      timestamp_dev = format(Sys.time(), tz = "UTC", "%Y-%m-%dT%H:%M:%OS3Z")
      timestamp <- sub("T", " ", timestamp_dev)  # Sostituisce 'T' con uno spazio
      timestamp <- sub("Z$", "", timestamp)  # Rimuove la 'Z' finale
      # Rimuoviamo i millisecondi (parte dopo i secondi)
      timestamp <- sub("\\.[0-9]+", "", timestamp)
      # payload = toJSON(list(
      #   # id=unbox(scenario_name),
      #   # type=unbox("scenario"),
      #   AC=list(
      #     type=unbox("json"),
      #     value=list(
      #       schema=unbox("processloader_db"),
      #       table=unbox("bigdatafordevice")
      #     )
      #   ),
      #   JS20=list(
      #     type=unbox("json"),
      #     value=list(
      #       schema=unbox("processloader_db"),
      #       table=unbox("bigdatafordevice")
      #     )
      #   ),
      #   TDMStar=list(
      #     type=unbox("json"),
      #     value=unbox("")
      #   ),
      #   TFRDevice=list(
      #     type=unbox("string"),
      #     value=unbox("")
      #   ),
      #   # areaOfInterest=list(
      #   #   type=unbox("json"),
      #   #   value=unbox("")
      #   # ),
      #   dateObserved=list(
      #     type=unbox("string"),
      #     value=unbox(timestamp)
      #   ),
      #   description=list(
      #     type=unbox("string"),
      #     value=unbox(description)
      #   ),
      #   endTime=list(
      #     type=unbox("string"),
      #     value=unbox(endTime)
      #   ),
      #   latitude=list(
      #     type=unbox("float"),
      #     value=unbox("43.77641140000001")
      #   ),
      #   location=list(
      #     type=unbox("string"),
      #     value=unbox(locality)
      #   ),
      #   longitude=list(
      #     type=unbox("float"),
      #     value=unbox("11.266625000000001")
      #   ),
      #   modality=list(
      #     type=unbox("string"),
      #     value=unbox("thisismode")
      #   ),
      #   
      #   name=list(
      #     type=unbox("string"),
      #     value=unbox(device_name)
      #   ),
      #   referenceKB=list(
      #     type=unbox("string"),
      #     value=unbox(referenceKB)
      #   ),
      #   roadGraph=list(
      #     type=unbox("json"),
      #     value=list(
      #       schema=unbox("processloader_db"), 
      #       table=unbox("bigdatafordevice")
      #       )
      #   ),
      #   sourceData=list(
      #     type=unbox("string"),
      #     value=unbox("thissource")
      #   ),
      #   startTime=list(
      #     type=unbox("string"),
      #     value=unbox(startTime)
      #   ),
      #   status=list(
      #     type=unbox("string"),
      #     value=unbox("acc")
      #   ),
      #   trafficSensorList=list(
      #     type=unbox("json"),
      #     value=unbox("sensors")
      #   )
      # ))
      
      payload = toJSON(list(
        dateObserved=list(
          type=unbox("string"),
          value=unbox(timestamp_dev)
        ),
        status=list(
          type=unbox("string"),
          value=unbox("acc")
        ),
        fatherDateObserved=list(
          type=unbox("string"),
          value=unbox(DateObserved)
        )
      ))
      
      send_data_to_device(broker, device_name, device_type, payload, Token)
      
      # brokenUrl="https://www.snap4city.org/orionfilter/orionUNIFI/v2/entities/"
      # # Create the URL
      # urlB <- paste0(brokenUrl,device_name,"/attrs?elementid=",device_name,"&type=scenario")
      # 
      # #payloadEndoded=URLencode(payload)
      # # Make the PATCH request
      # response <- PATCH(url=urlB, 
      #                   body = payload, 
      #                   encode = "json", 
      #                   header,
      #                   config(ssl_verifypeer=0))
      # 
      # print(status_code(response))
      #####################################################################
      
      
      # 2_PASSO
      #mediante POST: il grosso -> ma dove?
      # header <- paste("Bearer", Token)
      # urlScenario <- paste("https://snap4city.org/processloader/api/bigdatafordevice/postIt.php?suri=",SURI,sep="")
      
      ##SI rimette tutto nel DB
       
      
      
      ##SI Aggiunge solo quello che modifco 
      
      # payload=toJSON(list(
      #   grandidati=list(
      #     AC=outputAC,
      #     JS20=roadsToJson,
      #     TDM=outputAC
      #   )
      # ))

      payload=toJSON(list(
        grandidati=list(
          roadGraph=dataDB$grandidati$roadGraph,
          filters=dataDB$grandidati$filters,
          sensors=dataDB$grandidati$sensors,
          restrictions=dataDB$grandidati$restrictions,
          AC=outputAC,
          JS20=roadsToJson,
          TDM=outputAC
        )
      ), digits=20)
      
      send_graph_data_to_db(Token, SURI, payload,dateObserved = timestamp)
      
      # resPOST=POST(
      #   url=urlScenario,
      #   body=list(
      #     AC=outAC,
      #     JS20=jsonStaticGraph
      #   ),
      #   encode = "json", 
      #   add_headers("Content-Type" = "application/json","Authorization" = header), 
      #   config(ssl_verifypeer=0)
      # )
      
      
      # resultPOST <- POST(url = "https://wmsserver.snap4city.org/trafficflowmanager/api/upload?type=staticGraph",
      #                    body = request_body_json,
      #                    encode = "json", add_headers("Content-Type" = "application/json"), config(ssl_verifypeer=0))
      
      # msg.payload = {
      #   "roadGraph": rg,
      #   "AC": ac,
      #   "JS20": js2
      # }
      
      # POST:POST:POST
      # access_token <- content(post_req)$access_token
      # refresh_token <- content(post_req)$refresh_token
      # header <- paste("Bearer", access_token)
      # urlKPI <- paste("https://www.snap4city.org/mypersonaldata/api/v1/kpidata/",kipID,"/values?sourceRequest=plumber&sourceId=",appID,sep="")
      # 
      # infoKPI <- list("dataTime"=as.numeric(Sys.time())*1000,
      #                 "latitude"= 0,
      #                 "longitude"= 0,
      #                 "value"="START RECONSTRUCTION SCENARIO PROCEDURE"
      # )
      # 
      # infoKPI_json <- toJSON(infoKPI, auto_unbox = TRUE)
      # 
      # #POST Data in My Kpi
      # resKPI_POST <- POST(url=urlKPI,
      #                     body=infoKPI_json,  
      #                     encode="json",
      #                     add_headers("Content-Type" = "application/json","Authorization" = header),
      #                     config(ssl_verifypeer=0))
      # 
      # content(resKPI_POST)
      
      #controllo output json se necessario format diverso#
      
      # #OUTPUT JSON
      # 
      # dataOutput=dataJson
      # 
      # dataOutput$graphAC=nodesMergedSensorsRestrAC
      # 
      # #dataOutput=nodesMergedSensorsRestrAC
      # 
      # dataOutputJson0=toJSON(dataOutput,pretty=TRUE)
      # dataOutputJson1=toJSON(dataOutput)
      # 
      # save(dataOutputJson0, file=paste(scenario_name,locality,"GraphAC_0.json",sep="-"))
      # write_json(dataOutputJson1, paste(scenario_name,locality,"GraphAC_1.json",sep="-"), simplifyVector = TRUE)
      
      #save(roads,file=paste(scenario_name,locality,"roads",sep="-"))
      
      # ####################################################################################
      # #PROCEDURE DI INVIO DATI TRAFFIC MANAGER O ALTRO REPOSITORY
      # 
      # ####################################################################################
      # fluxName=paste("FirenzeFIPILITrafficScenario",scenario_name,sep="")
      # 
      # locality="FirenzeFIPILI"(sostituire con locality)
      # 
      # organization="Toscana"
      # 
      # scenarioID=scenario_name
      # 
      # duration="60min"
      # 
      # metricName="TrafficDensity"
      # 
      # unitOfMeasure="vehicle per 20m" 
      # 
      # colorMap="densityTrafficMap"
      # 
      # staticGraphName=paste(fluxName,"StaticGraph",sep="")
      # 
      # 
      # tic()
      # roadsToJson <- vector("list", length(roads))
      # for (i in 1:length(roads)){
      #   segmentsNames = colnames(roads[[i]]$segments)
      #   lengthSmallSegments = sum(roads[[i]]$segments[2,]) - length(roads[[i]]$segments[2,])
      #   segments <- vector("list", lengthSmallSegments)
      #   l = 1
      #   j = 1
      #   while(j <= lengthSmallSegments){
      #     coordinatesMatrix = roads[[i]]$coord[[segmentsNames[l]]]
      #     colnames(coordinatesMatrix) <- c("long", "lat")
      #     for(k in 1:(dim(coordinatesMatrix)[1]-1)){
      #       segments[[j]] =list(id = unbox(paste(segmentsNames[l], ".", k, sep="")))#, start = c(coordinatesMatrix[k,1],coordinatesMatrix[k,2]), end = c(coordinatesMatrix[k+1,1], coordinatesMatrix[k+1,2]), type="corsie")
      #       segments[[j]]$start$long = unbox(as.character(coordinatesMatrix[k,1]))
      #       segments[[j]]$start$lat = unbox(as.character(coordinatesMatrix[k,2]))
      #       segments[[j]]$end$long = unbox(as.character(coordinatesMatrix[k+1,1]))
      #       segments[[j]]$end$lat = unbox(as.character(coordinatesMatrix[k+1,2]))
      #       segments[[j]]$lanes = unbox(roads[[i]]$lanes[[segmentsNames[l]]])
      #       
      #       #FIPILI check
      #       if(grepl(names(roads[i]),"OS00001587534LR")){
      #         segments[[j]]$FIPILI=unbox(1)
      #       }else{segments[[j]]$FIPILI=unbox(0)}
      #       
      #       j = j+1
      #     }
      #     l = l+1
      #   }
      #   
      #   roadsToJson[[i]] = list(road = unbox(names(roads[i])), segments = segments)
      #   
      #   
      # }
      # 
      # jsonStaticGraph = toJSON(roadsToJson)
      # toc()
      # 
      # dataGraph=list()
      # 
      # listAttribGraph=list("nameGraphID"=list("staticGraphName"=staticGraphName),
      #                      "dataGraph"=roadsToJson)
      # 
      # dataGraph[[1]]=listAttribGraph
      # 
      # request_body_json <- toJSON(dataGraph[[1]], auto_unbox = TRUE,digits=10)
      # 
      # ####POST SECTION####
      # 
      # resultPOST <- POST(url = "https://wmsserver.snap4city.org/trafficflowmanager/api/upload?type=staticGraph",
      #                    body = request_body_json,
      #                    encode = "json", add_headers("Content-Type" = "application/json"), config(ssl_verifypeer=0))
      # 
      # ###############TRAFFIC_MANAGER#######################
      # 
      # infoKPI <- list("dataTime"=as.numeric(Sys.time())*1000,
      #                 "latitude"= 0,
      #                 "longitude"= 0,
      #                 "value"="DATA STRUCTURE CREATED"
      # )
      # 
      # infoKPI_json <- toJSON(infoKPI, auto_unbox = TRUE)
      # 
      # #POST Data in My Kpi
      # resKPI_POST <- POST(url=urlKPI,
      #                     body=infoKPI_json,  
      #                     encode="json",
      #                     add_headers("Content-Type" = "application/json","Authorization" = header),
      #                     config(ssl_verifypeer=0))
      # 
      # content(resKPI_POST)
      # ###################################
      # # Converto la lista in JSON
      # json <- toJSON(request_body_json, pretty=TRUE, auto_unbox = TRUE)
      # 
      # return(json)
      
      return(paste("OK",timestamp,sep=""))
      
    }
    else{
      return(paste("KO - ",messageError[indexCheck],sep=""))
    }
  }
  
  
}
