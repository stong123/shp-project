package cx.shapefile.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.service.CxSvrGisDeal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/zgis/")
public class ControllerGisDeal
{
    @Autowired
    CxSvrGisDeal cxSvrGisDeal;

    @PostMapping("getShpPrj")
    public String getPrjMessage(String shpPath) throws Exception
    {
        return cxSvrGisDeal.getPrjMessage(shpPath);
    }

    @ResponseBody
    @PostMapping("readShp")
    public JSON readShpFile(String filePath, String charset) throws Exception
    {
        return cxSvrGisDeal.readShpFile(filePath, charset);
    }

    @PostMapping("json2shp")
    public String geoJson2Shp(@RequestBody String geoJson) throws Exception
    {
        return cxSvrGisDeal.geoJson2Shp(geoJson);
    }

    @PostMapping("wfs2shp")
    public String wfs2Shp(String url, String totalLayerName) throws Exception
    {
        return cxSvrGisDeal.geoWfs2Shp(url, totalLayerName);
    }

    @PostMapping("projectShape")
    public String projectShape(String shpPath,String endEPSG) throws Exception
    {
        return cxSvrGisDeal.projectShape(shpPath,endEPSG);
    }

    @PostMapping("transform")
    public Double[] getCoordinate(double x, double y , String startEPSG, String endEPSG) throws Exception
    {
        return cxSvrGisDeal.transform(x,y,startEPSG,endEPSG);
    }

    @PostMapping("geoAnalyse")
    @ResponseBody
    public JSONObject geoAnalyse(@RequestParam("url") String url,
                                 @RequestParam("tileName") String tileName,
                                 @RequestPart("json") JSONObject scope,
                                 @RequestParam("method") String method) throws Exception
    {
        return cxSvrGisDeal.geoAnalyse(url,tileName,scope,method);
    }

    @PostMapping("spatialAnalyse")
    @ResponseBody
    public JSONArray spatialAnalyse(@RequestBody JSONObject json) throws Exception
    {
        return cxSvrGisDeal.spatialAnalyse(json);
    }

    @PostMapping("getAttribute")
    @ResponseBody
    public JSONArray getPointAttribute(@RequestBody JSONObject json) throws Exception
    {
        return cxSvrGisDeal.getPointAttribute(json);
    }

    @PostMapping("queryAttribute")
    @ResponseBody
    public JSONObject queryAttribute(String url, String layer, String exp) throws Exception
    {
        return cxSvrGisDeal.queryAttribute(url,layer,exp);
    }
}
