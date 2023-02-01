package cx.shapefile.gis;

import org.locationtech.jts.geom.Geometry;

public class OverlayAnalyse
{
    public Geometry overlayAnalyse(Geometry geo1, Geometry geo2, String method)
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
}
