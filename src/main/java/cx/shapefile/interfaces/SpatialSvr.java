package cx.shapefile.interfaces;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.ReferenceIdentifier;


public interface SpatialSvr
{
    String outFormatShape = "SHAPE-ZIP";

    String outFormatJSON = "application/json";

    public JSON geoSpatialAnalyse(FeatureIterator featureIterator, Geometry scope) throws Exception;

    ReferenceIdentifier getSpatialReference(SimpleFeature feature)throws Exception;

    FeatureCollection geoJson2Collection(String geoJson)throws Exception;

    String geoServerWfs(String url, String totalLayerName, String outFormat)throws Exception;

    String geoServerWms(JSONObject message)throws Exception;

    Geometry json2Geometry(JSON json) throws Exception;

    FeatureCollection<SimpleFeatureType, SimpleFeature> getFeaturesCollectionByJson(String geoJSON) throws Exception;
}
