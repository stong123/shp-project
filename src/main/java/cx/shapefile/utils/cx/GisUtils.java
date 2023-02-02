package cx.shapefile.utils.cx;

import com.alibaba.fastjson.JSONObject;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;

import java.io.StringWriter;

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
}
