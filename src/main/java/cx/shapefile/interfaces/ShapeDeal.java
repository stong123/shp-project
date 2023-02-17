package cx.shapefile.interfaces;

import com.alibaba.fastjson.JSON;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public interface ShapeDeal
{
    ShapefileDataStore getShapeDataStore(String shpPath) throws Exception;

    String getCoordinateSystemWKT(String path) throws Exception;

    JSON readShpFile(String shapePath, String charset) throws Exception;

    SimpleFeatureIterator getSimpleFeatureIterator(ShapefileDataStore dataStore, String charset) throws Exception;

    ArrayList<Field> setFields(SimpleFeature feature) throws Exception;

    Feature setFeature(SimpleFeature feature, Geometry geometry)throws Exception;

    String featureCollectionToShp(FeatureCollection<SimpleFeatureType, SimpleFeature> features) throws Exception;

    boolean saveFeaturesToShp(List<SimpleFeature> features, SimpleFeatureType TYPE, String shpPath) throws Exception;

    boolean generateCpgFile(String filePath, Charset charset) throws Exception;

}
