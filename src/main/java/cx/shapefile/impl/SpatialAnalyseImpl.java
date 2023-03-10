package cx.shapefile.impl;

import cx.shapefile.interfaces.SpatialAnalyse;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpatialAnalyseImpl implements SpatialAnalyse
{
    @Override
    public Geometry spatialAnalyse(Geometry geo1, Geometry geo2, String method) throws Exception
    {
        switch (method)
        {
        case "intersection":    //交叉分析
            return geo1.intersection(geo2);
        case "difference":      //差异分析
            return geo1.difference(geo2);
        case "symDifference":   //对称差异分析
            return geo1.symDifference(geo2);
        case "union":           //联合分析
            return geo1.union(geo2);
        default:
            throw new RuntimeException("no such method");
        }
    }

    @Override
    public Geometry pointBuffer(Geometry geometry, Double distance) throws Exception
    {
        if(distance < 0)
        {
            throw new RuntimeException("distance must >= 0");
        }
        return geometry.buffer(distance);
    }

    @Override
    public Geometry lineBuffer(Geometry geometry, Double distance, Integer quadrantSegments, Integer endCapStyles) throws Exception
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
     * 线的两边缓冲区大小不同时
     */
    @Override
    public Geometry lineDifBuffer(Geometry geometry, Double distance1, Double distance2) throws Exception
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

    @Override
    public Geometry polygonBuffer(Geometry geometry, Double distance) throws Exception
    {
        return geometry.buffer(distance);
    }

    @Override
    public Geometry multiLineBuffer(List<Geometry> geometryList, Double distance, Integer quadrantSegments, Integer endCapStyles) throws Exception
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

    @Override
    public Geometry multiPointBuffer(List<Geometry> geometryList, Double distance) throws Exception
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

    @Override
    public Geometry multiPolygonBuffer(List<Geometry> geometryList, Double distance) throws Exception
    {
        Geometry unionBuffer = null;
        for (Geometry geo : geometryList)
        {
            Geometry bufferedGeo = geo.buffer(distance);
            unionBuffer = bufferedGeo.union(unionBuffer);
        }
        return unionBuffer;
    }
}
