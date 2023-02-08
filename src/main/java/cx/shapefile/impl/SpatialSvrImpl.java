package cx.shapefile.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import cx.shapefile.interfaces.SpatialSvr;
import cx.shapefile.utils.cx.GisUtils;
import cx.shapefile.utils.cx.SvrUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Component
public class SpatialSvrImpl implements SpatialSvr
{
    @Override
    public JSON getWfsFeature(String url, String layerName, String exp) throws Exception
    {
        StringBuilder address = new StringBuilder();
        address.append(url+"?");
        address.append("service=wfs&version=2.0.0&request=GetFeature&outputFormat=json&");
        address.append("typeNames="+layerName);
        if (exp != null)
        {
            address.append("&"+exp);
        }
        System.out.println(address);
        byte[] bytes = SvrUtils.getRequest(address.toString());
        JSONObject jsonObject = (JSONObject)JSONObject.parse(bytes, 0, bytes.length, StandardCharsets.UTF_8.newDecoder(), new Feature[0]);
        return jsonObject;
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
