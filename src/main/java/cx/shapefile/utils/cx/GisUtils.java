package cx.shapefile.utils.cx;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

@Slf4j
public class GisUtils
{
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

    public static String jsonToWkt(JSONObject jsonObject)
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
}
