package cx.shapefile.domain;

import lombok.Data;

import java.util.ArrayList;

@Data
public class DisplayFieldName
{
    private ArrayList<Feature> features;
//    private FieldAliases fieldAliases;
    private ArrayList<Field> fields;
//    private String geometryType;
    private String spatialReference;
}
