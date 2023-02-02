package cx.shapefile.impl;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import cx.shapefile.interfaces.ShapeDeal;
import cx.shapefile.utils.MyUtils;
import cx.shapefile.utils.cx.GisUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Component
public class ShapeDealImpl implements ShapeDeal
{
    @Override
    public Geometry json2Geometry(JSONObject json) throws Exception
    {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);
        String wkt = MyUtils.jsonToWkt(json);
        Geometry geometry = reader.read(wkt);
        return geometry;
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

    /**
     * 获取shape的数据存储对象
     * @param shpPath
     * @return
     */
    @Override
    public ShapefileDataStore getShapeDataStore(String shpPath) throws Exception
    {
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        ShapefileDataStore dataStore = (ShapefileDataStore) factory.createDataStore(new File(shpPath).toURI().toURL());
        return dataStore;
    }

    /**
     * 获取SimpleFeature的迭代器，从中可以拿到SimpleFeature
     * @param dataStore
     * @param charset
     * @return
     */
    @Override
    public SimpleFeatureIterator getSimpleFeatureIterator(ShapefileDataStore dataStore, String charset) throws Exception
    {
        if(charset != null)
        {
            dataStore.setCharset(Charset.forName(charset));
        }
        else
        {
            dataStore.setCharset(Charset.forName("GBK"));
        }
        SimpleFeatureSource featureSource = dataStore.getFeatureSource();
        SimpleFeatureIterator itertor = featureSource.getFeatures().features();
        return itertor;
    }

    /**
     * 获取feature中的属性，并根据set中包换的字段设置field
     */
    @Override
    public ArrayList<Field> setFields(HashSet<String> set, SimpleFeature feature) throws Exception
    {
        // 要素属性信息，名称，值，类型
        List<Property> propertyList = (List<Property>) feature.getValue();
        int i = 0;
        ArrayList<Field> fields = new ArrayList<>();
        for(Property property : propertyList)
        {
            Field field = new Field();
            //判断outFields是不是*，
            if(set.contains("*"))
            {
                String name = new String(property.getName().toString().getBytes("ISO-8859-1"));
                field.setName(name);
                field.setAlias(name);
                field.setId(i++);
                field.setType(property.getType().toString());
                field.setLength(property.getType().getRestrictions().toString().trim());
                fields.add(field);
            }
            else
            {
                if(set.contains(property.getName().toString()))
                {
                    //设置fields得name以ISO-8859-1编码输出
                    String name = new String(property.getName().toString().getBytes("ISO-8859-1"));
                    field.setName(name);
                    field.setAlias(name);
                    field.setId(i++);
                    field.setType(property.getType().toString());
                    field.setLength(property.getType().getRestrictions().toString().trim());
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    @Override
    public Feature setFeature(HashSet<String> set, SimpleFeature feature, Geometry geometry) throws Exception
    {
        Feature tempFeature = new Feature();
        JSONObject jsonObject = GisUtils.wktToJson(geometry.toText());
        tempFeature.setArea(geometry.getArea());
        tempFeature.setCircumference(geometry.getLength());
        tempFeature.setGeometry(jsonObject);
        // 要素对象
        // 要素属性信息，名称，值，类型
        List<Property> propertyList = (List<Property>) feature.getValue();
        ArrayList<HashMap<Object, Object>> lists = new ArrayList<>();
        for(Property property : propertyList)
        {
            if("the_geom".equals(property.getName().toString()))
            {
                continue;
            }
            //判断outFields是不是*，
            if (set.contains("*"))
            {
                //所有的字段都获取
                HashMap<Object, Object> map = new HashMap<>();
                map.put(new String(property.getName().toString().getBytes("ISO-8859-1")),property.getValue().toString());
                lists.add(map);
            }
            else
            {
                //获取outFields内的字段
                if(set.contains(property.getName().toString()))
                {
                    HashMap<Object, Object> map = new HashMap<>();
                    map.put(new String(property.getName().toString().getBytes("ISO-8859-1")),property.getValue().toString());
                    lists.add(map);
                }
            }
        }
        tempFeature.setAttribute(lists);
        // 要素属性信息
        return tempFeature;
    }
}
