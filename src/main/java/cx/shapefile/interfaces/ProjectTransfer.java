package cx.shapefile.interfaces;

import org.locationtech.jts.geom.Geometry;

public interface ProjectTransfer
{
    Geometry toProject(Geometry geometry, String startEPSG , String endEPSG) throws Exception;

    Geometry transform(Geometry geometry, String startEPSG , String endEPSG) throws Exception;
}
