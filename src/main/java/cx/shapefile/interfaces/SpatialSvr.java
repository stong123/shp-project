package cx.shapefile.interfaces;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.geotools.feature.FeatureCollection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface SpatialSvr
{


    JSON getWfsFeature(String url, String layerName, String exp) throws Exception;

    Geometry json2Geometry(JSON json) throws Exception;

    FeatureCollection<SimpleFeatureType, SimpleFeature> getFeaturesCollectionByJson(String geoJSON) throws Exception;
}
