package cx.shapefile.impl;

import cx.shapefile.interfaces.ProjectTransfer;
import cx.shapefile.interfaces.ShapeDeal;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProjectTransferImpl implements ProjectTransfer
{
    @Autowired
    ShapeDeal shapeDeal;

    /**
     * 大地坐标转平面坐标
     */
    @Override
    public MathTransform toProject(String startEPSG, String endEPSG) throws Exception
    {
        try
        {
            CoordinateReferenceSystem source    = CRS.decode(startEPSG,true);
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

    @Override
    public void projectShape(String inputShp, String outputShp, String endEPSG) throws Exception
    {
        //源shape文件
        ShapefileDataStore shapeDS = shapeDeal.getShapeDataStore(inputShp);
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

                MathTransform transform = toProject(srs, endEPSG);
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

    /**
     * 坐标转换
     */
    @Override
    public Double[] transform(double x, double y , String startEPSG, String endEPSG) throws Exception
    {
        Double[] res = new Double[2];
        Coordinate tar = null;
        try
        {
            //封装点，这个是通用的，也可以用POINT（y,x）
            // private static WKTReader reader = new WKTReader( geometryFactory );
            Coordinate sour = new Coordinate(y, x);
            //这里要选择转换的坐标系是可以随意更换的
            MathTransform transform = toProject(startEPSG, endEPSG);
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
}
