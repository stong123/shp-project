package cx.shapefile.Mapper;

import cx.shapefile.pojo.Points;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PointsMapper
{
    List<Points> queryAll();
}
