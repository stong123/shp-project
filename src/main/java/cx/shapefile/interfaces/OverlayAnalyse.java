package cx.shapefile.interfaces;

import org.locationtech.jts.geom.Geometry;

public interface OverlayAnalyse
{
    Geometry overlayAnalyse(Geometry geo1, Geometry geo2, String method) throws Exception;
}
