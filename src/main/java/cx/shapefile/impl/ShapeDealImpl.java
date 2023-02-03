package cx.shapefile.impl;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import cx.shapefile.interfaces.ShapeDeal;
import cx.shapefile.utils.MyUtils;
import cx.shapefile.utils.cx.GisUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ShapeDealImpl implements ShapeDeal
{
    @Value("${shapefile.dir}")
    private String shpFileRecourseDir;

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

    @Override
    public String getFeaturesCollectionByJson(String geoJSON) throws Exception
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(geoJSON.getBytes());
        GeometryJSON gjson = new GeometryJSON();
        FeatureJSON fjson = new FeatureJSON(gjson);
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = fjson.readFeatureCollection(inputStream);
        String shpFilePath = featureCollectionToShp(features);
        return shpFilePath;
    }

    @Override
    public String featureCollectionToShp(FeatureCollection<SimpleFeatureType, SimpleFeature> features) throws Exception
    {
        // convert schema for shapefile
        SimpleFeatureType  schema = features.getSchema();
        GeometryDescriptor geom   = schema.getGeometryDescriptor();
        // geoJson文件属性
        List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();
        // geoJson文件空间类型（必须在第一个）
        GeometryType geomType = null;
        List<AttributeDescriptor> attribs = new ArrayList<>();
        for (AttributeDescriptor attrib : attributes)
        {
            AttributeType type = attrib.getType();
            if (type instanceof GeometryType)
            {
                geomType = (GeometryType) type;
            }
            else
            {
                attribs.add(attrib);
            }
        }
        if (geomType == null)
        {
            throw new RuntimeException();
        }
        // 使用geomType创建gt
        GeometryTypeImpl gt = new GeometryTypeImpl(new NameImpl("the_geom"), geomType.getBinding(),
                geom.getCoordinateReferenceSystem() == null ? DefaultGeographicCRS.WGS84 : geom.getCoordinateReferenceSystem(), // 用户未指定则默认为wgs84
                geomType.isIdentified(), geomType.isAbstract(), geomType.getRestrictions(),
                geomType.getSuper(), geomType.getDescription());

        // 创建识别符
        GeometryDescriptor geomDesc = new GeometryDescriptorImpl(gt, new NameImpl("the_geom"), geom.getMinOccurs(),
                geom.getMaxOccurs(), geom.isNillable(), geom.getDefaultValue());

        // the_geom 属性必须在第一个
        attribs.add(0, geomDesc);

        SimpleFeatureType outSchema = new SimpleFeatureTypeImpl(schema.getName(), attribs, geomDesc, schema.isAbstract(),
                schema.getRestrictions(), schema.getSuper(), schema.getDescription());
        List<SimpleFeature> outFeatures = new ArrayList<>();
        try (FeatureIterator<SimpleFeature> features2 = features.features())
        {
            while (features2.hasNext())
            {
                SimpleFeature f = features2.next();
                SimpleFeature reType = DataUtilities.reType(outSchema, f, true);

                reType.setAttribute(outSchema.getGeometryDescriptor().getName(),
                        f.getAttribute(schema.getGeometryDescriptor().getName()));

                outFeatures.add(reType);
            }
        }
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = simpleDateFormat.format(date);

        File dir = new File(shpFileRecourseDir+fileName);
        if(!dir.exists())
        {
            dir.mkdir();
        }
        String shpPath = dir.getPath()+"\\"+fileName+".shp";
        saveFeaturesToShp(outFeatures, outSchema, shpPath);
        return shpPath;
    }

    @Override
    public boolean saveFeaturesToShp(List<SimpleFeature> features, SimpleFeatureType TYPE, String shpPath) throws Exception
    {
        try
        {
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            File shpFile = new File(shpPath);
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", shpFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.setCharset(StandardCharsets.UTF_8);

            newDataStore.createSchema(TYPE);

            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore)
            {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try
                {
                    featureStore.addFeatures(collection);
                    generateCpgFile(shpPath, StandardCharsets.UTF_8);
                    transaction.commit();
                }
                catch (Exception problem)
                {
                    problem.printStackTrace();
                    transaction.rollback();
                }
                finally
                {
                    transaction.close();
                }
            }
            else
            {
                System.out.println(typeName + " does not support read/write access");
            }
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean generateCpgFile(String filePath, Charset charset) throws Exception
    {
        try
        {
            File file = new File(filePath);
            if (!file.exists())
            {
                return false;
            }
            String tempPath = file.getPath();
            int index = tempPath.lastIndexOf('.');
            String name = tempPath.substring(0, index);
            String cpgFilePath = name + ".cpg";
            File cpgFile = new File(cpgFilePath);
            if (cpgFile.exists())
            {
                return true;
            }
            boolean newFile = cpgFile.createNewFile();
            if (newFile)
            {
                Files.write(cpgFile.toPath(), charset.toString().getBytes(charset));
            }
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
