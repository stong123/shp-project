package cx.shapefile.interfaces;

import com.alibaba.fastjson.JSON;
import org.geotools.feature.FeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;


public interface SpatialSvr
{
    String outFormatShape = "SHAPE-ZIP";

    String outFormatJSON = "application/json";

    FeatureCollection geoJson2Collection(String geoJson)throws Exception;

    String geoServerWfs(String url, String totalLayerName, String outFormat)throws Exception;

    Geometry json2Geometry(JSON json) throws Exception;

    FeatureCollection<SimpleFeatureType, SimpleFeature> getFeaturesCollectionByJson(String geoJSON) throws Exception;
}
