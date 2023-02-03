package cx.shapefile.interfaces;

import org.locationtech.jts.geom.Geometry;

import java.util.List;

public interface SpatialAnalyse
{
    Geometry spatialAnalyse(Geometry geo1, Geometry geo2, String method) throws Exception;

    Geometry pointBuffer(Geometry geometry, Double distance) throws Exception;

    Geometry lineBuffer(Geometry geometry, Double distance, Integer quadrantSegments, Integer endCapStyles) throws Exception;

    Geometry lineDifBuffer(Geometry geometry, Double distance1, Double distance2) throws Exception;

    Geometry polygonBuffer(Geometry geometry, Double distance) throws Exception;

    Geometry multiLineBuffer(List<Geometry> geometryList, Double distance, Integer quadrantSegments, Integer endCapStyles) throws Exception;

    Geometry multiPointBuffer(List<Geometry> geometryList, Double distance) throws Exception;

    Geometry multiPolygonBuffer(List<Geometry> geometryList, Double distance) throws Exception;
}
