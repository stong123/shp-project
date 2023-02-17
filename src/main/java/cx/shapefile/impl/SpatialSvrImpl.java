package cx.shapefile.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.DisplayFieldName;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import cx.shapefile.interfaces.ShapeDeal;
import cx.shapefile.interfaces.SpatialAnalyse;
import cx.shapefile.interfaces.SpatialSvr;
import cx.shapefile.utils.cx.GisUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

@Component
public class SpatialSvrImpl implements SpatialSvr
{
    @Autowired
    ShapeDeal shapeDeal;
    @Autowired
    SpatialAnalyse spatialAnalyse;

    /**
     * 通过shp文件或geojson拿到FeatureIterator，然后通过遍历器拿到Feature，根据Feature进行空间分析
     * @param featureIterator
     * @param scope 所要分析的范围
     * @return
     * @throws Exception
     */
    @Override
    public JSON geoSpatialAnalyse(FeatureIterator featureIterator, Geometry scope) throws Exception
    {
        DisplayFieldName displayField = new DisplayFieldName();
        ArrayList<Feature> features = new ArrayList<>();
        boolean flag = false;
        while(featureIterator.hasNext())
        {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();
            if(!flag)
            {
                //如果是第一次进入就获取该文件的中文坐标，文件内的属性。
                ArrayList<Field> fields = shapeDeal.setFields(feature);
                displayField.setFields(fields);
                String spatialReference = String.valueOf(getSpatialReference(feature));
                displayField.setSpatialReference(spatialReference);
                flag = true;
            }
            Geometry geo = (Geometry) feature.getDefaultGeometry();
            Geometry geoIntersectGeo = geo.intersection(geo);
            //叠加分析后的图形数据
            Geometry result = spatialAnalyse.spatialAnalyse(geoIntersectGeo,scope, SpatialAnalyse.Intersection);
            if(!result.isEmpty())
            {
                //设置相交图形的属性（文件中可直接获取的）及相交后图形的属性（周长，面积，图形）
                Feature feature1 = shapeDeal.setFeature(feature, result);
                features.add(feature1);
            }
        }
        displayField.setFeatures(features);
        //关闭文件
        featureIterator.close();

        return (JSON) JSONObject.toJSON(displayField);
    }

    /**
     * 从SimpleFeature中获取空间坐标
     * @param feature
     * @return
     * @throws Exception
     */
    @Override
    public ReferenceIdentifier getSpatialReference(SimpleFeature feature)
    {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        //获取坐标参考系信息
        CoordinateReferenceSystem coordinateReferenceSystem = geometryAttribute.getDescriptor().getCoordinateReferenceSystem();
        //获取空间坐标
        ReferenceIdentifier identifier = coordinateReferenceSystem.getCoordinateSystem().getName();
        return identifier;
    }

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
