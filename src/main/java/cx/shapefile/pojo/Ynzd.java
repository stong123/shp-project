package cx.shapefile.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Ynzd
{
    private String id;
    private String county;
    private String township;
    private String village;
    private String village_group;
    private String land_no_survey;
    private String land_type_name1;
    private String land_type_code;
    private String spot_area;
    private String confer_area;
    private String area_unit;
    private String owner;
    private String geom;
}
