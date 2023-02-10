package cx.shapefile.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.interfaces.SpatialSvr;
import cx.shapefile.utils.cx.GisUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;

@Component
public class SpatialSvrImpl implements SpatialSvr
{

    @Override
    public FeatureCollection geoJson2Collection(String geoJson) throws Exception
    {
        DefaultFeatureCollection simpleFeatures = new DefaultFeatureCollection();
        FeatureJSON featureJSON = new FeatureJSON();
        SimpleFeature simpleFeature = null;
        FeatureCollection featureCollection = null;
        try {
            simpleFeature = featureJSON.readFeature(geoJson);
            featureCollection = featureJSON.readFeatureCollection(geoJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (featureCollection == null || featureCollection.isEmpty()) {
            simpleFeatures.add(simpleFeature);
            return simpleFeatures;
        }
        return featureCollection;
    }

    @Override
    public String geoServerWfs(String url, String totalLayerName, String outFormat) throws Exception
    {
        String layerPrefix = totalLayerName.substring(0, totalLayerName.indexOf(":"));
        String output = null;
        if(outFormat.equalsIgnoreCase("shape") || outFormat == "SHAPE-ZIP")
        {
            output = outFormatShape;
        }
        else if(outFormat.equalsIgnoreCase("json") || outFormat == "application/json")
        {
            output = outFormatJSON;
        }
        //wfs完整url路径
        StringBuilder address = new StringBuilder();
        address.append(url + "/" + layerPrefix +"/"+ "wfs?");
        address.append("service=WFS&");
        address.append("version=2.0.0&");
        address.append("request=GetFeature&");
        address.append("typeName="+totalLayerName+"&");
        address.append("outputFormat="+output);
        return address.toString();
    }

    @Override
    public String geoServerWms(JSONObject message) throws Exception
    {
        if(message.containsKey("REQUEST")){
            String request = message.getString("REQUEST");
            if(request.equalsIgnoreCase("GetFeatureInfo")){
                //调用返回GetFeatureInfo的url的方法
            }
        }

        return null;
    }


    @Override
    public Geometry json2Geometry(JSON json) throws Exception
    {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);
        String wkt = GisUtils.jsonToWkt((JSONObject) json);
        Geometry geometry = reader.read(wkt);
        return geometry;
    }

    @Override
    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeaturesCollectionByJson(String geoJSON) throws Exception
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(geoJSON.getBytes());
        GeometryJSON gjson = new GeometryJSON();
        FeatureJSON fjson = new FeatureJSON(gjson);
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = fjson.readFeatureCollection(inputStream);
        return features;
    }
}
