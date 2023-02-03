package cx.shapefile.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cx.shapefile.service.CxSvrGisDeal;
import cx.shapefile.utils.MyGeoTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/zgis/deal")
public class ControllerGisDeal
{
    @Autowired
    CxSvrGisDeal cxSvrGisDeal;

    @PostMapping("/getShpPrj")
    public String getPrjMessage(String shpPath)
    {
        return cxSvrGisDeal.getPrjMessage(shpPath);
    }

    @ResponseBody
    @PostMapping("/readShp")
    public JSON readShpFile(String filePath, String charset)
    {
        return cxSvrGisDeal.readShpFile(filePath, charset);
    }

    @PostMapping("/json2shp")
    public String geoJson2Shp(@RequestBody String geoJson) throws Exception
    {
        return cxSvrGisDeal.geoJson2Shp(geoJson);
    }

    @PostMapping("wfs2shp")
    public String wfs2Shp(String totalLayerName) throws IOException
    {
        return cxSvrGisDeal.geoWfs2Shp(totalLayerName);
    }

    @PostMapping("/projectShape")
    public String projectShape(String shpPath,String endEPSG) throws Exception
    {
        return cxSvrGisDeal.projectShape(shpPath,endEPSG);
    }

    @PostMapping("/transform")
    public Double[] getCoordinate(double x, double y , String startEPSG, String endEPSG) throws Exception
    {
        return cxSvrGisDeal.transform(x,y,startEPSG,endEPSG);
    }

    @PostMapping("/geoAnalyse")
    public JSONObject geoAnalyse(String tileName, @RequestPart("json") JSONObject scope, String method) throws Exception
    {
        return cxSvrGisDeal.geoAnalyse(tileName,scope,method);
    }

}
