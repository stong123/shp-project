package cx.shapefile;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.Mapper.PointsMapper;
import cx.shapefile.pojo.Points;
import cx.shapefile.utils.cx.GisUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class SqlTest
{

    @Autowired
    PointsMapper pointsMapper;

    @Test
    void getAll() throws Exception
    {
        List<Points> points = pointsMapper.queryAll();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTReader reader = new WKTReader(geometryFactory);
        SimpleFeatureType TYPE = DataUtilities.createType("Link",
                "geometry:Point," + // <- the geometry attribute: Point type
                        "name:String," +   // a String attribute
                        "x:Double," +
                        "y:Double"
        );
        List<SimpleFeature> features = new ArrayList<>();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        for (Points point : points)
        {
            Geometry pointGeom = reader.read(point.getGeom());
            featureBuilder.add(pointGeom);
            featureBuilder.add(point.getName());
            featureBuilder.add(point.getX());
            featureBuilder.add(point.getY());
            SimpleFeature simpleFeature = featureBuilder.buildFeature(point.getId());
            features.add(simpleFeature);
        }
        StringWriter writer = new StringWriter();
        FeatureJSON fjson = new FeatureJSON();
        SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
        fjson.writeFeatureCollection(collection, writer);
        System.out.println(writer);
    }
}
