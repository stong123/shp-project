package cx.shapefile.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.interfaces.ProjectTransfer;
import cx.shapefile.interfaces.ShapeDeal;
import cx.shapefile.interfaces.SpatialAnalyse;
import cx.shapefile.interfaces.SpatialSvr;
import cx.shapefile.pojo.CxContext;
import cx.shapefile.utils.cx.FileUtils;
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
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

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

    @Autowired
    RestTemplate restTemplate;

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

    public JSONArray geometryAnalyseByJSON(JSONObject json) throws Exception
    {
        JSONArray   results       = new JSONArray();
        JSONArray   layers        = json.getJSONArray(CxContext.LAYERS);         // 待分析的图层
        JSONObject  scopeJson     = json.getJSONObject(CxContext.GEOMETRY);      // 画出的范围
        Geometry    scopeGeometry = spatialSvr.json2Geometry(scopeJson);
        for (int i = 0; i < layers.size(); i++)
        {
            // 从json中读取url和layer并生成geoJSON数据
            JSONObject item    = layers.getJSONObject(i);
            String     url     = item.get(CxContext.URL).toString();
            String     layer   = item.get(CxContext.LAYER).toString();
            JSONObject geoJson = getWfsFeature(url, layer, null);

            // 根据geoJSON拿到feature的遍历器，进行空间分析
            FeatureCollection featureCollection = spatialSvr.geoJson2Collection(geoJson.toString());
            FeatureIterator   featureIterator   = featureCollection.features();
            JSON aResult = spatialSvr.geoSpatialAnalyse(featureIterator, scopeGeometry);
            results.add(aResult);
        }
        return results;
    }

    public JSONObject geometryAnalyseByURl(String url, String tileName, JSONObject scope, String method) throws Exception
    {
        ShapefileDataStore dataStore = null;
        Geometry scopeGeometry = spatialSvr.json2Geometry(scope);
        String zipFilePath     = geoWfs2Shp(url, tileName);

        //对zip文件解压，并读取其中.shp文件
        String fileName        = FileUtils.getShortName(zipFilePath);
        FileUtils.unzip(zipFilePath,shpFileRecourseDir+fileName);
        File   shpDir   = new File(shpFileRecourseDir+fileName);
        File[] subFiles = shpDir.listFiles();
        for (File subFile : subFiles)
        {
            if(FileUtils.endWiths(subFile.getName(),"shp"))
            {
                dataStore = shapeDeal.getShapeDataStore(subFile.getPath());
                break;
            }
        }

        //拿到feature的遍历器，并进行空间分析
        SimpleFeatureIterator featureIterator = shapeDeal.getSimpleFeatureIterator(dataStore, null);
        JSON result = spatialSvr.geoSpatialAnalyse(featureIterator, scopeGeometry);
        dataStore.dispose();
        return (JSONObject) result;
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

    /**
     * 根据wfs查询结果
     */
    public JSONArray getPointAttribute(JSONObject json) throws Exception
    {
        final Double scale = 0.027;       //设0.1为wfs查询的误差范围
        String coord = json.getString("coordinates");
        String[] split = coord.substring(1, coord.length() - 1).split(",");
        Double x = Double.valueOf(split[0]);
        Double y = Double.valueOf(split[1]);
        String bbox = (x-scale)+","+(y-scale)+","+(x+scale)+","+(y+scale);
        String crs = json.getString("crs");
        String exp = "BBOX="+bbox+","+crs;
        JSONArray layers = json.getJSONArray("layers");
        JSONArray result = new JSONArray();
        for (int i = 0; i < layers.size() ; i++)
        {
            JSONObject jsonObject = layers.getJSONObject(i);
            String url   = jsonObject.get("url").toString();
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
//        byte[] zip = SvrUtils.getRequest(wfsURL);
        byte[] zip = restTemplate.getForObject(wfsURL, byte[].class);
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
        byte[] bytes = restTemplate.getForObject(address.toString(), byte[].class);
        JSONObject jsonObject = (JSONObject)JSONObject.parse(bytes, 0, bytes.length, StandardCharsets.UTF_8.newDecoder(), new com.alibaba.fastjson.parser.Feature[0]);
        return jsonObject;
    }

    /**
     * 调用geoserver的wms服务，获取BBOX中的要素信息
     * @param json
     * @return
     * @throws Exception
     */
    public JSONArray getWmsFeatureInfo(JSONObject json) throws Exception
    {
        String BBOX = json.getString("BBOX");
        JSONArray layers = json.getJSONArray("layers");
        JSONArray result = new JSONArray();
        for (int i = 0; i < layers.size() ; i++)
        {
            JSONObject jsonObject = layers.getJSONObject(i);
            String url = jsonObject.getString("url");
            String layer = jsonObject.getString("layer");
            String WIDTH = jsonObject.getString("WIDTH");
            String HEIGHT = jsonObject.getString("HEIGHT");
            String X = jsonObject.getString("X");
            String Y = jsonObject.getString("Y");
            String layerPrefix = layer.substring(0, layer.indexOf(":"));
            StringBuilder address = new StringBuilder(url + "/" + layerPrefix +"/"+ "wms?");
            address.append("service=WMS&");
            address.append("version=1.1.1&");
            address.append("request=GetFeatureInfo&");
            address.append("INFO_FORMAT=application/json&");
            address.append("WIDTH="+WIDTH+"&");
            address.append("HEIGHT="+HEIGHT+"&");
            address.append("X="+X+"&");
            address.append("Y="+Y+"&");
            address.append("BBOX="+BBOX+"&");
            address.append("Y="+Y+"&");
            address.append("LAYERS="+layer+"&");
            address.append("QUERY_LAYERS="+layer);
            byte[] bytes = restTemplate.getForObject(address.toString(), byte[].class);
            JSONObject pointSite = (JSONObject)JSONObject.parse(bytes, 0, bytes.length, StandardCharsets.UTF_8.newDecoder(), new com.alibaba.fastjson.parser.Feature[0]);
            result.add(pointSite);
        }
        return result;
    }

}
