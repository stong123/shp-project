package cx.shapefile.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.domain.DisplayFieldName;
import cx.shapefile.service.GeoAnalyseService;
import cx.shapefile.service.ShapeDealService;
import cx.shapefile.utils.MyGeoTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
public class ShapeDealController
{
    @Autowired
    private ShapeDealService shapeDealService;

    @Autowired
    private GeoAnalyseService geoAnalyseService;

    /**
     * 读shp文件
     * @param filePath
     * @return
     * @throws Exception
     */
    @ResponseBody
    @PostMapping("/readShp")
    public JSON readShpFile(String filePath, String charset)
    {
        JSON readFile = MyGeoTools.readFile(filePath, charset);
        return readFile;
    }

    /**
     * 获取Shape文件的坐标系信息
     * @param shpPath
     * @return
     */
    @PostMapping("/getShpPrj")
    public String getShpPrj(String shpPath)
    {
        return MyGeoTools.getCoordinateSystemWKT(shpPath);
    }

    /**
     * 坐标点转换
     * @param x
     * @param y
     * @param startEPSG
     * @param endEPSG
     * @return
     */
    @PostMapping("/getCoord")
    public Double[] getCoordinate(double x, double y , String startEPSG, String endEPSG)
    {
        return MyGeoTools.CoordinateChange(x, y, startEPSG, endEPSG);
    }

    /**
     * 读shp,如果是投影坐标系，则进行坐标转换
     * @param shpPath
     * @return
     * @throws IOException
     */
    @PostMapping("/convert")
    public String convert(String shpPath,String endEPSG)
    {
        String projectShapePath = shapeDealService.projectShape(shpPath,endEPSG);
        return projectShapePath;
    }

    /**
     * geojson转shp
     * @param geojson
     * @return
     * @throws IOException
     */
    @PostMapping("/json2shp")
    public String geoJson2Shape(@RequestBody String geojson) throws IOException
    {
        String shpFileName = shapeDealService.getFeaturesCollectionByjson(geojson);
        return shpFileName;
    }

    /**
     * geoserver通过wfs获取shp
     * @param totalLayerName
     * @return
     */
    @PostMapping("geoWfs2shp")
    public String geoWfs2Shp(String totalLayerName)
    {
        String outPath = shapeDealService.geoWfs2Shp(totalLayerName);
        return outPath;
    }

    /**
     * 空间分析
     * @param json  测试所用的范围数据
     * @param shpFilePath   shp文件地址
     * @return
     * @throws Exception
     */
    @PostMapping("/getGeometry")
    public JSON getGeoJson(@RequestPart("json") JSONObject json,
                           @RequestPart("path") String shpFilePath,
                           @RequestPart("method") String method,
                           @RequestPart(value = "distance",required = false) String distance) throws Exception
    {
        DisplayFieldName displayFieldName = geoAnalyseService.setDisplayFieldName(json, shpFilePath, method);
        JSON displayFieldNameJson = (JSON) JSON.toJSON(displayFieldName);
        return displayFieldNameJson;
    }

}
