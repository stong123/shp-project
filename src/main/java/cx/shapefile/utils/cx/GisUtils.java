package cx.shapefile.utils.cx;

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
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
public class GisUtils
{

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

    public static JSON readShpFile(String shapePath, String charset)
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
                FeatureJSON  fjson  = new FeatureJSON();
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

    public static JSONObject wktToJson(String wkt)
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
}
