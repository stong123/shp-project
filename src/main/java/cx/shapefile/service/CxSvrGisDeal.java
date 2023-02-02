package cx.shapefile.service;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.DisplayFieldName;
import cx.shapefile.domain.Feature;
import cx.shapefile.domain.Field;
import cx.shapefile.interfaces.OverlayAnalyse;
import cx.shapefile.interfaces.ShapeDeal;
import cx.shapefile.utils.cx.FileUtils;
import cx.shapefile.utils.cx.SvrUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;

@Service
public class CxSvrGisDeal
{
    @Value("${shapefile.dir}")
    private String shpFileRecourseDir;
    @Value("${geoserver.url}")
    private String URI;

    @Autowired
    ShapeDeal shapeDeal;

    @Autowired
    OverlayAnalyse overlayAnalyse;


    public String geoWfs2Shp(String totalLayerName)
    {
        //从完整的图层名（xxx:yyy）中获取前缀(xxx)和图层名(yyy)
        int i = totalLayerName.indexOf(":");
        String layerPrefix = totalLayerName.substring(0, i);
        String layerName = totalLayerName.substring(i + 1);

        //wfs的完整url路径
        String totalPath = URI+"/"+layerPrefix+"/ows?service=WFS&version=1.0.0&request=GetFeature&"+"typeName="+layerPrefix+":"+layerName+"&maxFeatures=50&outputFormat=SHAPE-ZIP";

        File file = null;
        byte[] zip = SvrUtils.getRequest(totalPath);
        if(zip != null)
        {
            try
            {
                file = new File(shpFileRecourseDir+layerName+".zip"); //文件路径（路径+文件名）
                FileUtils.creatEmptyDir(file);
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

    public JSONObject geoAnalyse(String tileName, JSONObject scope, String method) throws Exception
    {
        String shpFilePath = geoWfs2Shp(tileName);

        DisplayFieldName displayFieldName = new DisplayFieldName();
        Geometry geometry = shapeDeal.json2Geometry(scope);

        //获取outFields中的数据并存入set中
        Object outFields = scope.get("outFields");
        HashSet<String> set = new HashSet<>();
        String[] split = outFields.toString().split(",");
        for (String s : split)
        {
            set.add(s);
        }

        ShapefileDataStore dataStore = shapeDeal.getShapeDataStore(shpFilePath);
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
            Geometry result = overlayAnalyse.overlayAnalyse(geoIntersectGeo,geometry,method);
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
}