package cx.shapefile.controller;

import com.alibaba.fastjson.JSONObject;
import cx.shapefile.service.CxSvrGisDeal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/zgis/deal")
public class ControllerGisDeal
{
    @Autowired
    CxSvrGisDeal cxSvrGisDeal;

    @PostMapping("/geoAnalyse")
    public JSONObject geoAnalyse(String tileName, @RequestPart("json") JSONObject scope, String method) throws Exception
    {
        return cxSvrGisDeal.geoAnalyse(tileName,scope,method);
    }

    @PostMapping("wfs2shp")
    public String wfs2Shp(String totalLayerName)
    {
        return cxSvrGisDeal.geoWfs2Shp(totalLayerName);
    }


}
