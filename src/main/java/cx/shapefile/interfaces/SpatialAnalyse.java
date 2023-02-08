package cx.shapefile.interfaces;

import org.locationtech.jts.geom.Geometry;

import java.util.List;

public interface SpatialAnalyse
{
    String Intersection = "intersection";
    String Difference = "difference";
    String SymDifference = "symDifference";
    String Union = "union";

    int CAP_ROUND = 1;
    int CAP_FLAT = 2;
    int CAP_SQUARE = 3;

    int DEFAULT_QUADRANT_SEGMENTS = 8;

    Geometry spatialAnalyse(Geometry geo1, Geometry geo2, String method) throws Exception;

    Geometry pointBuffer(Geometry geometry, Double distance) throws Exception;

    Geometry lineBuffer(Geometry geometry, Double distance, Integer quadrantSegments, Integer endCapStyles) throws Exception;

    Geometry lineDifBuffer(Geometry geometry, Double distance1, Double distance2) throws Exception;

    Geometry polygonBuffer(Geometry geometry, Double distance) throws Exception;

    Geometry multiLineBuffer(List<Geometry> geometryList, Double distance, Integer quadrantSegments, Integer endCapStyles) throws Exception;

    Geometry multiPointBuffer(List<Geometry> geometryList, Double distance) throws Exception;

    Geometry multiPolygonBuffer(List<Geometry> geometryList, Double distance) throws Exception;
}
