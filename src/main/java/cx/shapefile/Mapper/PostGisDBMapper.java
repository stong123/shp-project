package cx.shapefile.Mapper;

import cx.shapefile.pojo.Ynzd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostGisDBMapper
{
    List<Ynzd> queryGeom();
}
