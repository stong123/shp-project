package cx.shapefile.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class Attribute
{
    ArrayList<HashMap<Object,Object>> list;
}
