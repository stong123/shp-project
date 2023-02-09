package cx.shapefile.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.DisplayFieldName;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import cx.shapefile.interfaces.ProjectTransfer;
import cx.shapefile.interfaces.ShapeDeal;
import cx.shapefile.interfaces.SpatialAnalyse;
import cx.shapefile.interfaces.SpatialSvr;
import cx.shapefile.utils.cx.FileUtils;
import cx.shapefile.utils.cx.SvrUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;

@Service
public class CxSvrGisDeal
{
    @Value("${shapefile.dir}")
    private String shpFileRecourseDir;

    @Autowired
    ShapeDeal shapeDeal;
    @Autowired
    ProjectTransfer projectTransfer;
    @Autowired
    SpatialAnalyse spatialAnalyse;
    @Autowired
    SpatialSvr spatialSvr;

    public String getPrjMessage(String shpPath) throws Exception
    {
        return shapeDeal.getCoordinateSystemWKT(shpPath);
    }

    public JSON readShpFile(String filePath, String charset) throws Exception
    {
        return shapeDeal.readShpFile(filePath,charset);
    }

    public String geoJson2Shp(String geoJson) throws Exception
    {

        FeatureCollection<SimpleFeatureType, SimpleFeature> featuresCollection = spatialSvr.getFeaturesCollectionByJson(geoJson);
        return shapeDeal.featureCollectionToShp(featuresCollection);
    }

    public JSONArray spatialAnalyse(JSONObject json) throws Exception
    {
        JSONArray jsonArrayResult = new JSONArray();
        JSONObject scopeJson = json.getJSONObject("geometry");
        Geometry scopeGeometry = spatialSvr.json2Geometry(scopeJson);
        JSONArray layers = json.getJSONArray("layers");
        for (int i = 0; i < layers.size() ; i++)
        {
            DisplayFieldName displayFieldName = new DisplayFieldName(); //存放分析结果的实体类
            JSONObject jsonObject = layers.getJSONObject(i);
            String url = jsonObject.get("url").toString();
            String layer = jsonObject.get("layer").toString();
            JSONObject geoJson = getWfsFeature(url, layer, null);
            FeatureCollection featureCollection = spatialSvr.geoJson2Collection(geoJson.toString());
            FeatureIterator featureIterator = featureCollection.features();

            ArrayList<Feature> features = new ArrayList<>();
            HashSet<String> set = new HashSet<>();
            set.add("*");
            boolean flag = false;
            while(featureIterator.hasNext())
            {
                SimpleFeature feature = (SimpleFeature) featureIterator.next();
                if(!flag)
                {
                    //如果是第一次进入就获取该文件的中文坐标，文件内的属性。
                    ArrayList<Field> fields = shapeDeal.setFields(set, feature);
                    displayFieldName.setFields(fields);
                    String spatialReference = String.valueOf(shapeDeal.getSpatialReference(feature));
                    displayFieldName.setSpatialReference(spatialReference);
                    flag = true;
                }
                Geometry geo = (Geometry) feature.getDefaultGeometry();
                Geometry geoIntersectGeo = geo.intersection(geo);
                //叠加分析后的图形数据
                Geometry result = spatialAnalyse.spatialAnalyse(geoIntersectGeo,scopeGeometry,SpatialAnalyse.Intersection);
                if(!result.isEmpty())
                {
                    //设置相交图形的属性（文件中可直接获取的）及相交后图形的属性（周长，面积，图形）
                    Feature feature1 = shapeDeal.setFeature(set, feature, result);
                    features.add(feature1);
                }
            }
            displayFieldName.setFeatures(features);
            //关闭文件
            featureIterator.close();

            jsonArrayResult.add(displayFieldName);
        }
        return jsonArrayResult;
    }

