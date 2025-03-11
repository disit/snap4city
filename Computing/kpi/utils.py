'''
   Computing KPI
   Copyright (C) 2024 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''

import json
import requests
import datetime
import math
import os

def get_scenario_device_data_from_suri(access_token, service_uri, use_snap4city=True, microx_ip="192.168.X.X", microx_use_https=False):
    """
    Get scenario data from the service URI.

    Args:
        access_token (str): The access token for authorization.
        service_uri (str): The service URI for the request.
        use_snap4city (bool): Flag to use Snap4City URL or custom microx IP.
        microx_ip (str): The IP address of the microx server.
        microx_use_https (bool): Flag to determine if HTTPS should be used for microx.   

    Returns:
        dict: The JSON response containing the scenario data.

    Raises:
        SystemExit: If an error occurs during the HTTP request.
    """
    if use_snap4city:
        url = os.getenv("BASE_URL","https://www.snap4city.org") + "/superservicemap/api/v1/"        
    else:
        protocol = "https" if microx_use_https else "http"
        url = f"{protocol}://{microx_ip}/superservicemap/api/v1/"

    headers = {
        'Authorization': f'Bearer {access_token}'
    }
    
    complete_url = f"{url}?serviceUri={service_uri}"    
    
    try:
        response = requests.get(complete_url, headers=headers)
        response.raise_for_status()  # Raise an exception for HTTP errors
        return response.json()
    except requests.exceptions.RequestException as e:
        print("FAIL ", e)
        raise e
    
def send_graph_data_to_db(access_token, service_uri, payload, use_snap4city=True, microx_ip="192.168.X.X", microx_use_https=False, dateObserved=""):
    """
    Send data to the specified device endpoint.

    Args:
        access_token (str): The access token for authorization.
        service_uri (str): The service URI for the request.
        payload (str): The payload to be sent to the device. Must be a json STRING e.g. '{"grandidati":{"roadGraph":[{"road":"http://www.d...'
        use_snap4city (bool): Flag to use Snap4City URL or custom microx IP.
        microx_ip (str): The IP address of the microx server.
        microx_use_https (bool): Flag to determine if HTTPS should be used for micro
        dateObserved (str): the date observed that must be in the format '%Y-%m-%d %H:%M:%S'

    Returns:
        str: The message of the HTTP response.

    Raises:
        SystemExit: If an error occurs during the HTTP request.
    """
    if dateObserved == "": 
        dateObserved = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    else:
        try: # check for correct datetime format
            datetime.datetime.strptime(dateObserved, '%Y-%m-%d %H:%M:%S')        
        except:
            print("dateObserved not in the correcto format ('%Y-%m-%d %H:%M:%S')")
            print("updated to current datetime")
            dateObserved = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
    if use_snap4city:
        base_url = os.getenv("BASE_URL","https://www.snap4city.org")
    else:
        protocol = "https" if microx_use_https else "http"
        base_url = f"{protocol}://{microx_ip}"
    
    url = f"{base_url}/processloader/api/bigdatafordevice/postIt.php?suri={service_uri}&accessToken={access_token}&dateObserved={dateObserved}"
    
    headers = {
        'Authorization': f'Bearer {access_token}'
    }
    
    try:
        response = requests.post(url, data=payload, headers=headers)
        response.raise_for_status()  # Raise an exception for HTTP errors
        return response
    except requests.exceptions.RequestException as e:
        print("FAIL ", e)
        raise e

def get_graph_data_from_suri(access_token, service_uri, use_snap4city=True, microx_ip="192.168.X.X", microx_use_https=False):
    """
    Get device data from the service URI.

    Args:
        access_token (str): The access token for authorization.
        service_uri (str): The service URI for the request.
        use_snap4city (bool): Flag to use Snap4City URL or custom microx IP.
        microx_ip (str): The IP address of the microx server.
        microx_use_https (bool): Flag to determine if HTTPS should be used for microx.

    Returns:
        dict: The JSON response containing the device data.

    Raises:
        SystemExit: If an error occurs during the HTTP request.
    """
    if use_snap4city:
        base_url = os.getenv("BASE_URL","https://www.snap4city.org")
    else:
        protocol = "https" if microx_use_https else "http"
        base_url = f"{protocol}://{microx_ip}"

    url = f"{base_url}/processloader/api/bigdatafordevice/getOneSpecific.php?suri={service_uri}&accessToken={access_token}"
    headers = {
        'Authorization': f'Bearer {access_token}'
    }
    
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()  # Raise an exception for HTTP errors
        return response.json()
    except requests.exceptions.RequestException as e:
        print("FAIL ", e)
        raise e
    
# 2) PROVO A PRENDERE I DATI DAL TFMANAGER

def fetch_traffic_data(tfmref, selected_datetime):
    """
    Recupera i dati del traffico per un dispositivo e una data/ora specifici.

    Args:
        device_name (str): Il nome del dispositivo da cui recuperare i dati.
        selected_datetime (str): La data e l'ora, che verranno formattate in un formato accettabile per l'URL.

    Returns:
        dict: I dati restituiti dall'API come dizionario.
        str: Un messaggio di errore in caso di problemi.
    """
    
    # Costruisci l'URL utilizzando device_name e formatted_datetime
    base_url = os.getenv("BASE_URL","https://wmsserver.snap4city.org") + "/trafficflowmanager/api/json"
    url = f"{base_url}?layerName={tfmref}_{selected_datetime}"
    
    try:
        # Effettua la richiesta GET
        response = requests.get(url)
        
        # Verifica se la richiesta è andata a buon fine
        response.raise_for_status()  # Solleva un'eccezione per status code 4xx/5xx
        
        # Restituisce i dati JSON come dizionario
        return response.json()
    
    except requests.RequestException as e:
        # Gestisce eventuali eccezioni di rete o HTTP
        return f"Errore durante la richiesta: {e}"
    
def get_tfmanager_results_for_reconstruction_date(dati_ricostruzione,DATEOBSERVED_ricostruzione):
    """
    Filtro i dati del tfmanager per prendere quelli di una data specifica

    Args:
        dati_ricostruzione (dict): i dati dal tfmanager
        DATEOBSERVED_ricostruzione (str): DateOnserved nel formato AAAA-MM-GGTHH:MM:SS
    
    Returns:
        dict: I dati della ricostruzione all'ora specificata

    """
    
    res = []
    for v in dati_ricostruzione['result']:
        if DATEOBSERVED_ricostruzione in v['dateObserved'] :
            res.append(v)
    return res
    
def fetch_traffic_data_tfmanager(TFM_REF, DATEOBSERVED_ricostruzione):
    """
    Recupera i dati del traffico per un dispositivo e una data/ora specifici.

    Args:
        TFM_REF (str): Il riferimeto al Traffic Flow Manager

    Returns:
        dict: I dati restituiti dall'API come dizionario.
        str: Un messaggio di errore in caso di problemi.
    """
    
    # Costruisci l'URL utilizzando il rif fornito
    #url = f"https://www.snap4city.org/ServiceMap/api/v1/trafficflow/?scenario={TFM_REF}"
    url = os.getenv("BASE_URL","https://www.snap4city.org") + f"/superservicemap/api/v1/trafficflow/?scenario={TFM_REF}"

    
    try:
        # Effettua la richiesta GET
        response = requests.get(url)
        
        # Verifica se la richiesta è andata a buon fine
        response.raise_for_status()  # Solleva un'eccezione per status code 4xx/5xx
        
        # Restituisce i dati JSON come dizionario
        res = get_tfmanager_results_for_reconstruction_date(response.json(),DATEOBSERVED_ricostruzione)
        return res
    
    except requests.RequestException as e:
        # Gestisce eventuali eccezioni di rete o HTTP
        return f"Errore durante la richiesta: {e}"
    
def calcola_distanza(coord_start, coord_end):
    """
    Calcola la distanza tra due punti sulla superficie terrestre utilizzando la formula dell'Haversine.
    
    La distanza è calcolata in chilometri e tiene conto della curvatura della Terra.

    Parameters:
    ----------
    coord_start : dict
        Un dizionario contenente le coordinate del punto di partenza. 
        Deve avere la struttura {'location': {'lon': 'longitudine', 'lat': 'latitudine'}}.
    coord_end : dict
        Un dizionario contenente le coordinate del punto di arrivo.
        Deve avere la struttura {'location': {'lon': 'longitudine', 'lat': 'latitudine'}}.

    Returns:
    -------
    float
        La distanza tra i due punti in chilometri.
    """
    
    # Estrai le coordinate
    lon1, lat1 = float(coord_start['location']['lon']), float(coord_start['location']['lat'])
    lon2, lat2 = float(coord_end['location']['lon']), float(coord_end['location']['lat'])
    
    # Raggio della Terra in km
    R = 6371.0
    
    # Conversione delle coordinate in radianti
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    lon1_rad = math.radians(lon1)
    lon2_rad = math.radians(lon2)
    
    # Differenza tra le coordinate
    delta_lat = lat2_rad - lat1_rad
    delta_lon = lon2_rad - lon1_rad
    
    # Calcolo della formula di Haversine
    a = math.sin(delta_lat / 2)**2 + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(delta_lon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    
    # Distanza finale in km
    distanza_km = R * c
    return distanza_km

def get_density_max(numerolanes):
    """"
    Calcola la densità massima (veicoli al km) tramite il numero di corsie del segmento considerato 

    Args:
        numerolanes (int): il numero di corsie del segmento considerato
    
    Returns:
        int: il valore della densità max

    """
    densit_maxs = [35, 70, 105, 140, 175, 210]

    return  2 * densit_maxs[numerolanes - 1]
    
def get_flow_class(numerolanes, ladensity, conversionfactor = 2, verbose = 0):
    """
    dato il numero di lanes e ladensity
    ti restituisce
    0 se è in Free Flow
    1 se è in Fluid Flow
    2 se è in Heavy Flow
    3 se è in Very Hhavy Flow
    """
    
    densità = [35,70,105,140,175,210]
    critical_density = densità[numerolanes]*2   # per aggiungere anche l'info sulla densità massima
    density =  ladensity * 50 *conversionfactor
    
    if verbose ==1:print("density", density)
    if verbose ==1:print("critical_density",critical_density)
    if 0 < density < critical_density / 2:
        return 0
    elif critical_density / 2 <= density <= critical_density:
        return 1
    elif critical_density < density <= critical_density * 3 / 2:
        return 2
    elif critical_density * 3 / 2 < density <= critical_density * 4:
        return 3

def get_lane_values(data):
    """
    Estrae i valori dalle chiavi nel dizionario 'lanes' e li converte in interi.
    
    :param data: Dizionario contenente la chiave 'lanes'.
    :return: Lista di valori interi estratti e convertiti.
    """
    lane_values = []
    lanes = data.get('lanes', {})  # Ottiene il dizionario 'lanes', vuoto se non presente
    
    for key in lanes:
        # Converte il valore in intero e lo aggiunge alla lista
        value = int(lanes[key][0])
        lane_values.append(value)
    
    return lane_values

def analisi_dati_ricostruzione_1(dati_ricostruzione, log_level = 0):
    """"
    Prima analisi dei dati ottenuti dal TFManager
    In particolare vengono arricchiti con le informazioni su:
        - density_max # determinata dal num di corsie (veic per km)
        - trafficState # lo stato di traffico sul segmento stradale (0.FreeFLow - 1.FluidFLow - 2. HeavyFlow - 3.VeryHEavyFlow)
        - length #la lunghezza in km del segmento considerato
        - numeroVeicoli # il num dei veic sul segmento tramite densità * lunghezza
        - velocità_media #la velocità media sul segmento in km/h calcolata a partire dal vmax....
        - tempo_di_running # il tempo per percorrere il segmento in quelle condizioni di traffico in secondi


    Calcola inoltre i km totali dello scenario considerato e il num tot dei veicoli

    Args:
        dati_ricostruzione (dict): dizionario contente i dati di traffico con chiavi
        # dict_keys(['dateObserved', 'density', 'kind', 'line', 'start', 'dir', 'lane_numbers', 'segments', 'scenario', 'numVehicle', 'roadElements', 'end', 'flow'])
        log_level (int): da impostare a 0 se nn vuoi i dettagli dei valori per ogni segmento / valore > 1 se lo vuoi

    Returns:
        Dict: una copia dei dati di ricostruzione arricchiti
        float: km tot dello scenario
        float: num tot veicoli nello scenario    
        float: tempo di attraversameto dello scenario in s    
    """
    log_level = 0 #metti a 0 se nn vuoi i print, altrimenti un qualsiasi valore > 1 per i dettagli
    tot_km, tot_veicoli, tot_running = 0, 0, 0 # per il calcolo dei km totali e del numero di veicoli complessivo sul grafo strade considerato
    # risistemo i parametri per il calcolo dei kpi a partire dai dati del tfmanager
    for tmp_dati_ricostruzione in dati_ricostruzione:
        numerolanes = int(tmp_dati_ricostruzione['lane_numbers'])
        if log_level> 1: print("numero lanes:", numerolanes)

        density_max = get_density_max(numerolanes)
        tmp_dati_ricostruzione['density_max'] = density_max
        if log_level> 1: print("densità massima sul segmento:", density_max)

        ladensity = float(tmp_dati_ricostruzione['density'])
        if log_level> 1: print("density:", ladensity)

        laclasse = get_flow_class(numerolanes, ladensity, conversionfactor = 0.04, verbose = 0)
        tmp_dati_ricostruzione['trafficState'] = laclasse    
        if log_level> 1: print("traffic state:", laclasse)

        coord_start = tmp_dati_ricostruzione['start']
        coord_end = tmp_dati_ricostruzione['end']
        L_in_km = calcola_distanza(coord_start, coord_end)
        L_in_m = L_in_km * 1000
        tmp_dati_ricostruzione['length'] = L_in_km
        tot_km += L_in_km
        num_veic_sul_segment = ladensity * L_in_km
        tot_veicoli += num_veic_sul_segment
        tmp_dati_ricostruzione['numeroVeicoli'] = num_veic_sul_segment 
        if log_level> 1: print("numero veicoli sul segmento:", num_veic_sul_segment)

        # TODO: velocità media in 'velocità_media' ! errore perchè nn c'è il valore di vmax associato al segmento stradale, imp ottenere il tempo di running complessivo
        
        # per ora nn c'è va aggiornato per ora lo metto a 14 m al secondo 
        # vmax_m_s = float(tmp_dati_ricostruzione['vmax']) # NB è in m/s... forse
        vmax_m_s = 14
        vmax = vmax_m_s * 3.6
        vkmh = vmax * (1 - (ladensity / density_max)) # km/h
        vms = vkmh / 3.6
        tmp_dati_ricostruzione['velocità_media'] = vkmh # km/h
        if log_level> 1: print("velocità media sul segmento in km/h:", vkmh)

        # Aggiungi l'informazione sul tempo di running sul segmento
        T = L_in_m / vms # [s]
        tmp_dati_ricostruzione['tempo_di_running'] = T # [s]
        # tmp_dati_ricostruzione['tempo_di_running_comprensivo_del_num_veicoli'] = T * num_veic_sul_segment # [s]
        tot_running += T

    return dati_ricostruzione.copy(), tot_km, tot_veicoli, tot_running

def rimappa_in_base_all_accorpato(dati_ricostruzione, ACrimappato, ora=9):
    howmanyna = 0

    # Conta quante densità sono "NA"
    for road, data_dict in dati_ricostruzione['reconstructionData'].items():
        for item in data_dict['data']:
            for key in item:
                if item[key] == "NA":
                    howmanyna += 1
    print(f"WARN ci sono {howmanyna} NA nella ricostruzione ")

    # Mappa le densità dai dati di ricostruzione agli accorpati
    for road, data_dict in dati_ricostruzione['reconstructionData'].items():
        for item in data_dict['data']:
            for key, density in item.items():
                il_rico_e_a_20_m = "INV" in key
                key = key.split(".")[0]
                for accorpato in ACrimappato:
                    sto_acc_e_a_20_m = "INV" in key
                    segmenti_accorpati = accorpato['segment']
                    if sto_acc_e_a_20_m == il_rico_e_a_20_m:
                        if key in segmenti_accorpati:
                            accorpato['ricostruito_ora_' + str(ora)] = density

    # Aggiusta i valori che non sono stati trovati o sono NA
    for val in ACrimappato:
        if 'ricostruito_ora_' + str(ora) not in val or val['ricostruito_ora_' + str(ora)] == "NA":
            val['ricostruito_ora_' + str(ora)] = 0.000000001

    # Aggiorna la classe di densità
    for val in ACrimappato:
        try:
            ladensity = float(val['ricostruito_ora_' + str(ora)])
        except ValueError:
            print("Problema nel convertire la densità")
            ladensity = 0.000000001
        #print(val)
        numerolanes = get_lane_values(val)[0]
        laclasse = get_flow_class(numerolanes - 1, ladensity, 2)
        val['trafficState'] = laclasse

    densit_maxs = [35, 70, 105, 140, 175, 210]  
    tot_km = 0
    tot_veicoli = 0
    tot_running = 0

    for val in ACrimappato:
        # Sistema l'info sulla densità
        d = float(val['ricostruito_ora_' + str(ora)])
        densità_veicoli_al_km = (d / 20) * 1000
        val['densità_veicoli_al_km'] = densità_veicoli_al_km

        # Aggiungi l'informazione sul numero di veicoli sul segmento
        L_in_m = float(val['length'][0])
        L_in_km = L_in_m * 0.001
        num_veic_sul_segment = (d / 20) * L_in_m
        val['numeroVeicoli'] = num_veic_sul_segment

        # Aggiungi l'informazione sulla velocità media sul segmento in base alla densità

        value =  get_lane_values(val)[0]
        densit_max = 2 * densit_maxs[value - 1] # NB è in veicoli al km
        vmax = float(val['vmax'][0]) * 3.6 # NB è in km/h
        vkmh = vmax * (1 - (densità_veicoli_al_km / densit_max)) # km/h
        vms = vkmh / 3.6
        val['velocità_media'] = vkmh # km/h

        # Aggiungi l'informazione sul tempo di running sul segmento
        T = L_in_m / vms # [s]
        val['tempo_di_running'] = T # [s]
        val['tempo_di_running_comprensivo_del_num_veicoli'] = T * num_veic_sul_segment # [s]

        # Aggiorna i totali
        tot_km += L_in_km
        tot_veicoli += val['numeroVeicoli']
        tot_running += val['tempo_di_running']

    return ACrimappato, tot_km, tot_veicoli, tot_running

def determina_consumo(ACrimappato):
    consumo_tot = 0
    for val in ACrimappato:
        vkmh = val['velocità_media']
        L_in_m = float(val['length'][0])
        num_veic_sul_segment = val['numeroVeicoli']
        #### aggiungo l'informazione sul consumo di carburante per veicolo
        # litri ogni 100 km per veicolo
        if vkmh<10:
            c = 128 + (800/(vkmh*vkmh*8))
        else:
            c= 7 + (99/vkmh)
        # quindi lo voglio al metro
        c = c /100 # al km
        c = c /1000 #al m
        # è per veicolo quindi lo voglio moltiplicare per il numero di veicoli e per la lunghezza del segmento
        c = c *L_in_m
        val["carburante_consumato"] = c 
        val["carburante_consumato_comprensivo_del_num_veicoli"] = c  * num_veic_sul_segment # [Litri]
        consumo_tot +=val["carburante_consumato_comprensivo_del_num_veicoli"]
    return ACrimappato, consumo_tot

def determina_consumo_tfm(dati_ricostruzione):
    consumo_tot = 0
    for val in dati_ricostruzione:
        vkmh = val['velocità_media']
        L_in_km = float(val['length'])
        L_in_m = L_in_km*1000
        num_veic_sul_segment = val['numeroVeicoli']
        #### aggiungo l'informazione sul consumo di carburante per veicolo
        # litri ogni 100 km per veicolo
        if vkmh<10:
            c = 128 + (800/(vkmh*vkmh*8))
        else:
            c= 7 + (99/vkmh)
        # quindi lo voglio al metro
        c = c /100 # al km
        c = c /1000 #al m
        # è per veicolo quindi lo voglio moltiplicare per il numero di veicoli e per la lunghezza del segmento
        c = c *L_in_m
        val["carburante_consumato"] = c 
        val["carburante_consumato_comprensivo_del_num_veicoli"] = c  * num_veic_sul_segment # [Litri]
        consumo_tot +=val["carburante_consumato_comprensivo_del_num_veicoli"]
    return dati_ricostruzione.copy(), consumo_tot


def determina_CO2(ACrimappato):
    co2_tot = 0
    for val in ACrimappato:
        densità_veicoli_al_km = val['densità_veicoli_al_km']
        vkmh = val['velocità_media']
        L_in_m = float(val['length'][0])
        L_in_km = L_in_m * 0.001
        ### informzione sulla CO2 di stef
        f_auto_allora = densità_veicoli_al_km * vkmh
        val['f_auto_allora'] = f_auto_allora
        try:
            if val['trafficState'] < 2:
                CO2_segmento = 272* densità_veicoli_al_km*L_in_km
            else:
                CO2_segmento = 496.3* densità_veicoli_al_km*L_in_km
        except: 
            CO2_segmento = 0
        val["CO2_segmento"] = CO2_segmento
        co2_tot +=  val["CO2_segmento"] 
    return ACrimappato, co2_tot

def determina_CO2_tfm(dati_ricostruzione):
    co2_tot = 0
    for val in dati_ricostruzione:
        densità_veicoli_al_km = val['density']
        vkmh = val['velocità_media']
        L_in_km = float(val['length'])
        #L_in_m = L_in_km* 1000
        ### informzione sulla CO2 di stef
        f_auto_allora = densità_veicoli_al_km * vkmh
        val['f_auto_allora'] = f_auto_allora
        try:
            if val['trafficState'] < 2:
                CO2_segmento = 272* densità_veicoli_al_km*L_in_km
            else:
                CO2_segmento = 496.3* densità_veicoli_al_km*L_in_km
        except: 
            CO2_segmento = 0
        val["CO2_segmento"] = CO2_segmento
        co2_tot +=  val["CO2_segmento"] 
    return dati_ricostruzione.copy(), co2_tot



def get_scenario_device_data_from_suri(access_token, service_uri, use_snap4city=True, microx_ip="192.168.X.X", microx_use_https=False):
    """
    Get scenario data from the service URI.

    Args:
        access_token (str): The access token for authorization.
        service_uri (str): The service URI for the request.
        use_snap4city (bool): Flag to use Snap4City URL or custom microx IP.
        microx_ip (str): The IP address of the microx server.
        microx_use_https (bool): Flag to determine if HTTPS should be used for microx.   

    Returns:
        dict: The JSON response containing the scenario data.

    Raises:
        SystemExit: If an error occurs during the HTTP request.
    """
    if use_snap4city:
        url = os.getenv("BASE_URL","https://www.snap4city.org") + "/superservicemap/api/v1/"        
    else:
        protocol = "https" if microx_use_https else "http"
        url = f"{protocol}://{microx_ip}/superservicemap/api/v1/"

    headers = {
        'Authorization': f'Bearer {access_token}'
    }
    
    complete_url = f"{url}?serviceUri={service_uri}"    
    try:
        response = requests.get(complete_url, headers=headers)
        response.raise_for_status()  # Raise an exception for HTTP errors
        return response.json()
    except requests.exceptions.RequestException as e:
        print("FAIL ", e)
        raise e


def kpi_sull_accorpat_con_distanza(ACrimappato, ora = 9):
    """
    funzione che sull'accorpato mi calcola i kpi per bene! 
    ps prima non andava mi sa...
    """
    FRrs = 0
    FLrs = 0
    HErs = 0
    VHrs = 0
    lunghezza_tot = 0
    for v in ACrimappato:
        
        ladensity = float(v['ricostruito_ora_'+ str(ora)])
        densità = [35,70,105,140,175,210]
        numerolanes =  get_lane_values(v)[0]
        conversionfactor = 2
        critical_density = densità[numerolanes]*2   # per aggiungere anche l'info sulla densità massima
        density =  ladensity * 50 *conversionfactor
        lunghezza = float(v['length'][0])

        # print(density)
        # print(critical_density)
        if 0 < density < critical_density / 2:
            FRrs += lunghezza
        elif critical_density / 2 <= density <= critical_density:
            FLrs += lunghezza
        elif critical_density < density <= critical_density * 3 / 2:
            HErs += lunghezza
        elif critical_density * 3 / 2 < density <= critical_density * 4:
            VHrs += lunghezza
        lunghezza_tot = lunghezza_tot + lunghezza

    # FRrs = FRrs / lunghezza_tot
    # #print(FRrs)
    # FLrs = FLrs / lunghezza_tot
    # #print(FLrs)
    # HErs = HErs / lunghezza_tot
    # #print(HErs)
    # VHrs = VHrs / lunghezza_tot
    # #print(VHrs)
    return FRrs*0.001, FLrs*0.001, HErs*0.001, VHrs*0.001

def kpi_traffic_state(dati_ricostruzione):

    FRrs, FLrs, HErs, VHrs = 0,0,0,0

    for v in dati_ricostruzione:
        
        density = float(v['density'])
        critical_density = v['density_max']        
        trafficState = v['trafficState']
        
        if trafficState == 0:
            FRrs += v['length']
        elif trafficState == 1:
            FLrs += v['length']
        elif trafficState==2:
            HErs += v['length']
        elif trafficState ==3:
            VHrs += v['length']
    
    funzionale_traffic_state = 1*FRrs + 2*FLrs + 20*HErs + 30*VHrs

    return funzionale_traffic_state, FRrs, FLrs, HErs, VHrs 

def kpi_service_old_old(SURI, DATEOBSERVED_scenario, DATEOBSERVED_ricostruzione, ACCESS_TOKEN):
    DATEOBSERVED_scenario_orig = DATEOBSERVED_scenario
    DATEOBSERVED_scenario_date = DATEOBSERVED_scenario.split("T")[0]
    DATEOBSERVED_scenario_time = DATEOBSERVED_scenario.split("T")[1].replace("-",":")
    DATEOBSERVED_scenario = f"{DATEOBSERVED_scenario_date}T{DATEOBSERVED_scenario_time}"

    BROKER =  SURI.split("/")[-3]
    ORGANIZATION = SURI.split("/")[-2]
    DEVICENAME = SURI.split("/")[-1]

    device_data = get_scenario_device_data_from_suri(ACCESS_TOKEN, SURI)
    LOCATION = device_data['realtime']['results']['bindings'][0]['location']['value']

    # 1) PROVO A PRENDERE I DATI DAL MYSQL
    result = get_graph_data_from_suri(ACCESS_TOKEN, SURI)
    # Trova i dati corrispondenti alla data fornita
    for res in result:
        if len(json.loads(res['data'])['grandidati']['AC']) > 1:
            data_json = json.loads(res['data'])  # Converti il string JSON in un dizionario Python
            AC = data_json.get('grandidati', {}).get('AC', []) 
            print("ok ac preso")
            break
    else:
        print(f"Nessun dato trovato per la data {DATEOBSERVED_scenario}")
        
    # 2) prendi i dati dalla ricostruzione
    # sistemo la data 
    DATEOBSERVED_ricostruzione = DATEOBSERVED_ricostruzione.replace(" ", "T")
    DATEOBSERVED_ricostruzione = DATEOBSERVED_ricostruzione.replace(":", "-")

    TFM_REF = f"{LOCATION}_{BROKER}_{ORGANIZATION}_{DEVICENAME}_{DATEOBSERVED_scenario_orig}".replace(" ", "")
    dati_ricostruzione = fetch_traffic_data(TFM_REF, DATEOBSERVED_ricostruzione) 

    # 3) e ora associa i valori di densità all'accorpato dai dati_ricostrunione e determina le prime metriche
    ACrimappato, tot_km, tot_veicoli, tot_running = rimappa_in_base_all_accorpato(dati_ricostruzione, AC, ora=9)

    # 4) mi calcolo il consumo
    ACrimappato, consumo_tot = determina_consumo(ACrimappato)

    # 5) co2
    ACrimappato, co2_tot = determina_CO2(ACrimappato)

    # 6) anche le componenti e il funzionale di  traffic state
    FRrs, FLrs, HErs, VHrs = kpi_sull_accorpat_con_distanza(ACrimappato)
    funzionale_traffic_state = 1*FRrs + 2*FLrs + 20*HErs + 30*VHrs
    return tot_km, tot_veicoli, tot_running , consumo_tot, co2_tot, FRrs, FLrs, HErs, VHrs, funzionale_traffic_state


# dovree essere def kpi_service_tfm(SURI, DATEOBSERVED_scenario, DATEOBSERVED_ricostruzione, ACCESS_TOKEN):
# per la prod lo chiamo kpi_service
def kpi_service_old(SURI, DATEOBSERVED_scenario, DATEOBSERVED_ricostruzione, ACCESS_TOKEN):
    DATEOBSERVED_scenario_orig = DATEOBSERVED_scenario
    DATEOBSERVED_scenario_date = DATEOBSERVED_scenario.split("T")[0]
    DATEOBSERVED_scenario_time = DATEOBSERVED_scenario.split("T")[1].replace("-",":")
    DATEOBSERVED_scenario = f"{DATEOBSERVED_scenario_date}T{DATEOBSERVED_scenario_time}"
    dataok = DATEOBSERVED_ricostruzione.split("T")[0]
    oraapposto = DATEOBSERVED_ricostruzione.split("T")[1].replace("-",":")
    DATEOBSERVED_ricostruzione = "{}T{}".format(dataok, oraapposto)
    DATEOBSERVED_ricostruzione
    

    BROKER =  SURI.split("/")[-3]
    ORGANIZATION = SURI.split("/")[-2]
    DEVICENAME = SURI.split("/")[-1]

    device_data = get_scenario_device_data_from_suri(ACCESS_TOKEN, SURI)
    LOCATION = device_data['realtime']['results']['bindings'][0]['location']['value']

    # 1) PROVO A PRENDERE I DATI DAL MYSQL
    result = get_graph_data_from_suri(ACCESS_TOKEN, SURI)
    # Trova i dati corrispondenti alla data fornita
    for res in result:
        if len(json.loads(res['data'])['grandidati']['AC']) > 1:
            data_json = json.loads(res['data'])  # Converti il string JSON in un dizionario Python
            AC = data_json.get('grandidati', {}).get('AC', []) 
            print("ok ac preso")
            break
    else:
        print(f"Nessun dato trovato per la data {DATEOBSERVED_scenario}")
        
    # 2) prendi i dati dalla ricostruzione
    # sistemo la data 

    TFM_REF = f"{BROKER}_{ORGANIZATION}_{DEVICENAME}_{DATEOBSERVED_scenario_orig}".replace(" ", "").replace(":", "-")
    dati_ricostruzione = fetch_traffic_data_tfmanager(TFM_REF,DATEOBSERVED_ricostruzione) 

    # 3) e ora associa i valori di densità all'accorpato dai dati_ricostrunione e determina le prime metriche
    # era cosi ACrimappato, tot_km, tot_veicoli, tot_running = rimappa_in_base_all_accorpato(dati_ricostruzione, AC, ora=9)
    # ora aggiornato:
    dati_ricostruzione_v1, tot_km, tot_veicoli, tot_running = analisi_dati_ricostruzione_1(dati_ricostruzione,log_level = 2)

    # 4) mi calcolo il consumo
    # era ACrimappato, consumo_tot = determina_consumo(ACrimappato)
    dati_ricostruzione_v2, consumo_tot = determina_consumo_tfm(dati_ricostruzione_v1)

    # 5) co2
    # era ACrimappato, co2_tot = determina_CO2(ACrimappato)
    dati_ricostruzione_v3, co2_tot = determina_CO2_tfm(dati_ricostruzione_v2)

    # 6) anche le componenti e il funzionale di  traffic state
    #era FRrs, FLrs, HErs, VHrs = kpi_sull_accorpat_con_distanza(ACrimappato)
    # funzionale_traffic_state = 1*FRrs + 2*FLrs + 20*HErs + 30*VHrs
    funzionale_traffic_state, FRrs, FLrs, HErs, VHrs = kpi_traffic_state(dati_ricostruzione)

    return tot_km, tot_veicoli, tot_running , consumo_tot, co2_tot, FRrs, FLrs, HErs, VHrs, funzionale_traffic_state


def kpi_service(SURI, DATEOBSERVED_scenario, DATEOBSERVED_ricostruzione, ACCESS_TOKEN):
    DATEOBSERVED_scenario_orig = DATEOBSERVED_scenario
    DATEOBSERVED_scenario = DATEOBSERVED_scenario.replace("T", " ").split(".")[0]
    
    dataok = DATEOBSERVED_ricostruzione.split("T")[0]
    oraapposto = DATEOBSERVED_ricostruzione.split("T")[1].replace("-",":").split("+")[0]
    DATEOBSERVED_ricostruzione = "{}T{}".format(dataok, oraapposto)
    DATEOBSERVED_ricostruzione


    BROKER =  SURI.split("/")[-3]
    ORGANIZATION = SURI.split("/")[-2]
    DEVICENAME = SURI.split("/")[-1]

    device_data = get_scenario_device_data_from_suri(ACCESS_TOKEN, SURI)
    LOCATION = device_data['realtime']['results']['bindings'][0]['location']['value']

    # 1) PROVO A PRENDERE I DATI DAL MYSQL
    result = get_graph_data_from_suri(ACCESS_TOKEN, SURI)
    # Trova i dati corrispondenti alla data fornita
    for res in result:
        try:
            data_json = json.loads(res['data'])  # Converti il string JSON in un dizionario Python
            AC = data_json['grandidati']['AC']
            
            if len(AC) > 1:
                print("ok ac preso")
                break
        except KeyError:
            # Se 'AC' non esiste, il codice qui dentro verrà eseguito
            continue
    else:
        print(f"Nessun dato trovato per la data {DATEOBSERVED_scenario}")
        
    # 2) prendi i dati dalla ricostruzione
    # sistemo la data 

    #TFM_REF = f"{BROKER}_{ORGANIZATION}_{DEVICENAME}_{DATEOBSERVED_scenario_orig}".replace(" ", "").replace(":", "-")
    TFM_REF = f"{BROKER}_{ORGANIZATION}_{DEVICENAME}_{DATEOBSERVED_scenario_orig}".replace(" ", "").replace(":", "-").split(".")[0]
    dati_ricostruzione = fetch_traffic_data_tfmanager(TFM_REF,DATEOBSERVED_ricostruzione) 

    # 3) e ora associa i valori di densità all'accorpato dai dati_ricostrunione e determina le prime metriche
    # era cosi ACrimappato, tot_km, tot_veicoli, tot_running = rimappa_in_base_all_accorpato(dati_ricostruzione, AC, ora=9)
    # ora aggiornato:
    dati_ricostruzione_v1, tot_km, tot_veicoli, tot_running = analisi_dati_ricostruzione_1(dati_ricostruzione,log_level = 2)

    # 4) mi calcolo il consumo
    # era ACrimappato, consumo_tot = determina_consumo(ACrimappato)
    dati_ricostruzione_v2, consumo_tot = determina_consumo_tfm(dati_ricostruzione_v1)

    # 5) co2
    # era ACrimappato, co2_tot = determina_CO2(ACrimappato)
    dati_ricostruzione_v3, co2_tot = determina_CO2_tfm(dati_ricostruzione_v2)

    # 6) anche le componenti e il funzionale di  traffic state
    #era FRrs, FLrs, HErs, VHrs = kpi_sull_accorpat_con_distanza(ACrimappato)
    # funzionale_traffic_state = 1*FRrs + 2*FLrs + 20*HErs + 30*VHrs
    funzionale_traffic_state, FRrs, FLrs, HErs, VHrs = kpi_traffic_state(dati_ricostruzione)

    return tot_km, tot_veicoli, tot_running , consumo_tot, co2_tot, FRrs, FLrs, HErs, VHrs, funzionale_traffic_state
