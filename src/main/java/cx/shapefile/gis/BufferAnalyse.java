package cx.shapefile.gis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.util.List;

public class BufferAnalyse
{

    public static final int CAP_ROUND = 1;
    public static final int CAP_FLAT = 2;
    public static final int CAP_SQUARE = 3;

    public static final int DEFAULT_QUADRANT_SEGMENTS = 8;

    /**
     * 多个多边形
     * @param geometryList
     * @param distance
     * @return
     */
    public Geometry multiPolygonBuffer(List<Geometry> geometryList, Double distance)
    {
        Geometry unionBuffer = null;
        for (Geometry geo : geometryList)
        {
            Geometry bufferedGeo = geo.buffer(distance);
            unionBuffer = bufferedGeo.union(unionBuffer);
        }
        return unionBuffer;
    }

    /**
     * 多边形的缓冲区
     * @param geometry
     * @param distance
     * @return
     */
    public Geometry polygonBuffer(Geometry geometry, Double distance)
    {
        return geometry.buffer(distance);
    }

    /**
     * 普通线缓冲区
     * @param geometry
     * @param distance
     * @param quadrantSegments
     * @param endCapStyles
     * @return
     */
    public Geometry lineBuffer(Geometry geometry, Double distance, Integer quadrantSegments, Integer endCapStyles)
    {
        if(distance < 0)
        {
            throw new RuntimeException("distance must >= 0");
        }
        if(quadrantSegments == null)
        {
            quadrantSegments = 8;
        }
        if(endCapStyles == null)
        {
            endCapStyles = 1;
        }
        return geometry.buffer(distance, quadrantSegments, endCapStyles);
    }

    /**
     * 多个线的缓冲区
     * @param geometryList
     * @param distance
     * @param quadrantSegments
     * @param endCapStyles
     * @return
     */
    public Geometry multiLineBuffer(List<Geometry> geometryList, Double distance, Integer quadrantSegments, Integer endCapStyles)
    {
        if(distance < 0)
        {
            throw new RuntimeException("distance must >= 0");
        }
        if(quadrantSegments == null)
        {
            quadrantSegments = 8;
        }
        if(endCapStyles == null)
        {
            endCapStyles = 1;
        }
        Geometry unionBuffer = null;
        for (Geometry geo : geometryList)
        {
            Geometry bufferedGeo = geo.buffer(distance,quadrantSegments,endCapStyles);
            unionBuffer = bufferedGeo.union(unionBuffer);
        }
        return unionBuffer;
    }

    /**
     * 线的两边缓冲区大小不同时
     * @param geometry
     * @param distance1
     * @param distance2
     * @return
     */
    public Geometry lineDifBuffer(Geometry geometry, Double distance1, Double distance2)
    {
        if(distance1 < 0 && distance2 < 0)
        {
            throw new RuntimeException("distance must >= 0");
        }
        BufferParameters parameters = new BufferParameters();
        parameters.setEndCapStyle(BufferParameters.CAP_FLAT);
        parameters.setSingleSided(true);
        Geometry bufferOp1 = BufferOp.bufferOp(geometry, distance1, parameters);
        Geometry bufferOp2 = BufferOp.bufferOp(geometry, distance2, parameters);
        return bufferOp1.union(bufferOp2);
    }

    /**
     * 单点缓冲区
     * @param geometry
     * @param distance
     * @return
     */
    public Geometry pointBuffer(Geometry geometry, Double distance)
    {
        if(distance < 0)
        {
            throw new RuntimeException("distance must >= 0");
        }
        return geometry.buffer(distance);
    }

    /**
     * 多点缓冲区
     * @param geometryList
     * @param distance
     * @return
     */
    public Geometry multiPointBuffer(List<Geometry> geometryList, Double distance)
    {
        if(distance < 0)
        {
            throw new RuntimeException("distance must >= 0");
        }
        Geometry unionBuffer = null;
        for (Geometry geo : geometryList)
        {
            Geometry bufferedGeo = geo.buffer(distance);
            unionBuffer = bufferedGeo.union(unionBuffer);
        }
        return unionBuffer;
    }

}
