''' Snap4city GTFS processor.
   Copyright (C) 2021 DISIT Lab http://www.disit.org - University of Florence
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

from zipfile import ZipFile
import pandas as pd
import os, pytz, datetime, argparse

import requests

import json, flask, sys


class ChouettePublisher:
    _api_directory = '/api/v1/datas/'

    _route_type = {
        0: "<http://vocab.gtfs.org/terms#LightRail>",
        1: "<http://vocab.gtfs.org/terms#Subway>",
        2: "<http://vocab.gtfs.org/terms#Rail>",
        3: "<http://vocab.gtfs.org/terms#Bus>",
        4: "<http://vocab.gtfs.org/terms#Ferry>",
        5: "<http://vocab.gtfs.org/terms#CableCar>",
        6: "<http://vocab.gtfs.org/terms#Gondola>",
        7: "<http://vocab.gtfs.org/terms#Funicular>"
    }

    def __init__(self, url: str = "chouette.snap4city.org", publication_api_namespace: str = None,
                 publication_api_key: str = None, output_dir: str = None):
        self.chouette_url = url
        self.publication_api = publication_api_namespace
        self.publication_api_key = publication_api_key
        if output_dir is None:
            output_dir = os.getcwd() + os.sep + 'output'
        else:
            output_dir = os.path.abspath(output_dir)
        os.makedirs(output_dir, exist_ok=True)
        self.output_directory = output_dir

    def __str__(self):
        return """Source URL: %s\n Publication Name: %s\n Publication Key: %s\n Output directory: %s""" \
            % (self.chouette_url, self.publication_api, self.publication_api_key, self.output_directory)

    def set_url(self, url: str):
        self.chouette_url = url

    def set_publication_name(self, name: str):
        self.publication_api = name

    def set_publication_key(self, key: str):
        self.publication_api_key = key

    def set_output_directory(self, output_dir: str = None):
        if output_dir is None:
            output_dir = os.getcwd() + os.sep + 'output'
        else:
            output_dir = os.path.abspath(output_dir)
        os.makedirs(output_dir, exist_ok=True)
        self.output_directory = output_dir

    def _extract_agencies_triples(self, entry_name):
        agency = pd.read_csv(self.output_directory + os.sep + 'agency.txt', delimiter=',')
        print("Write triples for agency")
        f = open(self.output_directory + os.sep + "agency.n3", "w+")
        for i in range(len(agency["agency_id"])):
            # semantic subject
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Agency_" + str(
                agency["agency_id"][i]).replace(" ", "") + \
                         ">"

            f.write(first_term + " <http://purl.org/dc/terms/identifier> " +
                    '"' + entry_name + '_' + str(agency["agency_id"][i]).replace(" ", "") + '" .\n')
            f.write(first_term + " <http://xmlns.com/foaf/0.1/name> " + '"' + str(agency["agency_name"][i]).replace(" ", "") + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#timeZone> " + '"' + str(
                    agency["agency_timezone"][i]) + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#fareUrl> " + '"' + str(agency["agency_url"][i]) + '" .\n')
            f.write(
                first_term + " <http://purl.org/dc/terms/language> " + '"' + str(agency["agency_lang"][i]) + '" .\n')
            f.write(
                first_term +
                " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Agency> . \n")
            f.write("\n")
        f.close()
        return first_term

    def _extract_calendar_dates_triples(self, entry_name):
        #check if calendar file exists
        calendar_file, read_file = None, None
        if os.path.exists(self.output_directory + os.sep + 'calendar.txt'):
            calendar_file = pd.read_csv(self.output_directory + os.sep + 'calendar.txt')

        if calendar_file is not None:
            read_file = pd.read_csv(self.output_directory + os.sep + 'calendar_dates.txt')
            #clear exception_type 2 (removed service)
            read_file = read_file[read_file['exception_type'] != 2]
            read_file.reset_index(drop=True, inplace=True)
            #add the dates to perform the service changes
            for service_id in calendar_file['service_id'].to_numpy():
                #read from the calendar file
                row = calendar_file.loc[calendar_file['service_id']==service_id]
                date_from = str(row['start_date'].values[0]) 
                date_to = str(row['end_date'].values[0])
                # Convert the string dates to datetime objects
                start_date = datetime.datetime.strptime(date_from, '%Y%m%d')
                end_date = datetime.datetime.strptime(date_to, '%Y%m%d')
                # Generate an array of date strings
                date_array = [(start_date + datetime.timedelta(days=i)).strftime('%Y%m%d') for i in range((end_date - start_date).days + 1)]
                for date in date_array:
                    # Convert the string to a datetime object
                    date_obj = datetime.datetime.strptime(date, '%Y%m%d')
                    # Get the day name and convert it to lowercase
                    day_name = date_obj.strftime('%A').lower()
                    #check what day is (mon, tue, ...)
                    if(row[day_name].values[0] == 1): 
                        query = f"date == {date}"
                        row_changed = not read_file.query(query).empty
                        # if not is already added, add it to the df
                        if not row_changed:
                            read_file.loc[len(read_file)] = [service_id, int(date), 1]
            
        else:
            read_file = pd.read_csv(self.output_directory + os.sep + 'calendar_dates.txt')                

        read_file.to_csv(r'calendar_dates.csv', index=None)
        calendar = pd.read_csv("calendar_dates.csv", delimiter=",")
        print("Write triples for calendar_dates")
        f = open(self.output_directory + os.sep + "calendar_dates.n3", "w+")
        for i in range(len(calendar["service_id"])):
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Service_" + str(
                calendar["service_id"][i]) + ">"
            date = str(calendar["date"][i])[0:4] + "-" + str(calendar["date"][i])[4:6] + "-" + str(calendar["date"][i])[
                                                                                               6:8]

            f.write(first_term + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_Service_" +
                    str(calendar["service_id"][i]) + '" .\n')
            f.write(
                first_term + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Service> .\n")
            f.write(first_term + " <http://purl.org/dc/terms/date>" + '"' + date + '" .\n')
            f.write("\n")
        f.close()

    def _extract_stop_triples(self, entry_name, agency, km4c_class):
        read_file = pd.read_csv(self.output_directory + os.sep + 'stops.txt')
        read_file.to_csv(r'stops.csv', index=None)
        stops = pd.read_csv("stops.csv", delimiter=",")
        print("Write triples for stops")
        f = open(self.output_directory + os.sep + 'stops.n3', "w+")
        for i in range(len(stops["stop_id"])):
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Stop_" + str(
                stops["stop_id"][i]) + ">"

            f.write(first_term + " <http://vocab.gtfs.org/terms#agency> " + agency + ' .\n')
            f.write(
                first_term + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_Stop_" + str(
                    stops["stop_id"][
                        i]) + '" .\n')
            f.write(first_term + " <http://vocab.gtfs.org/terms#code> " + '"' + str(stops["stop_code"][i]) + '" .\n')

            f.write(first_term + " <http://www.w3.org/2003/01/geo/wgs84_pos#long> " + '"' + str(
                stops["stop_lon"][i]) + '"^^<http://www.w3.org/2001/XMLSchema#float> .\n')
            f.write(first_term + " <http://www.w3.org/2003/01/geo/wgs84_pos#lat> " + '"' + str(
                stops["stop_lat"][i]) + '"^^<http://www.w3.org/2001/XMLSchema#float> .\n')
            f.write(first_term + " <http://www.w3.org/2003/01/geo/wgs84_pos#geometry> " + '"POINT(' + str(
                stops["stop_lon"][i]) + ' ' + str(
                stops["stop_lat"][i]) + ')"^^<http://www.openlinksw.com/schemas/virtrdf#Geometry> .\n')

            f.write(
                first_term + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Stop> .\n")
            f.write(first_term + " <http://xmlns.com/foaf/0.1/name> " + '"' +
                    str(stops["stop_name"][i]).replace('"', r'\"') + '" .\n')
            f.write(
                first_term + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.disit.org/km4city/schema#" + km4c_class + "> .\n")
            f.write("\n")
        f.close()

    def _extract_stop_times_triples(self, entry_name):
        read_file = pd.read_csv(self.output_directory + os.sep + 'stop_times.txt')
        read_file.to_csv(r'stop_times.csv', index=None)
        stop_times = pd.read_csv("stop_times.csv", delimiter=",")
        print("Write triples for stop_times")
        f = open(self.output_directory + os.sep + 'StopTimes.n3', "w+")
        for i in range(len(stop_times["trip_id"])):
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_StopTime_" + str(
                stop_times["trip_id"][
                    i]) + "_" + str(stop_times["stop_sequence"][i]).zfill(2) + "> "
            first_term_var = "<http://www.disit.org/km4city/resource/" + entry_name + "_Trip_" + str(
                stop_times["trip_id"][
                    i]) + "> "
            first_term_var2 = "<http://www.disit.org/km4city/resource/" + entry_name + "_Stop_" + str(
                stop_times["stop_id"][
                    i]) + ">"

            f.write(first_term + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_StopTime_" +
                    str(stop_times["trip_id"][i]) + "_" + str(stop_times["stop_sequence"][i]).zfill(2) + '" .\n')
            f.write(
                first_term + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#StopTime> .\n")
            f.write(
                first_term_var + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Trip> .\n")
            f.write(first_term + " <http://vocab.gtfs.org/terms#trip> " + first_term_var + " .\n")
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#departureTime> " + '"' + str(stop_times["departure_time"][
                                                                                             i]) + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#stop> <http://www.disit.org/km4city/resource/" + entry_name + "_Stop_" +
                str(stop_times["stop_id"][i]) + "> .\n")
            f.write(first_term + " <http://vocab.gtfs.org/terms#stopSequence> " + '"' + str(
                stop_times["stop_sequence"][i]).zfill(2) + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#arrivalTime> " + '"' + str(stop_times["arrival_time"][
                                                                                           i]) + '" .\n')
            f.write(first_term_var + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_Trip_" +
                    str(stop_times["trip_id"][i]) + '" .\n')
            f.write(
                first_term_var2 + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Stop> .\n")
            f.write(first_term_var2 + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_Stop_" +
                    str(stop_times["stop_id"][i]) + '" .\n')
            f.write("\n")
        f.close()

    def _extract_trips_triples(self, entry_name):
        trips = pd.read_csv(self.output_directory + os.sep + "trips.txt", delimiter=",", low_memory=True)
        print("Write triples for trips")
        f = open(self.output_directory + os.sep + "trips.n3", "w+")
        for i in range(len(trips["route_id"])):
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Trip_" + str(
                trips["trip_id"][i]) + ">"
            first_term_var = "<http://www.disit.org/km4city/resource/" + entry_name + "_Service_" + \
                             str(trips["service_id"][
                                     i]) + ">"
            first_term_var2 = "<http://www.disit.org/km4city/resource/" + entry_name + "_Shape_" + str(
                trips["shape_id"][i]) + ">"
            first_term_var3 = "<http://www.disit.org/km4city/resource/" + entry_name + "_Route_" + str(
                trips["route_id"][i]) + ">"
            short_name = str(trips["trip_short_name"][i])
            if short_name == "nan":
                short_name = "NULL"

            f.write(
                first_term + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Trip> .\n")
            f.write(first_term_var + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_Service_" +
                    str(trips["service_id"][i]) + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#route> <http://www.disit.org/km4city/resource/" + entry_name + "_Route_" + str(
                    trips["route_id"][i]) + "> .\n")
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#direction> " + '"' + str(
                    trips["direction_id"][i]) + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#headsign> " + '"' + str(
                    trips["trip_headsign"][i]).replace('"', r'\"') + '" .\n')
            f.write(first_term_var2 + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + "_Shape_" + str(
                trips["shape_id"][i]) + '" .\n')
            f.write(
                first_term + " <http://www.opengis.net/ont/geosparql#hasGeometry> <http://www.disit.org/km4city/resource/" + entry_name + "_Shape_" + str(
                    trips["shape_id"][i]) + "> .\n")
            f.write(
                first_term_var + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Service> .\n")
            f.write(first_term + ' <http://vocab.gtfs.org/terms#shortName> "' + short_name + '" .\n')
            f.write(
                first_term_var3 + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Route> .\n")
            f.write(
                first_term + ' <http://purl.org/dc/terms/identifier> "' + entry_name + "_Trip_" + str(trips["trip_id"][
                                                                                                          i]) + '" .\n')
            f.write(
                first_term + " <http://vocab.gtfs.org/terms#service> <http://www.disit.org/km4city/resource/" + entry_name + "_Service_" +
                str(trips["service_id"][i]) + "> .\n")
            f.write(first_term_var3 + ' <http://purl.org/dc/terms/identifier> "' + entry_name + "_Route_" + str(
                trips["route_id"][i]) + '". \n')
            f.write(
                first_term_var2 + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.opengis.net/ont/geosparql#Geometry> .\n\n")

            f.write("\n")
        f.close()

    def _extract_shapes_triples(self, entry_name):
        shapes = pd.read_csv(self.output_directory + os.sep + 'shapes.txt', delimiter=',', low_memory=True, dtype=str)
        print("Write triples for shapes")
        shape = dict()
        for i in range(len(shapes['shape_id'])):
            if shapes['shape_id'][i] not in shape.keys():
                shape.update({shapes['shape_id'][i]: ""})
            shape[shapes['shape_id'][i]] += """, %s %s""" % (shapes['shape_pt_lon'][i], shapes['shape_pt_lat'][i])

        f = open(self.output_directory + os.sep + 'shapes.n3', 'w+')
        for element in shape.keys():
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Shape_" + str(element) + ">"
            # identifier
            f.write(first_term + " <http://purl.org/dc/terms/identifier> " + '"' + entry_name + '_Shape_' + str(
                element) + '" .\n')
            # syntax_type
            f.write(first_term + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " +
                    "<http://www.opengis.net/ont/geosparql#Geometry> .\n")
            # shape points
            f.write(first_term + ' <http://www.opengis.net/ont/geosparql#asWKT> "LINESTRING((' +
                    shape[element][2:] + '))" .\n\n')
        f.close()

    def _extract_routes_triples(self, entry_name):
        routes = pd.read_csv(self.output_directory + os.sep + "routes.txt", delimiter=",", low_memory=True)
        print("Write triples for routes")
        f = open(self.output_directory + os.sep + "routes.n3", "w+")
        for i in range(len(routes['route_id'])):
            first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Route_" + str(
                routes["route_id"][i]) + ">"
            agency_first_term = "<http://www.disit.org/km4city/resource/" + entry_name + "_Agency_" + \
                                str(routes["agency_id"][i]).replace(" ", "") + ">"
            short_name = str(routes["route_short_name"][i])
            if short_name == "nan":
                short_name = ""

            f.write(first_term + """ <http://purl.org/dc/terms/identifier> "%s_Route_%s" . \n"""
                    % (entry_name, str(routes['route_id'][i])))
            f.write("""%s <http://vocab.gtfs.org/terms#color> "%s" .\n""" % (first_term, routes['route_color'][i]))
            f.write(
                """%s <http://vocab.gtfs.org/terms#textColor> "%s" .\n""" % (first_term, routes['route_text_color'][i]))
            f.write("""%s <http://vocab.gtfs.org/terms#longName> "%s" .\n""" % (
                first_term, str(routes['route_long_name'][i]).replace('"', r'\"')))
            f.write("""%s <http://vocab.gtfs.org/terms#shortName> "%s" . \n""" % (
                first_term, short_name.replace('"', r'\"')))
            f.write("""%s <http://purl.org/dc/terms/identifier> "%s_Agency_%s" . \n""" % (agency_first_term, entry_name,
                                                                                          str(routes['agency_id'][i]).replace(" ", "")))
            f.write("""%s <http://vocab.gtfs.org/terms#agency> %s .\n""" % (first_term, agency_first_term))
            f.write("""%s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Agency> .\n"""
                    % agency_first_term)
            f.write("""%s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#Route> .\n"""
                    % first_term)
            if (id := int(routes['route_type'][i])) in range(0, 8):
                # id definiti tra 0 e 7, vedi _route_type
                f.write(
                    """%s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.gtfs.org/terms#RouteType> . \n"""
                    % self._route_type[id])
                f.write("""%s <http://vocab.gtfs.org/terms#routeType> %s . \n\n""" % (first_term, self._route_type[id]))

    def _extract_triple(self, entry_name, file_path, km4c_class, save_original: bool = False):
        # per testare un file di prova decommentare sotto
        # file_path = os.path.abspath('Bus_cttnordlucca-2.gtfs')
        print(file_path)
        with ZipFile(file_path, 'r') as zip_obj:
            zip_obj.extractall(self.output_directory)
        agency = self._extract_agencies_triples(entry_name)
        self._extract_calendar_dates_triples(entry_name)
        self._extract_stop_triples(entry_name, agency, km4c_class)
        self._extract_stop_times_triples(entry_name)
        self._extract_trips_triples(entry_name)
        self._extract_shapes_triples(entry_name)
        self._extract_routes_triples(entry_name)

        data_version = """<http://www.disit.org/km4city/resource/%s> <http://purl.org/dc/terms/date> "%s"^^<http://www.w3.org/2001/XMLSchema#dateTime> .
        """ % (entry_name, datetime.datetime.now(pytz.utc).isoformat())
        f = open(self.output_directory + os.sep + "dataset_version.n3", 'w+')
        f.write(data_version)
        f.close()

        if os.path.exists(self.output_directory + os.sep + "agency.txt"):
            os.remove(self.output_directory + os.sep + "agency.txt")

        if os.path.exists(self.output_directory + os.sep + "calendar_dates.txt"):
            os.remove(self.output_directory + os.sep + "calendar_dates.txt")

        if os.path.exists(self.output_directory + os.sep + "calendar.txt"):
            os.remove(self.output_directory + os.sep + "calendar.txt")

        if os.path.exists(self.output_directory + os.sep + "routes.txt"):
            os.remove(self.output_directory + os.sep + "routes.txt")

        if os.path.exists(self.output_directory + os.sep + "shapes.txt"):
            os.remove(self.output_directory + os.sep + "shapes.txt")

        if os.path.exists(self.output_directory + os.sep + "stop_times.txt"):
            os.remove(self.output_directory + os.sep + "stop_times.txt")

        if os.path.exists(self.output_directory + os.sep + "stops.txt"):
            os.remove(self.output_directory + os.sep + "stops.txt")

        if os.path.exists(self.output_directory + os.sep + "transfers.txt"):
            os.remove(self.output_directory + os.sep + "transfers.txt")

        if os.path.exists(self.output_directory + os.sep + "trips.txt"):
            os.remove(self.output_directory + os.sep + "trips.txt")

        if not save_original:
            os.remove(file_path)

    def get_triples(self, entry_name: str = None, km4c_class: str = 'BusStop', save_original: bool = False):
        if entry_name is None:
            entry_name = self.publication_api

        if self.publication_api != '':
            url = self.chouette_url + self._api_directory + self.publication_api + "/gtfs.zip"
        else:
            url = self.chouette_url

        print(url)

        payload = {}
        headers = {}
        if self.publication_api_key != '':
            headers['Authorization'] = 'Token token=' + self.publication_api_key

        response = requests.request("GET", url, headers=headers, data=payload).content
        file_path = self.output_directory + os.sep + entry_name + '_publication.gtfs'

        f = open(file_path, 'wb+')
        f.write(response)
        f.close()

        return self._extract_triple(entry_name, file_path, km4c_class, save_original)


app = flask.Flask(__name__)

os.environ["FLASK_APP"] = __name__ + ".py"


@app.route('/chouette2n3', methods=['GET', 'POST'])
def chouette2n3():
    try:
        if flask.request.method == 'GET':
            api_namespace = flask.request.values.get('api_namespace')
            if api_namespace == None:
                return "missing api_namespace"
            api_key = flask.request.values.get('api_key')
            if api_key == None:
                return "missing api_key"
            chouette_url = flask.request.values.get('chouette_url')
            if chouette_url == None:
                chouette_url = "https://chouette.enroute.mobi"
            entry_name = flask.request.values.get('entry_name')
            if entry_name == None:
                entryname = api_namespace

            print("params: ", api_namespace, " ", api_key, " ", chouette_url, " ", entry_name)

            publisher = ChouettePublisher(chouette_url, api_namespace, api_key, '/data/gtfs-triples/' + api_namespace)
            print(publisher)
            publisher.get_triples(entry_name, True)

            return "done"
    except Exception as e:
        print("Error: " + str(e))
        message = "Error: " + str(e);
        return message


@app.route('/gtfs2n3', methods=['GET', 'POST'])
def gtfs2n3(gtfs_url=None, entry_name=None, km4c_class=None):
    try:
        if gtfs_url == None:
            if flask.request.method == 'GET':
                gtfs_url = flask.request.values.get('gtfs_url')
                if gtfs_url == None:
                    return "missing gtfs_url"
                entry_name = flask.request.values.get('entry_name')
                if entry_name == None:
                    return "missing entry_name"
                km4c_class = flask.request.values.get('km4c_class')
                if km4c_class == None:
                    km4c_class = 'BusStop'
        print("params: ", gtfs_url, " ", entry_name)

        data_dir = os.getenv('DATA_DIR', './data') + '/gtfs-triples/' + entry_name
        publisher = ChouettePublisher(gtfs_url, '', '', data_dir)
        print(publisher)
        publisher.get_triples(entry_name, km4c_class, True)

        if(os.getenv('VIRTUOSO_HOST')!=None):
            os.system("bash ./virtuoso-update-graph.sh "+data_dir+" " + os.getenv('VIRTUOSO_HOST') + " urn:gtfs:"+entry_name)

        return "{\"result\":\"done\",\"entry_name\":\"" + entry_name + "\"}"

    except Exception as e:
        print("Error: " + str(e))
        message = "Error: " + str(e);
        return message


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Interface for connecting to Enroute Chouette application and exporting '
                    'publications in triples format.')

    parser.add_argument('-action', metavar='ACTION', type=str, choices=['gtfs2n3', 'chouette', 'flask'],
                        default='flask',
                        help='Action to perform: \n'
                             '  gtfs2n3 -> generate from gtfs url\n'
                             '  chouette -> generate from chouette url')

    args, remaining_argv = parser.parse_known_args()

    parser = argparse.ArgumentParser()


    if args.action == "chouette":

        parser.add_argument('api_namespace', metavar='API_SHORT_NAME', type=str,
                            help='The short name of the publications API (can be found in '
                                 'Settings > Publications inside Chouette)')

        parser.add_argument('api_key', metavar='API_KEY', type=str,
                            help='The API key, can be created in Settings > Publication APIs inside Chouette')

        parser.add_argument('url', metavar='CHOUETTE_URL', default='chouette.snap4city.org', type=str, nargs='?',
                            help='The URL of the publications source. Default connects to "chouette.snap4city.org"')

        parser.add_argument('-s', '--save', action='store_true',
                            help='This flag can be used to preserve the original GTFS export inside OUTPUT_FOLDER')

        parser.add_argument('-e', '--entry-name', type=str, default=None,
                            help='The name for the extracted data entries. Default API_SHORT_NAME')

        parser.add_argument('-o', '--output-directory', type=str, default=os.path.join(os.curdir, 'output'),
                            help='The output directory of the triples. Default is inside the current directory.')

    elif args.action == "gtfs2n3":

        parser.add_argument('url', metavar='GTFS_URL', type=str, nargs='?',
                            help='The URL of the gtfs publications source.')
        parser.add_argument('entry_name', metavar='ENTRY-NAME', type=str, nargs='?',
                            help='Name of the entry')

        parser.add_argument('-km4c_class', metavar='KM4C_CLASS', default="BusStop", type=str,
                            help='Name of the km4c_class. Defatults to "BusStop"')

    else:

        parser.add_argument("-port", metavar='FLASK_PORT', type=int, default=8080, help='flask server run port')

    args = parser.parse_args(remaining_argv, namespace=args)
    print(args)

    if args.action == "gtfs2n3":
        gtfs2n3(args.url, args.entry_name, args.km4c_class)
    elif args.action == "chouette":
        publisher = ChouettePublisher(args.url, args.api_namespace, args.api_key, args.output_directory)
        print(publisher)
        publisher.get_triples(args.entry_name, args.save)
    else:
        app.run(host='0.0.0.0', port=args.port)
