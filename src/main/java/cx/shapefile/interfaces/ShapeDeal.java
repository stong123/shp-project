package cx.shapefile.interfaces;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.ReferenceIdentifier;

import java.util.ArrayList;
import java.util.HashSet;

public interface ShapeDeal
{
    Geometry json2Geometry(JSONObject json) throws Exception;

    ReferenceIdentifier getSpatialReference(SimpleFeature feature);

    ShapefileDataStore getShapeDataStore(String shpPath) throws Exception;

    SimpleFeatureIterator getSimpleFeatureIterator(ShapefileDataStore dataStore, String charset) throws Exception;

    ArrayList<Field> setFields(HashSet<String> set, SimpleFeature feature) throws Exception;

    Feature setFeature(HashSet<String> set, SimpleFeature feature, Geometry geometry)throws Exception;

}
