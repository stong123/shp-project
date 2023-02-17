package cx.shapefile.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Points
{
    private String id;
    private String name;
    private Double x;
    private Double y;
    private String geom;
}
