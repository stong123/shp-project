package cx.shapefile.impl;

import cx.shapefile.interfaces.ProjectTransfer;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Component;

@Component
public class ProjectTransferImpl implements ProjectTransfer
{
    @Override
    public Geometry toProject(Geometry geometry, String startEPSG, String endEPSG) throws Exception
    {
        return null;
    }

    /**
     * 大地坐标转平面坐标
     * @param geometry
     * @param startEPSG
     * @param endEPSG
     * @return
     * @throws Exception
     */
    @Override
    public Geometry transform(Geometry geometry, String startEPSG, String endEPSG) throws Exception
    {
        CoordinateReferenceSystem source = CRS.decode(startEPSG,true);
        CoordinateReferenceSystem crsTarget = CRS.decode(endEPSG,true);
        // 投影转换
        MathTransform transform = CRS.findMathTransform(source, crsTarget,true);
        return JTS.transform(geometry, transform);
    }
}
