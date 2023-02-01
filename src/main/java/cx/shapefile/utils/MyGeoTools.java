package cx.shapefile.utils;
 
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.geotools.geometry.jts.JTS;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;


@Slf4j
public class MyGeoTools
{
    public static void projectShape(String inputShp, String outputShp,String endEPSG)
    {
        try
        {
            //源shape文件
            ShapefileDataStore shapeDS = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(new File(inputShp).toURI().toURL());
            //获取当前shp的crs
            String srs = CRS.lookupIdentifier(shapeDS.getSchema().getCoordinateReferenceSystem(),true);
            //创建目标shape文件对象
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
            params.put(ShapefileDataStoreFactory.URLP.key, new File(outputShp).toURI().toURL());
            ShapefileDataStore ds = (ShapefileDataStore) factory.createNewDataStore(params);
            // 设置属性
            SimpleFeatureSource fs = shapeDS.getFeatureSource(shapeDS.getTypeNames()[0]);
            CoordinateReferenceSystem crs= CRS.decode(endEPSG);
            // CoordinateReferenceSystem crs = CRS.parseWKT(strWKTMercator);
            // 下面这行还有其他写法，根据源shape文件的simpleFeatureType可以不用retype，而直接用fs.getSchema设置
            ds.createSchema(SimpleFeatureTypeBuilder.retype(fs.getSchema(), crs));

            // 设置writer
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(ds.getTypeNames()[0], Transaction.AUTO_COMMIT);

            //写记录
            SimpleFeatureIterator it = fs.getFeatures().features();
            try
            {
                while (it.hasNext())
                {
                    SimpleFeature f = it.next();
                    SimpleFeature fNew = writer.next();
                    fNew.setAttributes(f.getAttributes());

                    MathTransform transform = getTransform(srs, endEPSG);
                    Geometry geom = JTS.transform((Geometry)f.getAttribute("the_geom"),transform);
                    fNew.setAttribute("the_geom", geom);
                }
            }
            finally
            {
                it.close();
            }
            writer.write();
            writer.close();
            ds.dispose();
            shapeDS.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    读取shp
     */
    public static JSON readFile(String shapePath, String charset)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        ShapefileDataStore dataStore = null;
        try
        {
            dataStore = (ShapefileDataStore) factory.createDataStore(new File(shapePath).toURI().toURL());
            if (dataStore != null)
            {
                if(charset != null)
                {
                    dataStore.setCharset(Charset.forName(charset));
                }
                else
                {
                    dataStore.setCharset(Charset.forName("GBK"));
                }
            }
            SimpleFeatureSource featureSource = dataStore.getFeatureSource();
            SimpleFeatureIterator itertor = featureSource.getFeatures().features();


            JSONArray jsonArray = new JSONArray();
            while (itertor.hasNext())
            {
                SimpleFeature feature = itertor.next();
                Iterator<Property> it = feature.getProperties().iterator();
                Map<String, Object> map = new HashMap<>();
                while (it.hasNext())
                {
                    Property pro = it.next();
                    map.put(String.valueOf(pro.getName()), String.valueOf(pro.getValue()));
                }
                list.add(map);
                // Feature转GeoJSON
                FeatureJSON fjson = new FeatureJSON();
                StringWriter writer = new StringWriter();
                fjson.writeFeature(feature, writer);
                //String sjson = writer.toString();
                jsonArray.add(JSONObject.parse(writer.toString()));
            }
            Map<String,Object> map = new HashMap<>();
            map.put("features",jsonArray);
            JSONObject jsonObject = new JSONObject(map);
            itertor.close();
            return jsonObject;
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if(dataStore != null)
            {
                dataStore.dispose();
            }
        }
        return null;
    }

    public static MathTransform getTransform(String startEPSG ,String endEPSG)
    {
        try
        {
            CoordinateReferenceSystem source = CRS.decode(startEPSG,true);
            CoordinateReferenceSystem crsTarget = CRS.decode(endEPSG,true);
            // 投影转换
            MathTransform transform = CRS.findMathTransform(source, crsTarget,true);
            return transform;
        }
        catch (FactoryException e)
        {
            throw new RuntimeException(e);
        }
    }


    //坐标转换
    public static Double[] CoordinateChange(Double x, Double y,String startEPSG ,String endEPSG)
    {
        Double[] res = new Double[2];
        Coordinate tar = null;
        try
        {
            //封装点，这个是通用的，也可以用POINT（y,x）
            // private static WKTReader reader = new WKTReader( geometryFactory );
            Coordinate sour = new Coordinate(y, x);
            //这里要选择转换的坐标系是可以随意更换的
            MathTransform transform = getTransform(startEPSG, endEPSG);
            tar = new Coordinate();
            //转换
            JTS.transform(sour, tar, transform);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        String[] split = (tar.toString().substring(1, tar.toString().length() - 1)).split(",");
        //经纬度精度
        DecimalFormat fm = new DecimalFormat("0.0000000");
        res[0] = Double.valueOf(fm.format(Double.valueOf(split[0])));
        res[1] = Double.valueOf(fm.format(Double.valueOf(split[1])));
        return res;
    }

    /*
     * 获取Shape文件的坐标系信息,GEOGCS表示这个是地址坐标系,PROJCS则表示是平面投影坐标系
     */
    public static String getCoordinateSystemWKT(String path)
    {
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        ShapefileDataStore dataStore = null;
        try
        {
            dataStore = (ShapefileDataStore) factory.createDataStore(new File(path).toURI().toURL());
            return dataStore.getSchema().getCoordinateReferenceSystem().toWKT();
        }
        catch (UnsupportedOperationException | IOException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            dataStore.dispose();
        }
        return "";
    }

}