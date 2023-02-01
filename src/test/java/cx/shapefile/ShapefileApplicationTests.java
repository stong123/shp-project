package cx.shapefile;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.utils.CommonMethod;
import cx.shapefile.utils.MyGeoTools;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.ClippedFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class ShapefileApplicationTests
{

    @Test
    void readShp() throws Exception
    {
        File shpFile = new File("C:\\Users\\stong\\Desktop\\sld_cookbook_point\\sld_cookbook_point.shp");
        Map<String, List> map = readSHP(shpFile, "GBK");
        String jsonString = JSONObject.toJSONString(map);
        System.out.println(jsonString);
    }


    @Test
    void spatialAnalysis() throws Exception
    {
//        buffer();       //缓冲区分析
        bufferDif();      //线的特殊缓冲区
//        clip();         //裁剪
//        union();        //合并同类图层
//        merge();        //多个多边形合并成一个
//        erase();        //擦除
//        intersect();    //求交集
    }

    /*
    缓冲区分析
     */
    public static void buffer() throws IOException
    {
        SimpleFeatureCollection cities = CommonMethod.readFeatureCollection("C:\\Users\\stong\\Desktop\\sld_cookbook_line\\sld_cookbook_line.shp");
        List<SimpleFeature> bufferResult = new ArrayList<>();
        SimpleFeatureIterator iterator = cities.features();
        SimpleFeatureType type = CommonMethod.createType(Polygon.class, "citesBuffer");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        while (iterator.hasNext()) {
            SimpleFeature simpleFeature = iterator.next();
            Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();
            Geometry buffer = geometry.buffer(100);
            featureBuilder.add("1");
            featureBuilder.add(buffer);
            SimpleFeature bufferSimpleFeature = featureBuilder.buildFeature(null);
            bufferResult.add(bufferSimpleFeature);
        }
        SimpleFeatureCollection collection = new ListFeatureCollection(type, bufferResult);
        CommonMethod.createShp("D:\\zgis\\geotools\\data\\buffer\\buffer1\\cookbook_line2.shp", collection);
    }

    /*
    缓冲区的两端大小不同时：
     */
    public static void bufferDif() throws Exception
    {

        String geom1 = "LINESTRING Z(100 0 0, 0 100 1)";
        WKTReader reader = new WKTReader(new GeometryFactory());
        LineString line = (LineString)reader.read(geom1);

        BufferParameters parameters1 = new BufferParameters();
        parameters1.setEndCapStyle(BufferParameters.CAP_FLAT);
        parameters1.setSingleSided(true);
        Geometry buffer = BufferOp.bufferOp(line,10,parameters1);
        Geometry buffer2 = BufferOp.bufferOp(line, -20, parameters1);
        System.out.println(buffer.union(buffer2).toString());
    }

    /*
    裁剪
     */
    public static void clip() {
        SimpleFeatureCollection china = CommonMethod.readFeatureCollection("E:\\data\\shp\\china.shp");
        SimpleFeatureCollection countries = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries.shp");
        SimpleFeature next = china.features().next();
        Geometry geometry = (Geometry) next.getDefaultGeometry();
        ClippedFeatureCollection clippedFeatureCollection = new ClippedFeatureCollection(countries, geometry, true);
        SimpleFeatureIterator clipedFeatures = clippedFeatureCollection.features();
        int gcount = 0;
        while (clipedFeatures.hasNext()) {
            SimpleFeature feature = clipedFeatures.next();
            Collection<Property> properties = feature.getProperties();
            Iterator<Property> iterator = properties.iterator();
            while (iterator.hasNext()) {
                Property property = iterator.next();
                System.out.println(property.getName() + "  " + property.getValue());
            }
            gcount ++;
        }
        System.out.println("裁剪后还剩下的元素！" + gcount);
    }
    /*
     合并同类图层
     */
    public static void union() throws IOException {
        SimpleFeatureCollection featureCollectionP1 = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries_part1.shp");
        SimpleFeatureCollection featureCollectionP2 = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries_part2.shp");
        List<SimpleFeature> features = new ArrayList<>();
        SimpleFeatureIterator iterator1 = featureCollectionP1.features();
        SimpleFeatureIterator iterator2 = featureCollectionP2.features();
        while (iterator1.hasNext()) {
            SimpleFeature simpleFeature = iterator1.next();
            features.add(simpleFeature);
        }
        while (iterator2.hasNext()) {
            SimpleFeature simpleFeature = iterator2.next();
            features.add(simpleFeature);
        }
        SimpleFeatureCollection collection = new ListFeatureCollection(featureCollectionP1.getSchema(), features);
        CommonMethod.createShp("E:\\data\\shp\\countries_union.shp", collection);
    }

    /*
    多个多边形合并成一个
     */
    public static void merge() throws IOException {
        SimpleFeatureCollection collection = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries_mergedata.shp");
        SimpleFeatureIterator features = collection.features();
        List<Polygon> polygons = new ArrayList<>();
        while (features.hasNext()) {
            SimpleFeature simpleFeature = features.next();
            Geometry defaultGeometry = (Geometry) simpleFeature.getDefaultGeometry();
            Geometry union = defaultGeometry.union();
            polygons.add((Polygon) union);
        }
        Polygon[] ps = polygons.toArray(new Polygon[polygons.size()]);
        MultiPolygon multiPolygon = new MultiPolygon(ps, new GeometryFactory());
        Geometry union = multiPolygon.union();
        SimpleFeatureType type = CommonMethod.createType(MultiPolygon.class, "countriesMerge");
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.add("1");
        builder.add(union);
        SimpleFeature simpleFeature = builder.buildFeature(null);
        List<SimpleFeature> featureList = new ArrayList<>();
        featureList.add(simpleFeature);
        SimpleFeatureCollection simpleFeatureCollection = new ListFeatureCollection(type, featureList);
        // 生成矢量数据
        CommonMethod.createShp("E:\\data\\shp\\countriesMerge.shp", simpleFeatureCollection);
    }

    /*
    擦除
     */
    public static void erase() throws IOException {
        SimpleFeatureCollection subCollection = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries_differenceData.shp");
        SimpleFeatureCollection collection = CommonMethod.readFeatureCollection("E:\\data\\shp\\countriesMerge.shp");
        SimpleFeatureIterator subFeatures = subCollection.features();
        SimpleFeatureIterator features = collection.features();
        Geometry subGeometry = null;
        while (subFeatures.hasNext()) {
            SimpleFeature simpleFeature = subFeatures.next();
            subGeometry = (Geometry) simpleFeature.getDefaultGeometry();
        }
        Geometry geometry = null;
        while (features.hasNext()) {
            SimpleFeature simpleFeature = features.next();
            geometry = (Geometry) simpleFeature.getDefaultGeometry();
        }
        Geometry difference = geometry.difference(subGeometry);
        SimpleFeatureType type = CommonMethod.createType(MultiPolygon.class, "countriesDifference");
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.add("1");
        builder.add(difference);
        SimpleFeature simpleFeature = builder.buildFeature(null);
        List<SimpleFeature> featureList = new ArrayList<>();
        featureList.add(simpleFeature);
        SimpleFeatureCollection simpleFeatureCollection = new ListFeatureCollection(type, featureList);
        // 生成矢量数据
        CommonMethod.createShp("E:\\data\\shp\\countriesDifference.shp", simpleFeatureCollection);
    }

    /*
    求交集
     */
    public static void intersect() throws IOException {
        SimpleFeatureCollection intersectCollection = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries_intersect.shp");
        SimpleFeatureCollection countries = CommonMethod.readFeatureCollection("E:\\data\\shp\\countries.shp");
        SimpleFeatureIterator features = intersectCollection.features();
        SimpleFeatureIterator countriesFeatures = countries.features();
        List<Polygon> polygons = new ArrayList<Polygon>();
        Geometry other = null;
        while (features.hasNext()) {
            SimpleFeature next = features.next();
            other = (Geometry) next.getDefaultGeometry();
            other =  other.buffer(0);
        }
        List<Geometry> geometries = new ArrayList<>();
        // 一个一个求交集，合并成一个大图层求交集会报错，还不清楚什么原因
        while (countriesFeatures.hasNext()) {
            SimpleFeature next = countriesFeatures.next();
            Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
            if (defaultGeometry instanceof MultiPolygon) {
                MultiPolygon multiPolygon = (MultiPolygon) defaultGeometry;
                int numGeometries = multiPolygon.getNumGeometries();
                for (int i = 0; i < numGeometries; i ++) {
                    Geometry geometryN = multiPolygon.getGeometryN(i);
                    boolean valid = geometryN.isValid();
                    System.out.println("======>" + valid);
                    polygons.add((Polygon) multiPolygon.getGeometryN(i));
                    try {
                        Geometry intersection = other.intersection(geometryN);
                        geometries.add(intersection);
                    } catch (Exception e) {
                        Property fid = next.getProperty("FID");
                        System.out.println(fid.getValue());
                    }

                }
            } else  {
                Geometry union = defaultGeometry.union();
                polygons.add((Polygon) union);
            }
        }
        SimpleFeatureType type = CommonMethod.createType(MultiPolygon.class, "countriesIntersection");
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        List<SimpleFeature> featureList = new ArrayList<>();
        for (int i = 0; i < geometries.size(); i ++) {
            builder.add("1");
            builder.add(geometries.get(i));
            SimpleFeature simpleFeature = builder.buildFeature(null);

            featureList.add(simpleFeature);
        }
        SimpleFeatureCollection simpleFeatureCollection = new ListFeatureCollection(type, featureList);
        // 生成矢量数据
        CommonMethod.createShp("E:\\data\\shp\\countriesIntersection.shp", simpleFeatureCollection);
    }

    /**
     * 读取Shapefiles文件表内容和对应表数据
     * @param SHPFile Shapefiles文件
     * @return Map<（entity/datas）, List（对应map数据）>
     */
    public Map<String, List> readSHP(File SHPFile, String charset) throws Exception {

        // 一个数据存储实现，允许从Shapefiles读取和写入
        ShapefileDataStore shpDataStore = null;
        shpDataStore = new ShapefileDataStore(SHPFile.toURI().toURL());
        shpDataStore.setCharset(Charset.forName(charset));

        // 获取这个数据存储保存的类型名称数组
        // getTypeNames:获取所有地理图层
        String typeName = shpDataStore.getTypeNames()[0];

        // 通过此接口可以引用单个shapefile、数据库表等。与数据存储进行比较和约束
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) shpDataStore.getFeatureSource(typeName);

        // 一个用于处理FeatureCollection的实用工具类。提供一个获取FeatureCollection实例的机制
        FeatureCollection<SimpleFeatureType, SimpleFeature> result = featureSource.getFeatures();

        FeatureIterator<SimpleFeature> iterator = result.features();

        // 迭代
        int stop = 0;
        Map<String, List> map = new HashMap<>();
        List<Map> entity = new ArrayList<>();
        List<Map> datas = new ArrayList<>();
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            Collection<Property> p = feature.getProperties();
            Iterator<Property> it = p.iterator();
            // 构建实体

            // 特征里面的属性再迭代,属性里面有字段
            String name;
            Map<String, Object> data = new HashMap<>();
            while (it.hasNext()) {
                Property pro = it.next();
                name = pro.getName().toString();
                if(stop == 0){
                    Map<String, Object> et = new HashMap<>();
                    PropertyType propertyType = pro.getType();
                    Class cls = propertyType.getBinding();
                    String className = cls.getName();
                    String tName = className.substring(className.lastIndexOf(".")+1);
                    Filter filter = propertyType.getRestrictions().isEmpty() ? null : propertyType.getRestrictions().get(0);
                    String typeLength = filter != null ? filter.toString() : "0";
                    Pattern pattern =Pattern.compile("[^0-9]");
                    Matcher matcher = pattern.matcher(typeLength);
                    String tLength = matcher.replaceAll("").trim();
                    et.put("name", name);
                    et.put("type", tName);
                    et.put("length", tLength);
                    entity.add(et);
                }

                data.put(name,pro.getValue().toString());

            } // end 里层while

            datas.add(data);
            stop++;
        } // end 最外层 while
        map.put("entity", entity);
        map.put("datas", datas);
        iterator.close();
        return map;
    }

}
