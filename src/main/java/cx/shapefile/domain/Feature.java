package cx.shapefile.domain;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class Feature {
    ArrayList<HashMap<Object,Object>> attribute;
    private double circumference;   //周长
    private double area;
    private JSONObject geometry;
}
