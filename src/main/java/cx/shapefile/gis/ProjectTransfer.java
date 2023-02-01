package cx.shapefile.gis;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public class ProjectTransfer
{

    /**
     * 转换
     * @param geometry
     * @param startEPSG
     * @param endEPSG
     * @return
     */
    public Geometry transform(Geometry geometry, String startEPSG , String endEPSG)
    {
        try
        {
            CoordinateReferenceSystem source = CRS.decode(startEPSG,true);
            CoordinateReferenceSystem crsTarget = CRS.decode(endEPSG,true);
            // 投影转换
            MathTransform transform = CRS.findMathTransform(source, crsTarget,true);
            return JTS.transform(geometry, transform);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
