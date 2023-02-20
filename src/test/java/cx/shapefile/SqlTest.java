package cx.shapefile;

import cx.shapefile.Mapper.PointsMapper;
import cx.shapefile.Mapper.PostGisDBMapper;
import cx.shapefile.pojo.Points;
import cx.shapefile.pojo.Ynzd;
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class SqlTest
{

    @Autowired
    PointsMapper pointsMapper;

    @Autowired
    PostGisDBMapper postGisDBMapper;

    @Test
    void searchGeom() throws Exception
    {
        List<Ynzd> dlzxxes = postGisDBMapper.queryGeom();
        System.out.println(dlzxxes.toString());
    }

    @Test
    void searchAll() throws Exception
    {
        List<Ynzd> ynzds = postGisDBMapper.queryGeom();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTReader reader = new WKTReader(geometryFactory);
        SimpleFeatureType TYPE = DataUtilities.createType("Link",
                "geometry:Geometry," +
                        "county:String," +
                        "township:String," +
                        "village:String,"+
                        "village_group:String,"+
                        "land_no_survey:String,"+
                        "land_type_name1:String,"+
                        "land_type_code:String,"+
                        "spot_area:String,"+
                        "confer_area:String,"+
                        "area_unit:String,"+
                        "owner:String"
        );
        List<SimpleFeature> features = new ArrayList<>();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        for (Ynzd ynzd : ynzds)
        {
            Geometry ynzdGeom = reader.read(ynzd.getGeom());
            featureBuilder.add(ynzdGeom);
            featureBuilder.add(ynzd.getCounty());
            featureBuilder.add(ynzd.getTownship());
            featureBuilder.add(ynzd.getVillage());
            featureBuilder.add(ynzd.getVillage_group());
            featureBuilder.add(ynzd.getLand_no_survey());
            featureBuilder.add(ynzd.getLand_type_name1());
            featureBuilder.add(ynzd.getLand_type_code());
            featureBuilder.add(ynzd.getSpot_area());
            featureBuilder.add(ynzd.getConfer_area());
            featureBuilder.add(ynzd.getArea_unit());
            featureBuilder.add(ynzd.getOwner());
            SimpleFeature simpleFeature = featureBuilder.buildFeature(ynzd.getId());
            features.add(simpleFeature);
        }
        StringWriter writer = new StringWriter();
        FeatureJSON fjson = new FeatureJSON();
        SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
        fjson.writeFeatureCollection(collection, writer);

        File file = new File("C:\\Users\\stong\\Desktop\\temp\\pgsqltext.txt");
        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(String.valueOf(writer));
        bw.flush();
        bw.close();
        fw.close();
    }


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
