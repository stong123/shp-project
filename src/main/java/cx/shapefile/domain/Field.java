package cx.shapefile.domain;

import lombok.Data;

@Data
public class Field {
    private int id;
    private String name;
    private String type;
    private String alias;
    private String length;
}
