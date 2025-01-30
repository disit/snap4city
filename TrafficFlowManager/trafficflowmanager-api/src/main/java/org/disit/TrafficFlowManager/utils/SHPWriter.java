/* TrafficFlowManager.
   Copyright (C) 2023 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.disit.TrafficFlowManager.utils;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Source: https://gitlab.com/snippets/9275
public class SHPWriter {

    private ShapefileDataStore shpDataStore;

    public SHPWriter(File f) {
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        try {
            params.put("url", f.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            shpDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> features) {

        if (shpDataStore == null) {
            throw new IllegalStateException("Datastore can not be null when writing");
        }
        SimpleFeatureType schema = features.getSchema();
        GeometryDescriptor geom = schema.getGeometryDescriptor();
        String oldGeomAttrib = "";
        try {

            /*
             * Write the features to the shapefile
             */
            Transaction transaction = new DefaultTransaction("create");

            String typeName = shpDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = shpDataStore.getFeatureSource(typeName);

            /*
             * The Shapefile format has a couple limitations: - "the_geom" is always
             * first, and used for the geometry attribute name - "the_geom" must be of
             * type Point, MultiPoint, MuiltiLineString, MultiPolygon - Attribute
             * names are limited in length - Not all data types are supported (example
             * Timestamp represented as Date)
             *
             * Because of this we have to rename the geometry element and then rebuild
             * the features to make sure that it is the first attribute.
             */

            List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();
            GeometryType geomType = null;
            List<AttributeDescriptor> attribs = new ArrayList<>();
            for (AttributeDescriptor attrib : attributes) {
                AttributeType type = attrib.getType();
                if (type instanceof GeometryType) {
                    geomType = (GeometryType) type;
                    oldGeomAttrib = attrib.getLocalName();
                } else {
                    attribs.add(attrib);
                }
            }

            assert geomType != null;
            GeometryTypeImpl gt = new GeometryTypeImpl(new NameImpl("the_geom"), geomType.getBinding(),
                    geomType.getCoordinateReferenceSystem(), geomType.isIdentified(), geomType.isAbstract(),
                    geomType.getRestrictions(), geomType.getSuper(), geomType.getDescription());

            GeometryDescriptor geomDesc = new GeometryDescriptorImpl(gt, new NameImpl("the_geom"), geom.getMinOccurs(),
                    geom.getMaxOccurs(), geom.isNillable(), geom.getDefaultValue());

            attribs.add(0, geomDesc);

            SimpleFeatureType shpType = new SimpleFeatureTypeImpl(schema.getName(), attribs, geomDesc, schema.isAbstract(),
                    schema.getRestrictions(), schema.getSuper(), schema.getDescription());

            shpDataStore.createSchema(shpType);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                List<SimpleFeature> feats = new ArrayList<>();

                FeatureIterator<SimpleFeature> features2 = features.features();
                while (features2.hasNext()) {
                    SimpleFeature f = features2.next();
                    SimpleFeature reType = DataUtilities.reType(shpType, f, true);
                    //set the default Geom (the_geom) from the original Geom
                    reType.setAttribute("the_geom", f.getAttribute(oldGeomAttrib));
                    feats.add(reType);
                }
                features2.close();
                SimpleFeatureCollection collection = new ListFeatureCollection(shpType, feats);

                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
                shpDataStore.dispose();
            } else {
                // Just try again, it should work
                transaction.close();
                writeFeatures(features);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
