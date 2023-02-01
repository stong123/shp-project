package cx.shapefile.utils;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class CommonMethod {
    public static SimpleFeatureType createType(Class<?> c, String layerName) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add("FID",String.class);
        builder.add("the_geom", c);
        // 设置了图层的名字
        builder.setName(layerName);
        SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
        return simpleFeatureType;
    }
    public static void createShp(String shpPath, SimpleFeatureCollection collection) throws IOException {
        File shpFile = new File(shpPath);
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        SimpleFeatureType simpleFeatureType = collection.getSchema();
        // 创造shpstore需要的参数
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", shpFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore newDataStore =
                (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(simpleFeatureType);
        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        featureStore.setTransaction(transaction);
        featureStore.addFeatures(collection);
        featureStore.setTransaction(transaction);
        transaction.commit();
        transaction.close();
    }
    public static SimpleFeatureCollection readFeatureCollection(String shpPath) {
        SimpleFeatureCollection featureCollection = null;
        File shpFile = new File(shpPath);
        try {
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(shpFile.toURI().toURL());
            // 设置编码,防止属性的中文字符出现乱码
            shapefileDataStore.setCharset(Charset.forName("UTF-8"));
            // 这个typeNamae不传递，默认是文件名称
            FeatureSource featuresource = shapefileDataStore.getFeatureSource(shapefileDataStore.getTypeNames()[0]);
            featureCollection = (SimpleFeatureCollection) featuresource.getFeatures();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return featureCollection;
    }
}
