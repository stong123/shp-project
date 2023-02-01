package cx.shapefile.service;

import cx.shapefile.utils.MyGeoTools;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ShapeDealService
{

    @Value("${shapefile.dir}")
    private String shpFileRecourseDir;

    public String getFeaturesCollectionByjson(String geoJSON) throws IOException
    {
        //Reader reader = new StringReader(geoJSON);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(geoJSON.getBytes());
        GeometryJSON gjson = new GeometryJSON();
        FeatureJSON fjson = new FeatureJSON(gjson);
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = fjson.readFeatureCollection(inputStream);
        String shpFilePath = featureCollectionToShp(features);
        return shpFilePath;
    }

    public String featureCollectionToShp(FeatureCollection<SimpleFeatureType, SimpleFeature> features)
    {
        // convert schema for shapefile
        SimpleFeatureType schema = features.getSchema();
        GeometryDescriptor geom = schema.getGeometryDescriptor();
        // geojson文件属性
        List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();
        // geojson文件空间类型（必须在第一个）
        GeometryType geomType = null;
        List<AttributeDescriptor> attribs = new ArrayList<>();
        for (AttributeDescriptor attrib : attributes)
        {
            AttributeType type = attrib.getType();
            if (type instanceof GeometryType)
            {
                geomType = (GeometryType) type;
            }
            else
            {
                attribs.add(attrib);
            }
        }
        if (geomType == null)
        {
            throw new RuntimeException();
        }

        // 使用geomType创建gt
        GeometryTypeImpl gt = new GeometryTypeImpl(new NameImpl("the_geom"), geomType.getBinding(),
                geom.getCoordinateReferenceSystem() == null ? DefaultGeographicCRS.WGS84 : geom.getCoordinateReferenceSystem(), // 用户未指定则默认为wgs84
                geomType.isIdentified(), geomType.isAbstract(), geomType.getRestrictions(),
                geomType.getSuper(), geomType.getDescription());

        // 创建识别符
        GeometryDescriptor geomDesc = new GeometryDescriptorImpl(gt, new NameImpl("the_geom"), geom.getMinOccurs(),
                geom.getMaxOccurs(), geom.isNillable(), geom.getDefaultValue());

        // the_geom 属性必须在第一个
        attribs.add(0, geomDesc);

        SimpleFeatureType outSchema = new SimpleFeatureTypeImpl(schema.getName(), attribs, geomDesc, schema.isAbstract(),
                schema.getRestrictions(), schema.getSuper(), schema.getDescription());
        List<SimpleFeature> outFeatures = new ArrayList<>();
        try (FeatureIterator<SimpleFeature> features2 = features.features())
        {
            while (features2.hasNext())
            {
                SimpleFeature f = features2.next();
                SimpleFeature reType = DataUtilities.reType(outSchema, f, true);

                reType.setAttribute(outSchema.getGeometryDescriptor().getName(),
                        f.getAttribute(schema.getGeometryDescriptor().getName()));

                outFeatures.add(reType);
            }
        }
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = simpleDateFormat.format(date);

        File dir = new File(shpFileRecourseDir+fileName);
        if(!dir.exists())
        {
            dir.mkdir();
        }
        String shpPath = dir.getPath()+"\\"+fileName+".shp";
        saveFeaturesToShp(outFeatures, outSchema, shpPath);
        return shpPath;
    }

    /**
     * 保存features为shp格式
     *
     * @param features 要素类
     * @param TYPE     要素类型
     * @param shpPath  shp保存路径
     * @return 是否保存成功
     */
    public boolean saveFeaturesToShp(List<SimpleFeature> features, SimpleFeatureType TYPE, String shpPath)
    {
        try
        {
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
            File shpFile = new File(shpPath);
            Map<String, Serializable> params = new HashMap<>();
            params.put("url", shpFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore newDataStore =
                    (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.setCharset(StandardCharsets.UTF_8);

            newDataStore.createSchema(TYPE);

            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore)
            {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
                featureStore.setTransaction(transaction);
                try
                {
                    featureStore.addFeatures(collection);
                    generateCpgFile(shpPath, StandardCharsets.UTF_8);
                    transaction.commit();
                }
                catch (Exception problem)
                {
                    problem.printStackTrace();
                    transaction.rollback();
                }
                finally
                {
                    transaction.close();
                }
            }
            else
            {
                System.out.println(typeName + " does not support read/write access");
            }
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }

    /**
     * 生成cpg文件
     *
     * @param filePath 文件完整路径
     * @param charset  文件编码
     * @return 是否生成成功
     */
    public static boolean generateCpgFile(String filePath, Charset charset)
    {
        try
        {
            File file = new File(filePath);
            if (!file.exists())
            {
                return false;
            }
            String tempPath = file.getPath();
            int index = tempPath.lastIndexOf('.');
            String name = tempPath.substring(0, index);
            String cpgFilePath = name + ".cpg";
            File cpgFile = new File(cpgFilePath);
            if (cpgFile.exists())
            {
                return true;
            }
            boolean newFile = cpgFile.createNewFile();
            if (newFile)
            {
                Files.write(cpgFile.toPath(), charset.toString().getBytes(charset));
            }
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public String projectShape(String shpPath,String endEPSG)
    {
        File shpFile = new File(shpPath);
        if(shpFile.exists())
        {
            String shpFileName = shpFile.getName();

            File dir = new File(shpFileRecourseDir+System.currentTimeMillis());
            if(!dir.exists())
            {
                dir.mkdir();
            }
            String outPath = dir + "\\" + shpFileName;
            MyGeoTools.projectShape(shpPath,outPath,endEPSG);
            return outPath;
        }
        return null;
    }

    @Value("${geoserver.url}")
    private String url;
    public String geoWfs2Shp(String totalLayerName)
    {
        //从完整的图层名（xxx:yyy）中获取前缀(xxx)和图层名(yyy)
        int i = totalLayerName.indexOf(":");
        String layerPrefix = totalLayerName.substring(0, i);
        String layerName = totalLayerName.substring(i + 1);

        //wfs的完整url路径
        String totalPath = url+"/"+layerPrefix+"/ows?service=WFS&version=1.0.0&request=GetFeature&"+"typeName="+layerPrefix+":"+layerName+"&maxFeatures=50&outputFormat=SHAPE-ZIP";
        System.out.printf(totalPath);
        File file = null;
        byte[] zip = getRequest(totalPath);
        if(zip != null)
        {
            try
            {
                file = new File(shpFileRecourseDir+layerName+".zip"); //文件路径（路径+文件名）
                if (!file.exists())  //文件不存在则创建文件，先创建目录
                {
                    File dir = new File(file.getParent());
                    dir.mkdirs();
                    file.createNewFile();
                }
                FileOutputStream outStream = new FileOutputStream(file); //文件输出流将数据写入文件
                outStream.write(zip);
                outStream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }

    public byte[] getRequest(String path)
    {
        HttpRequest request = HttpRequest.get(path);
        HttpResponse response = request.send();
        return response.bodyBytes();
    }
}