    public JSONObject geoAnalyse(String url, String tileName, JSONObject scope, String method) throws Exception
    {
        Geometry geometry    = spatialSvr.json2Geometry(scope);
        DisplayFieldName displayFieldName = new DisplayFieldName(); //存放分析结果的实体类
        //获取outFields中的数据并存入set中
        Object          outFields = scope.get("outFields");
        HashSet<String> set       = new HashSet<>();
        String[]        split     = outFields.toString().split(",");
        for (String s : split)
        {
            set.add(s);
        }
        String zipFilePath  = geoWfs2Shp(url, tileName);
        String fileName     = FileUtils.getShortName(zipFilePath);
        FileUtils.unzip(zipFilePath,shpFileRecourseDir+fileName);
        File   shpDir   = new File(shpFileRecourseDir+fileName);
        File[] subFiles = shpDir.listFiles();
        ShapefileDataStore dataStore = null;
        for (File subFile : subFiles)
        {
            if(FileUtils.endWiths(subFile.getName(),"shp"))
            {
                dataStore = shapeDeal.getShapeDataStore(subFile.getPath());
                break;
            }
        }
        SimpleFeatureIterator featureIterator = shapeDeal.getSimpleFeatureIterator(dataStore, null);
        boolean flag = false;
        ArrayList<Feature> features = new ArrayList<>();
        while(featureIterator.hasNext())
        {
            SimpleFeature feature = featureIterator.next();
            if(!flag)
            {
                //如果是第一次进入就获取该文件的中文坐标，文件内的属性。
                ArrayList<Field> fields = shapeDeal.setFields(set, feature);
                displayFieldName.setFields(fields);
                String spatialReference = String.valueOf(shapeDeal.getSpatialReference(feature));
                displayFieldName.setSpatialReference(spatialReference);
                flag = true;
            }
            Geometry geo = (Geometry) feature.getDefaultGeometry();
            Geometry geoIntersectGeo = geo.intersection(geo);
            //叠加分析后的图形数据
            Geometry result = spatialAnalyse.spatialAnalyse(geoIntersectGeo,geometry,method);
            if(!result.isEmpty())
            {
                //设置相交图形的属性（文件中可直接获取的）及相交后图形的属性（周长，面积，图形）
                Feature feature1 = shapeDeal.setFeature(set, feature, result);
                features.add(feature1);
            }
        }
        displayFieldName.setFeatures(features);
        //关闭文件
        featureIterator.close();
        dataStore.dispose();
        return (JSONObject) JSONObject.toJSON(displayFieldName);
    }

    public Double[] transform(double x, double y, String startEPSG, String endEPSG) throws Exception
    {
        return projectTransfer.transform(x, y, startEPSG, endEPSG);
    }

    public String projectShape(String shpPath, String endEPSG) throws Exception
    {
        File shpFile = new File(shpPath);
        if(shpFile.exists())
        {
            String shpFileName = shpFile.getName();

            File dir = new File(shpFileRecourseDir+System.currentTimeMillis());
            if(!dir.exists())
            {
                dir.mkdirs();
            }
            String outPath = dir + "\\" + shpFileName;
            projectTransfer.projectShape(shpPath,outPath,endEPSG);
            return outPath;
        }
        return null;
    }

    public JSONArray getPointAttribute(JSONObject json) throws Exception
    {
        String bbox = json.getString("BBOX");
        String crs = json.getString("crs");
        String exp = "BBOX="+bbox+","+crs;
        JSONArray layers = json.getJSONArray("layers");
        JSONArray result = new JSONArray();
        for (int i = 0; i < layers.size() ; i++)
        {
            JSONObject jsonObject = layers.getJSONObject(i);
            String url = jsonObject.get("url").toString();
            String layer = jsonObject.get("layer").toString();

            JSON feature = getWfsFeature(url,layer,exp);
            result.add(feature);
        }
        return result;
    }

    public JSONObject queryAttribute(String url, String layer, String exp) throws Exception
    {
        String expression = "cql_filter="+exp;
        return getWfsFeature(url,layer,expression);
    }

    public String geoWfs2Shp(String url, String totalLayerName) throws Exception
    {
        String wfsURL = spatialSvr.geoServerWfs(url, totalLayerName, "shape");
        byte[] zip = SvrUtils.getRequest(wfsURL);
        String layerName = totalLayerName.substring(totalLayerName.indexOf(":") + 1);
        File file = null;
        if(zip != null)
        {
            file = new File(shpFileRecourseDir+layerName+".zip"); //文件路径（路径+文件名）
            FileUtils.creatEmptyDir(file);                                 //如果目录为空则创建
            FileOutputStream outStream = new FileOutputStream(file);       //文件输出流将数据写入文件
            outStream.write(zip);
            outStream.close();
        }
        return file.getAbsolutePath();
    }

    public JSONObject getWfsFeature(String url, String layerName, String exp) throws Exception
    {
        String wfsURL = spatialSvr.geoServerWfs(url, layerName, "json");
        StringBuilder address = new StringBuilder(wfsURL);
        if (exp != null)
        {
            address.append("&"+exp);
        }
        byte[] bytes = SvrUtils.getRequest(address.toString());
        JSONObject jsonObject = (JSONObject)JSONObject.parse(bytes, 0, bytes.length, StandardCharsets.UTF_8.newDecoder(), new com.alibaba.fastjson.parser.Feature[0]);
        return jsonObject;
    }

}
