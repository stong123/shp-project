package cx.shapefile.gis;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.utils.MyUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class ShapeDeal
{

    /**
     * json转geometry
     * @param json
     * @return
     * @throws ParseException
     */
    public Geometry json2Geometry(JSONObject json) throws ParseException
    {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
        WKTReader reader = new WKTReader(geometryFactory);
        String wkt = MyUtils.jsonToWkt(json);
        Geometry geometry = reader.read(wkt);
        return geometry;
    }

    /**
     * 从SimpleFeature中获取空间坐标
     * @param feature
     * @return
     */
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
    public ShapefileDataStore getShapefileDataStore(String shpPath)
    {
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        try
        {
            ShapefileDataStore dataStore = (ShapefileDataStore) factory.createDataStore(new File(shpPath).toURI().toURL());
            return dataStore;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取SimpleFeature的迭代器，从中可以拿到SimpleFeature
     * @param dataStore
     * @param charset
     * @return
     */
    public SimpleFeatureIterator getSimpleFeatureIterator(ShapefileDataStore dataStore, String charset)
    {
        SimpleFeatureIterator itertor = null;
        try
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
            itertor = featureSource.getFeatures().features();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return itertor;
    }
}
