package cx.shapefile.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.DisplayFieldName;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
public class GeoAnalyseService
{

    public DisplayFieldName setDisplayFieldName(JSONObject json, String shpFilePath, String method) throws ParseException, IOException {
        DisplayFieldName displayFieldName = new DisplayFieldName();
        //将json转为geometry对象
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        WKTReader reader = new WKTReader(geometryFactory);
        String wkt = jsonToWkt(json);
        Geometry geometry = reader.read(wkt);
        //获取outFields中的数据并存入set中
        Object outFields = json.get("outFields");
        HashSet<String> set = new HashSet<>();
        String[] split = outFields.toString().split(",");
        for (String s : split)
        {
            set.add(s);
        }
        //设置displayFieldName的属性，空间分析
        SetFieldsAndSpatialReference(geometry,displayFieldName,set,shpFilePath,method);
        return displayFieldName;
    }

    public void SetFieldsAndSpatialReference(Geometry geometry, DisplayFieldName displayFieldName, HashSet<String> set,String shpFilePath, String method) throws IOException
    {
        //选择读取的文件
        File shapeFile = new File(shpFilePath);
        if (shapeFile == null)
        {
            return;
        }
        // 读取到数据存储中
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        // 获取特征资源
        SimpleFeatureSource simpleFeatureSource = store.getFeatureSource();
        // 要素集合
        SimpleFeatureCollection simpleFeatureCollection = simpleFeatureSource.getFeatures();
        // 获取要素迭代器
        SimpleFeatureIterator featureIterator = simpleFeatureCollection.features();

        int n = 0;
        ArrayList<Feature> features = new ArrayList<>();
        while(featureIterator.hasNext())
        {
            SimpleFeature feature = featureIterator.next();
            if(n == 0)
            {
                //如果是第一次进入就获取该文件的中文坐标，文件内的属性。
                ArrayList<Field> fields = setFields(set, feature);
                displayFieldName.setFields(fields);
                String spatialReference = getSpatialReference(feature);
                displayFieldName.setSpatialReference(spatialReference);
                n++;
            }
            Geometry geo = (Geometry) feature.getDefaultGeometry();
            Geometry geoIntersectGeo = geo.intersection(geo);
            //叠加分析后的图形数据
            Geometry result = spatialOperation(geoIntersectGeo,geometry,method);
            if(!result.isEmpty())
            {
                System.out.println("area: " + result.getArea());
                System.out.println("geo.area: " + geo.getArea());
                //设置相交图形的属性（文件中可直接获取的）及相交后图形的属性（周长，面积，图形）
                Feature feature1 = setFeature(set, feature, result);
                features.add(feature1);
            }
        }
        displayFieldName.setFeatures(features);
        //关闭文件
        featureIterator.close();
        store.dispose();
        shapeFile.exists();
    }

    /**
     * 从SimpleFeature中获取空间坐标
     * @param feature
     * @return
     */
    public String getSpatialReference(SimpleFeature feature)
    {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        //获取坐标参考系信息
        CoordinateReferenceSystem coordinateReferenceSystem = geometryAttribute.getDescriptor().getCoordinateReferenceSystem();
        //获取空间坐标
        ReferenceIdentifier identifier = coordinateReferenceSystem.getCoordinateSystem().getName();
        return identifier.toString();
    }

    public JSONObject wktToJson(String wkt)
    {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        WKTReader reader = new WKTReader( geometryFactory );
        String json = null;
        JSONObject jsonObject=new JSONObject();
        try
        {
            Geometry geometry = reader.read(wkt);
            StringWriter writer = new StringWriter();
            GeometryJSON geometryJSON=new GeometryJSON();
            geometryJSON.write(geometry,writer);
            json = writer.toString();
            jsonObject = JSONObject.parseObject(json);
        }
        catch (Exception e)
        {
            System.out.println("WKT转GeoJson出现异常");
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String jsonToWkt(JSONObject jsonObject)
    {
        String wkt = null;
        String type = jsonObject.getString("type");
        GeometryJSON gJson = new GeometryJSON();
        try
        {
            // {"geometries":[{"coordinates":[4,6],"type":"Point"},{"coordinates":[[4,6],[7,10]],"type":"LineString"}],"type":"GeometryCollection"}
            if("GeometryCollection".equals(type))
            {
                // 由于解析上面的json语句会出现这个geometries属性没有采用以下办法
                JSONArray geometriesArray = jsonObject.getJSONArray("geometries");
                // 定义一个数组装图形对象
                int size = geometriesArray.size();
                Geometry[] geometries=new Geometry[size];
                for (int i=0;i<size;i++)
                {
                    String str = geometriesArray.get(i).toString();
                    // 使用GeoUtil去读取str
                    Reader reader = GeoJSONUtil.toReader(str);
                    Geometry geometry = gJson.read(reader);
                    geometries[i]=geometry;
                }
                GeometryCollection geometryCollection = new GeometryCollection(geometries,new GeometryFactory());
                wkt=geometryCollection.toText();
            }
            else
            {
                Reader reader = GeoJSONUtil.toReader(jsonObject.toString());
                Geometry read = gJson.read(reader);
                wkt=read.toText();
            }
        }
        catch (IOException e)
        {
            System.out.println("GeoJson转WKT出现异常");
            e.printStackTrace();
        }
        return wkt;
    }

    /**
     * 叠加分析类型包括：交叉分析（Intersection）联合分析（Union）差异分析（Difference）对称差异分析（SymDifference）
     * @param geo1
     * @param geo2
     * @param operate
     * @return
     */
    public Geometry spatialOperation (Geometry geo1, Geometry geo2, String operate)
    {
        switch (operate)
        {
        case "intersection":    //交叉分析
            return geo1.intersection(geo2);
        case "difference":      //差异分析
            return geo1.difference(geo2);
        case "symDifference":   //对称差异分析
            return geo1.symDifference(geo2);
        case "union":           //联合分析
            return geo1.union(geo2);
        }
        return null;
    }

    /**
     * 根据geometry中的空间信息，以及feature中的要素信息，返回一个新Feature
     * @param set
     * @param feature
     * @param geometry
     * @return
     * @throws UnsupportedEncodingException
     */
    private Feature setFeature(HashSet<String> set, SimpleFeature feature, Geometry geometry) throws UnsupportedEncodingException
    {
        Feature tempFeature = new Feature();
        JSONObject jsonObject = wktToJson(geometry.toText());
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

    /**
     * 获取feature中的属性，并根据set中包换的字段设置field
     * @param set
     * @param feature
     * @return
     * @throws UnsupportedEncodingException
     */
    private ArrayList<Field> setFields(HashSet<String> set, SimpleFeature feature) throws UnsupportedEncodingException
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
}
